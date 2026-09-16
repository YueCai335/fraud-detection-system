package com.yuecai.fraud.batch;

import com.yuecai.fraud.modelclient.ModelFeatures;
import com.yuecai.fraud.modelclient.ModelScore;
import com.yuecai.fraud.modelclient.ModelServiceClient;
import com.yuecai.fraud.prediction.CsvTransactionParser;
import com.yuecai.fraud.prediction.Prediction;
import com.yuecai.fraud.prediction.PredictionCsv;
import com.yuecai.fraud.prediction.PredictionRepository;
import com.yuecai.fraud.prediction.PredictionResult;
import com.yuecai.fraud.storage.ObjectStore;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The queue consumer. The database is the queue: {@code batch_jobs} rows in PENDING are claimed
 * with {@code SELECT ... FOR UPDATE SKIP LOCKED}, so any number of service instances can run
 * this loop without stepping on each other and without any extra infrastructure.
 *
 * <p>Transactions are deliberately short. Claiming is one transaction; every chunk of scored
 * rows is committed together with the job's checkpoint in its own transaction; the model call
 * happens outside any transaction. A crash therefore loses at most one chunk of work, and a
 * retry resumes from {@code processedRows}.
 */
@Component
@ConditionalOnProperty(name = "batch.worker.enabled", havingValue = "true", matchIfMissing = true)
public class BatchJobWorker {

    private static final Logger log = LoggerFactory.getLogger(BatchJobWorker.class);

    private final BatchJobRepository jobs;
    private final PredictionRepository predictions;
    private final ObjectStore store;
    private final CsvTransactionParser parser;
    private final ModelServiceClient model;
    private final TransactionTemplate tx;
    private final BatchProperties props;

    public BatchJobWorker(BatchJobRepository jobs, PredictionRepository predictions, ObjectStore store,
                          CsvTransactionParser parser, ModelServiceClient model,
                          TransactionTemplate tx, BatchProperties props) {
        this.jobs = jobs;
        this.predictions = predictions;
        this.store = store;
        this.parser = parser;
        this.model = model;
        this.tx = tx;
        this.props = props;
    }

    /** Drain the queue: keep processing until no claimable job is left. */
    @Scheduled(fixedDelayString = "${batch.worker.poll-interval:5s}", initialDelayString = "10s")
    public void poll() {
        while (pollOnce()) {
            // keep going while there is work
        }
    }

    /** Process at most one job. Returns false when the queue was empty. Public for tests. */
    public boolean pollOnce() {
        Optional<BatchJob> claimed = claim();
        if (claimed.isEmpty()) {
            return false;
        }
        process(claimed.get().getId());
        return true;
    }

    /** RUNNING jobs whose worker stopped heart-beating are put back on the queue. */
    @Scheduled(fixedDelayString = "${batch.worker.stale-after:5m}", initialDelayString = "1m")
    public void reclaimStale() {
        Instant cutoff = Instant.now().minus(props.worker().staleAfter());
        tx.executeWithoutResult(status -> {
            for (BatchJob job : jobs.findByStatusAndUpdatedAtBefore(BatchJob.Status.RUNNING, cutoff)) {
                log.warn("Reclaiming stale job {} (last heartbeat {})", job.getId(), job.getUpdatedAt());
                job.reclaim(Instant.now());
            }
        });
    }

    // ---- one job -----------------------------------------------------------------------------

    private Optional<BatchJob> claim() {
        return tx.execute(status -> {
            List<BatchJob> candidates = jobs.findClaimable(Instant.now(), PageRequest.of(0, 1));
            if (candidates.isEmpty()) {
                return Optional.empty();
            }
            BatchJob job = candidates.get(0);
            job.markRunning(Instant.now());
            return Optional.of(job);
        });
    }

    void process(String jobId) {
        log.info("Batch job {} started", jobId);
        try {
            // 1. parse (outside any transaction)
            BatchJob snapshot = jobs.findById(jobId).orElseThrow();
            byte[] csv = store.get(snapshot.getInputKey());
            CsvTransactionParser.ParseResult parsed = parser.parse(new ByteArrayInputStream(csv));
            if (parsed.rows().isEmpty()) {
                throw new IllegalArgumentException("No valid transactions were parsed. Expected columns: "
                        + CsvTransactionParser.EXPECTED_HEADER + ". First problems: "
                        + String.join("; ", parsed.skipped().stream().limit(3).toList()));
            }
            if (parsed.rows().size() > props.worker().maxRows()) {
                throw new IllegalArgumentException("CSV has " + parsed.rows().size()
                        + " rows; the limit is " + props.worker().maxRows());
            }
            tx.executeWithoutResult(s -> jobs.findById(jobId).orElseThrow()
                    .setParsed(parsed.rows().size(), parsed.skipped().size(), Instant.now()));

            // 2. score in chunks, resuming from the checkpoint
            int from = snapshot.getProcessedRows();
            int fraudCount = snapshot.getFraudCount();
            int chunkSize = props.worker().chunkSize();
            UUID batchId = UUID.fromString(jobId);
            while (from < parsed.rows().size()) {
                int to = Math.min(from + chunkSize, parsed.rows().size());
                List<CsvTransactionParser.Row> chunk = parsed.rows().subList(from, to);
                List<ModelFeatures> features = chunk.stream().map(r -> r.transaction().toFeatures()).toList();

                List<ModelScore> scores = model.predictBatch(features); // retried by Resilience4j

                final int done = to;
                final int fraudSoFar = fraudCount + (int) scores.stream().filter(ModelScore::isFraud).count();
                tx.executeWithoutResult(s -> {
                    BatchJob job = jobs.findById(jobId).orElseThrow();
                    List<Prediction> rows = new ArrayList<>(chunk.size());
                    for (int i = 0; i < chunk.size(); i++) {
                        rows.add(Prediction.batchRow(job.getUser(), batchId, chunk.get(i).lineNumber(),
                                features.get(i), scores.get(i)));
                    }
                    predictions.saveAll(rows);
                    job.checkpoint(done, fraudSoFar, Instant.now()); // same transaction as the rows
                });
                fraudCount = fraudSoFar;
                from = to;
                log.debug("Batch job {}: {}/{} rows", jobId, from, parsed.rows().size());
            }

            // 3. result file, then SUCCEEDED
            List<PredictionResult> results = tx.execute(s -> predictions
                    .findByBatchIdAndUserUsernameOrderByCsvRowAsc(jobId, jobs.findById(jobId).orElseThrow().getUser().getUsername())
                    .stream().map(PredictionResult::from).toList());
            String resultKey = BatchJobService.resultKey(jobId);
            store.put(resultKey, PredictionCsv.write(results).getBytes(StandardCharsets.UTF_8), "text/csv");
            tx.executeWithoutResult(s -> jobs.findById(jobId).orElseThrow().markSucceeded(resultKey, Instant.now()));
            log.info("Batch job {} succeeded: {} rows, {} flagged", jobId, results.size(), fraudCount);

        } catch (Exception e) {
            log.warn("Batch job {} attempt failed: {}", jobId, e.toString());
            tx.executeWithoutResult(s -> jobs.findById(jobId).orElseThrow()
                    .markAttemptFailed(e.getMessage() == null ? e.toString() : e.getMessage(), Instant.now()));
        }
    }
}
