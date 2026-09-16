package com.yuecai.fraud.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Where batch job files live. S3 in AWS, MinIO in docker compose, in-memory in tests —
 * the application code never knows which.
 */
public interface ObjectStore {

    void put(String key, byte[] content, String contentType);

    byte[] get(String key);

    boolean exists(String key);

    /** A time-limited direct download link, when the backend supports it (S3 does; memory does not). */
    Optional<URI> presignedGetUrl(String key, Duration ttl);
}
