-- Asynchronous batch scoring: one row per uploaded CSV, processed by a background worker.
CREATE TABLE batch_jobs (
    id                VARCHAR(36)   NOT NULL,
    user_id           BIGINT        NOT NULL,
    status            VARCHAR(16)   NOT NULL,           -- PENDING | RUNNING | SUCCEEDED | FAILED
    idempotency_key   VARCHAR(128)  NOT NULL,
    original_filename VARCHAR(255)  NULL,
    input_key         VARCHAR(255)  NOT NULL,           -- object store key of the uploaded CSV
    result_key        VARCHAR(255)  NULL,               -- object store key of the result CSV
    total_rows        INT           NULL,               -- known after parsing
    processed_rows    INT           NOT NULL DEFAULT 0, -- checkpoint: retries resume from here
    fraud_count       INT           NOT NULL DEFAULT 0,
    skipped_rows      INT           NOT NULL DEFAULT 0,
    attempts          INT           NOT NULL DEFAULT 0,
    max_attempts      INT           NOT NULL DEFAULT 3,
    next_attempt_at   DATETIME(6)   NOT NULL,           -- back-off between attempts
    last_error        VARCHAR(1000) NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,           -- heartbeat while RUNNING
    started_at        DATETIME(6)   NULL,
    finished_at       DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_batch_jobs_user FOREIGN KEY (user_id) REFERENCES users (id),
    -- the same user submitting the same file twice gets the same job back
    CONSTRAINT uk_batch_jobs_user_key UNIQUE (user_id, idempotency_key)
);

-- worker claim query: PENDING jobs whose back-off has elapsed, oldest first
CREATE INDEX ix_batch_jobs_claim ON batch_jobs (status, next_attempt_at, created_at);

-- A job re-run after a crash must never store the same CSV row twice.
-- (single predictions have batch_id NULL; NULLs do not collide in a unique index)
CREATE UNIQUE INDEX uk_predictions_batch_row ON predictions (batch_id, csv_row);
