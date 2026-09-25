package com.yuecai.fraud.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code storage.type=memory} (default; tests, plain local runs) or {@code storage.type=s3}.
 * For S3: {@code bucket} is required; {@code endpoint}/{@code public-endpoint}/credentials are
 * only for S3-compatible servers such as LocalStack. In AWS the default credential chain (task role) is used.
 */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String type,
        String bucket,
        String region,
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        boolean createBucket) {

    public StorageProperties {
        if (type == null || type.isBlank()) type = "memory";
    }
}
