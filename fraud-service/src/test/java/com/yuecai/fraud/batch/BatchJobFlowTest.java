package com.yuecai.fraud.batch;

import static com.yuecai.fraud.TestFixtures.FRAUD_SCORE;
import static com.yuecai.fraud.TestFixtures.LEGIT_SCORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yuecai.fraud.modelclient.ModelFeatures;
import com.yuecai.fraud.modelclient.ModelScore;
import com.yuecai.fraud.modelclient.ModelServiceClient;
import com.yuecai.fraud.modelclient.ModelServiceUnavailableException;
import com.yuecai.fraud.prediction.PredictionRepository;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Submit -> worker -> result, through the real REST layer, Spring Security, JPA/H2/Flyway and the
 * in-memory object store. Only the model service is mocked. The worker is driven by hand
 * ({@code scheduling.enabled=false} in the test profile) and chunk size is 2 to exercise resume.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatchJobFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PredictionRepository predictions;
    @Autowired BatchJobRepository jobs;
    @Autowired BatchJobWorker worker;
    @Autowired PasswordEncoder encoder;
    @Autowired TransactionTemplate tx;
    @MockitoBean ModelServiceClient modelClient;

    private static final String CSV = """
            step,type_code,amount,oldbalanceOrg,newbalanceOrig,oldbalanceDest,newbalanceDest
            1,3,9839.64,170136.0,160296.36,0.0,0.0
            100,1,10000.0,10000.0,0.0,0.0,10000.0
            oops,bad,line,x,y,z,w
            1,3,1864.28,21249.0,19384.72,0.0,0.0
            """;

    @BeforeEach
    void seed() {
        predictions.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
        users.save(new User("alice", encoder.encode("secret"), "Alice", "A", "alice@example.com"));
        users.save(new User("bob", encoder.encode("secret"), "Bob", "B", "bob@example.com"));
    }

    private MockMultipartFile csv() {
        return new MockMultipartFile("file", "tx.csv", "text/csv", CSV.getBytes(StandardCharsets.UTF_8));
    }

    private String submitAs(String user) throws Exception {
        String body = mvc.perform(multipart("/api/v1/batch-jobs").file(csv()).with(httpBasic(user, "secret")))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/batch-jobs/")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.progressPercent").value(0))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void submitPollDownload() throws Exception {
        when(modelClient.predictBatch(anyList()))
                .thenReturn(List.of(LEGIT_SCORE, FRAUD_SCORE))   // chunk 1: rows 2,3
                .thenReturn(List.of(LEGIT_SCORE));               // chunk 2: row 5
        String id = submitAs("alice");

        assertThat(jobs.findById(id).orElseThrow().getInputKey()).isEqualTo("jobs/" + id + "/input.csv");

        assertThat(worker.pollOnce()).isTrue();
        assertThat(worker.pollOnce()).isFalse(); // queue empty

        mvc.perform(get("/api/v1/batch-jobs/{id}", id).with(httpBasic("alice", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.processedRows").value(3))
                .andExpect(jsonPath("$.skippedRows").value(1))
                .andExpect(jsonPath("$.fraudCount").value(1))
                .andExpect(jsonPath("$.progressPercent").value(100))
                .andExpect(jsonPath("$.attempts").value(1))
                .andExpect(jsonPath("$.resultUrl").value(org.hamcrest.Matchers.endsWith("/api/v1/batch-jobs/" + id + "/result")));

        // in-memory store has no presigned URLs, so the CSV is streamed directly
        mvc.perform(get("/api/v1/batch-jobs/{id}/result", id).with(httpBasic("alice", "secret")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("csv_row,step,type_code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\n3,100,1,CASH_OUT,10000.0")));

        assertThat(predictions.countByBatchIdAndFraudTrue(id)).isEqualTo(1);

        mvc.perform(get("/api/v1/batch-jobs").with(httpBasic("alice", "secret")))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void sameFileTwiceIsOneJob() throws Exception {
        String id = submitAs("alice");
        mvc.perform(multipart("/api/v1/batch-jobs").file(csv()).with(httpBasic("alice", "secret")))
                .andExpect(status().isOk())          // not 202: nothing new was created
                .andExpect(jsonPath("$.id").value(id));
        assertThat(jobs.count()).isEqualTo(1);

        // an explicit key wins over the content hash
        mvc.perform(multipart("/api/v1/batch-jobs").file(csv()).header("Idempotency-Key", "run-2")
                        .with(httpBasic("alice", "secret")))
                .andExpect(status().isAccepted());
        mvc.perform(multipart("/api/v1/batch-jobs").file(csv()).header("Idempotency-Key", "run-2")
                        .with(httpBasic("alice", "secret")))
                .andExpect(status().isOk());
        assertThat(jobs.count()).isEqualTo(2);

        // a different user with the same file gets their own job
        mvc.perform(multipart("/api/v1/batch-jobs").file(csv()).with(httpBasic("bob", "secret")))
                .andExpect(status().isAccepted());
        assertThat(jobs.count()).isEqualTo(3);
    }

    @Test
    void jobsArePrivate() throws Exception {
        String id = submitAs("alice");
        mvc.perform(get("/api/v1/batch-jobs/{id}", id).with(httpBasic("bob", "secret")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/batch-jobs/{id}/result", id).with(httpBasic("bob", "secret")))
                .andExpect(status().isNotFound());
    }

    @Test
    void resultBeforeSuccessIs409AndRetryOnlyForFailed() throws Exception {
        String id = submitAs("alice");
        mvc.perform(get("/api/v1/batch-jobs/{id}/result", id).with(httpBasic("alice", "secret")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/batch-jobs/{id}/retry", id).with(httpBasic("alice", "secret")))
                .andExpect(status().isConflict());
    }

    @Test
    void transientModelFailureBacksOffThenResumesFromCheckpoint() throws Exception {
        when(modelClient.predictBatch(anyList()))
                .thenReturn(List.of(LEGIT_SCORE, FRAUD_SCORE))                        // chunk 1 ok
                .thenThrow(new ModelServiceUnavailableException("down", null))         // chunk 2 fails
                .thenReturn(List.of(LEGIT_SCORE));                                    // chunk 2 on retry
        String id = submitAs("alice");

        assertThat(worker.pollOnce()).isTrue();

        BatchJob afterFailure = jobs.findById(id).orElseThrow();
        assertThat(afterFailure.getStatus()).isEqualTo(BatchJob.Status.PENDING);
        assertThat(afterFailure.getAttempts()).isEqualTo(1);
        assertThat(afterFailure.getProcessedRows()).isEqualTo(2);     // checkpoint kept
        assertThat(afterFailure.getLastError()).contains("down");
        assertThat(afterFailure.getNextAttemptAt()).isAfter(Instant.now()); // backing off
        assertThat(predictions.count()).isEqualTo(2);

        assertThat(worker.pollOnce()).isFalse(); // not claimable until the back-off elapses

        makeClaimableNow(id);
        assertThat(worker.pollOnce()).isTrue();

        BatchJob done = jobs.findById(id).orElseThrow();
        assertThat(done.getStatus()).isEqualTo(BatchJob.Status.SUCCEEDED);
        assertThat(done.getAttempts()).isEqualTo(2);
        assertThat(predictions.count()).isEqualTo(3);                // no duplicate rows

        // the retry scored only the remaining row, not the whole file again
        ArgumentCaptor<List<ModelFeatures>> calls = ArgumentCaptor.captor();
        verify(modelClient, times(3)).predictBatch(calls.capture());
        assertThat(calls.getAllValues().get(2)).hasSize(1);
    }

    @Test
    void givesUpAfterMaxAttemptsAndCanBeRetriedManually() throws Exception {
        when(modelClient.predictBatch(anyList())).thenThrow(new ModelServiceUnavailableException("down", null));
        String id = submitAs("alice");

        for (int attempt = 1; attempt <= 3; attempt++) {
            makeClaimableNow(id);
            assertThat(worker.pollOnce()).isTrue();
        }
        mvc.perform(get("/api/v1/batch-jobs/{id}", id).with(httpBasic("alice", "secret")))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.attempts").value(3))
                .andExpect(jsonPath("$.lastError").value("down"));

        // (doReturn: the mock is currently stubbed to throw, so when(mock.call()) would throw here)
        doReturn(List.of(LEGIT_SCORE, FRAUD_SCORE), List.of(LEGIT_SCORE)).when(modelClient).predictBatch(anyList());
        mvc.perform(post("/api/v1/batch-jobs/{id}/retry", id).with(httpBasic("alice", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attempts").value(0));
        assertThat(worker.pollOnce()).isTrue();
        assertThat(jobs.findById(id).orElseThrow().getStatus()).isEqualTo(BatchJob.Status.SUCCEEDED);
    }

    @Test
    void unparseableFileFailsWithoutCallingTheModel() throws Exception {
        MockMultipartFile bad = new MockMultipartFile("file", "bad.csv", "text/csv", "a,b\n".getBytes());
        String body = mvc.perform(multipart("/api/v1/batch-jobs").file(bad).with(httpBasic("alice", "secret")))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        for (int attempt = 1; attempt <= 3; attempt++) {
            makeClaimableNow(id);
            worker.pollOnce();
        }
        BatchJob job = jobs.findById(id).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.FAILED);
        assertThat(job.getLastError()).contains("No valid transactions");
        verify(modelClient, times(0)).predictBatch(anyList());
    }

    @Test
    void staleRunningJobsAreReclaimed() throws Exception {
        String id = submitAs("alice");
        tx.executeWithoutResult(s -> {
            BatchJob job = jobs.findById(id).orElseThrow();
            job.markRunning(Instant.now().minusSeconds(3600)); // pretend a worker took it an hour ago and died
        });

        worker.reclaimStale();

        BatchJob job = jobs.findById(id).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.PENDING);
        assertThat(job.getLastError()).contains("reclaimed");
    }

    /** Skip the back-off so the next pollOnce() can claim the job. */
    private void makeClaimableNow(String id) {
        tx.executeWithoutResult(s -> {
            BatchJob job = jobs.findById(id).orElseThrow();
            ReflectionTestUtils.setField(job, "nextAttemptAt", Instant.now().minusSeconds(1));
        });
    }
}
