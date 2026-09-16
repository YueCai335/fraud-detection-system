package com.yuecai.fraud.batch;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * State of a job as shown to clients (REST JSON and JSP pages).
 * A class with getters rather than a record because the JSP EL implementation in Tomcat 10.1
 * (EL 5.0) cannot read record accessors.
 */
@Schema(description = "State of an asynchronous batch scoring job")
public final class BatchJobResponse {

    private final String id;
    private final BatchJob.Status status;
    private final String originalFilename;
    private final Integer totalRows;
    private final int processedRows;
    private final int fraudCount;
    private final int skippedRows;
    private final int progressPercent;
    private final int attempts;
    private final int maxAttempts;
    private final String lastError;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final String resultUrl;

    private BatchJobResponse(BatchJob j, String resultUrl) {
        this.id = j.getId();
        this.status = j.getStatus();
        this.originalFilename = j.getOriginalFilename();
        this.totalRows = j.getTotalRows();
        this.processedRows = j.getProcessedRows();
        this.fraudCount = j.getFraudCount();
        this.skippedRows = j.getSkippedRows();
        this.progressPercent = j.getStatus() == BatchJob.Status.SUCCEEDED ? 100
                : j.getTotalRows() == null || j.getTotalRows() == 0 ? 0
                : (int) Math.round(100.0 * j.getProcessedRows() / j.getTotalRows());
        this.attempts = j.getAttempts();
        this.maxAttempts = j.getMaxAttempts();
        this.lastError = j.getLastError();
        this.createdAt = j.getCreatedAt();
        this.updatedAt = j.getUpdatedAt();
        this.startedAt = j.getStartedAt();
        this.finishedAt = j.getFinishedAt();
        this.resultUrl = j.getStatus() == BatchJob.Status.SUCCEEDED ? resultUrl : null;
    }

    public static BatchJobResponse from(BatchJob j, String resultUrl) {
        return new BatchJobResponse(j, resultUrl);
    }

    public String getId() { return id; }
    public BatchJob.Status getStatus() { return status; }
    public String getOriginalFilename() { return originalFilename; }
    @Schema(description = "null until the file has been parsed")
    public Integer getTotalRows() { return totalRows; }
    public int getProcessedRows() { return processedRows; }
    public int getFraudCount() { return fraudCount; }
    public int getSkippedRows() { return skippedRows; }
    @Schema(description = "0–100, based on processedRows/totalRows")
    public int getProgressPercent() { return progressPercent; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    @Schema(description = "Present once SUCCEEDED: GET this to download the result CSV")
    public String getResultUrl() { return resultUrl; }
}
