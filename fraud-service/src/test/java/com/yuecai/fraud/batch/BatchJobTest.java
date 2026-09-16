package com.yuecai.fraud.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yuecai.fraud.user.User;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** The state machine, without Spring. */
class BatchJobTest {

    private final User user = new User("alice", "{noop}x", "A", "B", "a@x.io");
    private final Instant t0 = Instant.parse("2026-09-16T10:00:00Z");

    private BatchJob pending() {
        return new BatchJob(user, "key", "tx.csv", "jobs/x/input.csv");
    }

    @Test
    void happyPath() {
        BatchJob job = pending();
        job.markRunning(t0);
        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.RUNNING);
        assertThat(job.getAttempts()).isEqualTo(1);

        job.setParsed(10, 1, t0);
        job.checkpoint(4, 2, t0.plusSeconds(1));
        assertThat(job.getProcessedRows()).isEqualTo(4);
        assertThat(job.getUpdatedAt()).isEqualTo(t0.plusSeconds(1)); // heartbeat

        job.markSucceeded("jobs/x/result.csv", t0.plusSeconds(2));
        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.SUCCEEDED);
        assertThat(job.getResultKey()).isEqualTo("jobs/x/result.csv");
        assertThat(job.getFinishedAt()).isEqualTo(t0.plusSeconds(2));
    }

    @Test
    void failuresBackOffThenGiveUpAfterMaxAttempts() {
        BatchJob job = pending();

        job.markRunning(t0);
        job.markAttemptFailed("boom", t0);
        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.PENDING);
        assertThat(job.getNextAttemptAt()).isEqualTo(t0.plus(Duration.ofSeconds(30)));

        job.markRunning(t0);
        job.markAttemptFailed("boom", t0);
        assertThat(job.getNextAttemptAt()).isEqualTo(t0.plus(Duration.ofSeconds(60)));

        job.markRunning(t0);
        job.markAttemptFailed("boom", t0);
        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.FAILED);
        assertThat(job.getAttempts()).isEqualTo(3);
        assertThat(job.getLastError()).isEqualTo("boom");
        assertThat(job.getFinishedAt()).isEqualTo(t0);
    }

    @Test
    void manualRetryResetsAttemptsButKeepsCheckpoint() {
        BatchJob job = pending();
        for (int i = 0; i < 3; i++) {
            job.markRunning(t0);
            if (i == 0) job.checkpoint(500, 7, t0);
            job.markAttemptFailed("boom", t0);
        }
        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.FAILED);

        job.retry(t0.plusSeconds(5));

        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.PENDING);
        assertThat(job.getAttempts()).isZero();
        assertThat(job.getProcessedRows()).isEqualTo(500);
        assertThat(job.getNextAttemptAt()).isEqualTo(t0.plusSeconds(5));
    }

    @Test
    void reclaimDoesNotConsumeAnAttempt() {
        BatchJob job = pending();
        job.markRunning(t0);
        job.reclaim(t0.plusSeconds(600));
        assertThat(job.getStatus()).isEqualTo(BatchJob.Status.PENDING);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getLastError()).contains("heartbeat");
    }

    @Test
    void transitionsAreGuarded() {
        BatchJob job = pending();
        assertThatThrownBy(() -> job.markSucceeded("k", t0)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> job.retry(t0)).isInstanceOf(IllegalStateException.class);
        job.markRunning(t0);
        assertThatThrownBy(() -> job.markRunning(t0)).isInstanceOf(IllegalStateException.class);
    }
}
