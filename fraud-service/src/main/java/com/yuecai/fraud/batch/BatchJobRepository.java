package com.yuecai.fraud.batch;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface BatchJobRepository extends JpaRepository<BatchJob, String> {

    Optional<BatchJob> findByIdAndUserUsername(String id, String username);

    Optional<BatchJob> findByUserUsernameAndIdempotencyKey(String username, String idempotencyKey);

    Page<BatchJob> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    /**
     * The queue: oldest claimable PENDING jobs. {@code PESSIMISTIC_WRITE} + lock timeout {@code -2}
     * is Hibernate's spelling of {@code SELECT ... FOR UPDATE SKIP LOCKED}, so several worker
     * instances can poll concurrently and never pick the same job. Must run inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select j from BatchJob j where j.status = com.yuecai.fraud.batch.BatchJob$Status.PENDING"
            + " and j.nextAttemptAt <= :now order by j.createdAt")
    List<BatchJob> findClaimable(@Param("now") Instant now, Pageable limit);

    /** RUNNING jobs whose heartbeat is older than the cutoff — their worker is presumed dead. */
    List<BatchJob> findByStatusAndUpdatedAtBefore(BatchJob.Status status, Instant cutoff);
}
