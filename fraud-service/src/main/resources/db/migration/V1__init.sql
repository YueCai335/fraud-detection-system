-- Users (replaces the hand-made XAMPP `user` table with SHA-256 hex passwords).
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(64)  NOT NULL,
    last_name     VARCHAR(64)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- Every scored transaction, single or batch.
CREATE TABLE predictions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    source          VARCHAR(16)  NOT NULL,
    batch_id        VARCHAR(36)  NULL,
    csv_row         INT          NULL,
    step            INT          NOT NULL,
    type_code       INT          NOT NULL,
    amount          DOUBLE       NOT NULL,
    oldbalance_org  DOUBLE       NOT NULL,
    newbalance_orig DOUBLE       NOT NULL,
    oldbalance_dest DOUBLE       NOT NULL,
    newbalance_dest DOUBLE       NOT NULL,
    fraud           BOOLEAN      NOT NULL,
    prob_fraud      DOUBLE       NULL,
    threshold       DOUBLE       NOT NULL,
    reason1         VARCHAR(255) NULL,
    reason2         VARCHAR(255) NULL,
    reason3         VARCHAR(255) NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_predictions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_predictions_user_created ON predictions (user_id, created_at);
CREATE INDEX ix_predictions_batch ON predictions (batch_id);
