package com.yuecai.fraud.batch;

import com.yuecai.fraud.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * One uploaded CSV, scored asynchronously by {@link BatchJobWorker}.
 *
 * <pre>
 *   PENDING ──claim──▶ RUNNING ──▶ SUCCEEDED
 *      ▲                 │
 *      └──retry/back-off─┴──▶ FAILED (after maxAttempts)
 * </pre>
 *
 * {@code processedRows} is the checkpoint: a re-run continues from there instead of scoring
 * the whole file again. {@code updatedAt} is the heartbeat used to reclaim jobs whose worker died.
 */
@Entity
@Table(name = "batch_jobs")
public class BatchJob {

    public enum Status { PENDING, RUNNING, SUCCEEDED, FAILED }

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "input_key", nullable = false)
    private String inputKey;

    @Column(name = "result_key")
    private String resultKey;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "processed_rows", nullable = false)
    private int processedRows;

    @Column(name = "fraud_count", nullable = false)
    private int fraudCount;

    @Column(name = "skipped_rows", nullable = false)
    private int skippedRows;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected BatchJob() {
        // JPA
    }

    public BatchJob(User user, String idempotencyKey, String originalFilename) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.idempotencyKey = idempotencyKey;
        this.originalFilename = originalFilename;
        this.inputKey = inputKey(id);
    }

    public static String inputKey(String jobId) {
        return "jobs/" + jobId + "/input.csv";
    }

    public static String resultKey(String jobId) {
        return "jobs/" + jobId + "/result.csv";
    }

    // ---- state transitions (the only way status changes) ------------------------------------

    /** Worker took the job. */
    public void markRunning(Instant now) {
        requireStatus(Status.PENDING);
        status = Status.RUNNING;
        attempts++;
        startedAt = now;
        touch(now);
    }

    /** One chunk done and committed; also serves as the heartbeat. */
    public void checkpoint(int processedRows, int fraudCount, Instant now) {
        requireStatus(Status.RUNNING);
        this.processedRows = processedRows;
        this.fraudCount = fraudCount;
        touch(now);
    }

    public void markSucceeded(String resultKey, Instant now) {
        requireStatus(Status.RUNNING);
        status = Status.SUCCEEDED;
        this.resultKey = resultKey;
        lastError = null;
        finishedAt = now;
        touch(now);
    }

    /** Attempt failed: back off and retry, or give up after {@code maxAttempts}. */
    public void markAttemptFailed(String error, Instant now) {
        requireStatus(Status.RUNNING);
        lastError = error == null ? "unknown error" : error.substring(0, Math.min(error.length(), 1000));
        if (attempts >= maxAttempts) {
            status = Status.FAILED;
            finishedAt = now;
        } else {
            status = Status.PENDING;
            nextAttemptAt = now.plus(backoff(attempts));
        }
        touch(now);
    }

    /** Worker died mid-run (no heartbeat): make the job claimable again without counting an attempt. */
    public void reclaim(Instant now) {
        requireStatus(Status.RUNNING);
        status = Status.PENDING;
        nextAttemptAt = now;
        lastError = "worker heartbeat lost; reclaimed";
        touch(now);
    }

    /** Manual retry of a FAILED job: fresh attempt budget, keeps the checkpoint. */
    public void retry(Instant now) {
        requireStatus(Status.FAILED);
        status = Status.PENDING;
        attempts = 0;
        nextAttemptAt = now;
        finishedAt = null;
        touch(now);
    }

    public void setParsed(int totalRows, int skippedRows, Instant now) {
        this.totalRows = totalRows;
        this.skippedRows = skippedRows;
        touch(now);
    }

    static Duration backoff(int attempt) {
        // 30s, 60s, 120s ...
        return Duration.ofSeconds(30L << Math.max(0, attempt - 1));
    }

    private void requireStatus(Status expected) {
        if (status != expected) {
            throw new IllegalStateException("Job " + id + " is " + status + ", expected " + expected);
        }
    }

    private void touch(Instant now) {
        updatedAt = now;
    }

    public String getId() { return id; }
    public User getUser() { return user; }
    public Status getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getInputKey() { return inputKey; }
    public String getResultKey() { return resultKey; }
    public Integer getTotalRows() { return totalRows; }
    public int getProcessedRows() { return processedRows; }
    public int getFraudCount() { return fraudCount; }
    public int getSkippedRows() { return skippedRows; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
