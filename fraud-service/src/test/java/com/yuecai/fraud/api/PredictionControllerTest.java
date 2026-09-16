package com.yuecai.fraud.api;

import static com.yuecai.fraud.TestFixtures.FRAUD_SCORE;
import static com.yuecai.fraud.TestFixtures.LEGIT_SCORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yuecai.fraud.modelclient.ModelServiceClient;
import com.yuecai.fraud.modelclient.ModelServiceException;
import com.yuecai.fraud.prediction.PredictionRepository;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end through the REST layer: real Spring Security, real JPA on H2 (Flyway-migrated),
 * only the Python model service is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PredictionControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PredictionRepository predictions;
    @Autowired com.yuecai.fraud.batch.BatchJobRepository batchJobs;
    @Autowired PasswordEncoder encoder;
    @MockitoBean ModelServiceClient modelClient;

    private static final String LEGIT_BODY = """
            {"step":1,"typeCode":3,"amount":9839.64,"oldbalanceOrg":170136.0,"newbalanceOrig":160296.36,
             "oldbalanceDest":0.0,"newbalanceDest":0.0}
            """;

    @BeforeEach
    void seedUser() {
        predictions.deleteAll();
        batchJobs.deleteAll();
        users.deleteAll();
        users.save(new User("alice", encoder.encode("secret"), "Alice", "A", "alice@example.com"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/predictions").contentType(MediaType.APPLICATION_JSON).content(LEGIT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scoresAndStoresSingleTransaction() throws Exception {
        when(modelClient.predict(any())).thenReturn(LEGIT_SCORE);

        mvc.perform(post("/api/v1/predictions")
                        .with(httpBasic("alice", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LEGIT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fraud").value(false))
                .andExpect(jsonPath("$.probFraud").value(0.13))
                .andExpect(jsonPath("$.type").value("PAYMENT"))
                .andExpect(jsonPath("$.reasons[0]").value("No risk detected"))
                .andExpect(jsonPath("$.id").isNumber());

        assertThat(predictions.count()).isEqualTo(1);

        mvc.perform(get("/api/v1/predictions").with(httpBasic("alice", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].typeCode").value(3));
    }

    @Test
    void rejectsInvalidPayloadWithProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/predictions")
                        .with(httpBasic("alice", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"step\":1,\"typeCode\":9,\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.typeCode").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.oldbalanceOrg").exists());
    }

    @Test
    void mapsModelOutageTo503() throws Exception {
        when(modelClient.predict(any())).thenThrow(new ModelServiceException("Model service is unavailable or timed out"));

        mvc.perform(post("/api/v1/predictions")
                        .with(httpBasic("alice", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LEGIT_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Model service is unavailable or timed out"));

        assertThat(predictions.count()).isZero();
    }

    @Test
    void batchUploadScoresStoresAndExportsCsv() throws Exception {
        when(modelClient.predictBatch(anyList())).thenReturn(List.of(LEGIT_SCORE, FRAUD_SCORE));
        String csv = """
                step,type_code,amount,oldbalanceOrg,newbalanceOrig,oldbalanceDest,newbalanceDest
                1,3,9839.64,170136.0,160296.36,0.0,0.0
                oops,this,line,is,bad,x,y
                100,1,10000.0,10000.0,0.0,0.0,10000.0
                """;
        MockMultipartFile file = new MockMultipartFile("file", "tx.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mvc.perform(multipart("/api/v1/predictions/batch").file(file).with(httpBasic("alice", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.fraudCount").value(1))
                .andExpect(jsonPath("$.skipped[0]").value("line 3: non-numeric value"))
                .andExpect(jsonPath("$.results[1].csvRow").value(4))
                .andExpect(jsonPath("$.results[1].fraud").value(true))
                .andReturn();

        String batchId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.batchId");
        assertThat(predictions.countByBatchIdAndFraudTrue(batchId)).isEqualTo(1);

        mvc.perform(get("/api/v1/predictions/batches/{id}/csv", batchId).with(httpBasic("alice", "secret")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("csv_row,step,type_code,type,amount")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "4,100,1,CASH_OUT,10000.0,10000.0,0.0,0.0,10000.0,1,0.39,\"Receiver balance increased sharply\"")));
    }

    @Test
    void batchRejectsRowsTheSingleEndpointWouldReject() throws Exception {
        // Regression (review of 0c10e15): amount=-5 was 400 on POST /predictions but accepted via CSV.
        when(modelClient.predictBatch(anyList())).thenReturn(List.of(FRAUD_SCORE));
        MockMultipartFile file = new MockMultipartFile("file", "tx.csv", "text/csv", """
                100,1,-5,10000.0,0.0,0.0,10000.0
                100,1,10000.0,10000.0,0.0,0.0,10000.0
                """.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/v1/predictions/batch").file(file).with(httpBasic("alice", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.skipped[0]").value("line 1: amount must be greater than or equal to 0"));

        assertThat(predictions.count()).isEqualTo(1);
    }

    @Test
    void batchWithNoValidRowsIs400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "tx.csv", "text/csv", "a,b,c\n".getBytes());

        mvc.perform(multipart("/api/v1/predictions/batch").file(file).with(httpBasic("alice", "secret")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.skipped[0]").value("line 1: expected 7 columns, found 3"));
    }

    @Test
    void otherUsersCannotReadMyBatch() throws Exception {
        users.save(new User("bob", encoder.encode("secret"), "Bob", "B", "bob@example.com"));
        when(modelClient.predictBatch(anyList())).thenReturn(List.of(LEGIT_SCORE));
        MockMultipartFile file = new MockMultipartFile("file", "tx.csv", "text/csv",
                "1,3,9839.64,170136.0,160296.36,0.0,0.0\n".getBytes());
        MvcResult result = mvc.perform(multipart("/api/v1/predictions/batch").file(file).with(httpBasic("alice", "secret")))
                .andExpect(status().isOk()).andReturn();
        String batchId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.batchId");

        mvc.perform(get("/api/v1/predictions/batches/{id}/csv", batchId).with(httpBasic("bob", "secret")))
                .andExpect(status().isNotFound());
    }

    @Test
    void openApiDocsArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Fraud Detection API"))
                .andExpect(jsonPath("$.paths['/api/v1/predictions'].post").exists());
    }
}
