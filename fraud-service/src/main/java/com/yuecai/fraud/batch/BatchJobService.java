package com.yuecai.fraud.batch;

import com.yuecai.fraud.prediction.PredictionRepository;
import com.yuecai.fraud.prediction.PredictionResult;
import com.yuecai.fraud.storage.ObjectStore;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Submitting, reading and retrying jobs. Processing lives in {@link BatchJobWorker}. */
@Service
public class BatchJobService {

    /** Outcome of a submission: the job, and whether this call created it. */
    public record Submission(BatchJob job, boolean created) {}

    private final BatchJobRepository jobs;
    private final PredictionRepository predictions;
    private final UserService users;
    private final ObjectStore store;
    private final BatchProperties props;

    public BatchJobService(BatchJobRepository jobs, PredictionRepository predictions, UserService users,
                           ObjectStore store, BatchProperties props) {
        this.jobs = jobs;
        this.predictions = predictions;
        this.users = users;
        this.store = store;
        this.props = props;
    }

    /**
     * Idempotent submit. The key is the caller's {@code Idempotency-Key} header, or — if absent —
     * a hash of the file content, so uploading the same file twice returns the same job.
     * Uniqueness is enforced by the database ({@code uk_batch_jobs_user_key}), not by
     * check-then-insert, so two concurrent submits still yield one job.
     */
    @Transactional
    public Submission submit(String username, byte[] csv, String originalFilename, String idempotencyKey) {
        if (csv == null || csv.length == 0) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        User user = users.requireByUsername(username);
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? "sha256:" + sha256(csv) : idempotencyKey.strip();
        if (key.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must be at most 128 characters");
        }

        Optional<BatchJob> existing = jobs.findByUserUsernameAndIdempotencyKey(username, key);
        if (existing.isPresent()) {
            return new Submission(existing.get(), false);
        }

        BatchJob job = new BatchJob(user, key, originalFilename);
        store.put(job.getInputKey(), csv, "text/csv");
        try {
            return new Submission(jobs.saveAndFlush(job), true);
        } catch (DataIntegrityViolationException raced) {
            // Lost the race with an identical concurrent submit: hand back the winner.
            return new Submission(jobs.findByUserUsernameAndIdempotencyKey(username, key).orElseThrow(), false);
        }
    }

    @Transactional(readOnly = true)
    public BatchJob get(String username, String id) {
        return jobs.findByIdAndUserUsername(id, username).orElseThrow(() -> new BatchJobNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<BatchJob> list(String username, Pageable pageable) {
        return jobs.findByUserUsernameOrderByCreatedAtDesc(username, pageable);
    }

    @Transactional
    public BatchJob retry(String username, String id) {
        BatchJob job = get(username, id);
        if (job.getStatus() != BatchJob.Status.FAILED) {
            throw new IllegalStateException("Only FAILED jobs can be retried; job is " + job.getStatus());
        }
        job.retry(Instant.now());
        return job;
    }

    @Transactional(readOnly = true)
    public List<PredictionResult> results(String username, String id, int limit) {
        get(username, id);
        return predictions.findByBatchIdAndUserUsernameOrderByCsvRowAsc(id, username).stream()
                .limit(limit)
                .map(PredictionResult::from)
                .toList();
    }

    /** Direct download link when the store supports it (S3); callers fall back to {@link #resultCsv}. */
    @Transactional(readOnly = true)
    public Optional<URI> resultUrl(String username, String id) {
        BatchJob job = requireSucceeded(username, id);
        return store.presignedGetUrl(job.getResultKey(), props.resultUrlTtl());
    }

    @Transactional(readOnly = true)
    public byte[] resultCsv(String username, String id) {
        return store.get(requireSucceeded(username, id).getResultKey());
    }

    private BatchJob requireSucceeded(String username, String id) {
        BatchJob job = get(username, id);
        if (job.getStatus() != BatchJob.Status.SUCCEEDED || job.getResultKey() == null) {
            throw new IllegalStateException("Job " + id + " has no result yet (status " + job.getStatus() + ")");
        }
        return job;
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String sha256(String s) {
        return sha256(s.getBytes(StandardCharsets.UTF_8));
    }
}
