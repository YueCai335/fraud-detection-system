package com.yuecai.fraud.config;

import com.yuecai.fraud.storage.InMemoryObjectStore;
import com.yuecai.fraud.storage.ObjectStore;
import com.yuecai.fraud.storage.S3ObjectStore;
import com.yuecai.fraud.storage.StorageProperties;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "memory", matchIfMissing = true)
    ObjectStore inMemoryObjectStore() {
        log.info("Object store: in-memory (files do not survive a restart)");
        return new InMemoryObjectStore();
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "s3")
    ObjectStore s3ObjectStore(StorageProperties props) {
        if (props.bucket() == null || props.bucket().isBlank()) {
            throw new IllegalStateException("storage.bucket is required when storage.type=s3");
        }
        AwsCredentialsProvider credentials = props.accessKey() != null && !props.accessKey().isBlank()
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey(), props.secretKey()))
                : DefaultCredentialsProvider.builder().build();
        Region region = Region.of(props.region() == null || props.region().isBlank() ? "us-east-1" : props.region());
        boolean custom = props.endpoint() != null && !props.endpoint().isBlank();

        var s3Builder = S3Client.builder()
                .region(region)
                .credentialsProvider(credentials)
                // MinIO serves buckets as /bucket/key, not bucket.host/key
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(custom).build());
        if (custom) {
            s3Builder.endpointOverride(URI.create(props.endpoint()));
        }
        S3Client s3 = s3Builder.build();

        // Presigned URLs are opened by the user's browser, so they must use the address the
        // browser can reach (e.g. localhost:9000, while the app talks to minio:9000 inside compose).
        String presignEndpoint = props.publicEndpoint() != null && !props.publicEndpoint().isBlank()
                ? props.publicEndpoint() : props.endpoint();
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(custom).build());
        if (presignEndpoint != null && !presignEndpoint.isBlank()) {
            presignerBuilder.endpointOverride(URI.create(presignEndpoint));
        }
        S3Presigner presigner = presignerBuilder.build();

        if (props.createBucket()) {
            ensureBucket(s3, props.bucket());
        }
        log.info("Object store: S3 bucket '{}'{}", props.bucket(), custom ? " at " + props.endpoint() : "");
        return new S3ObjectStore(s3, presigner, props.bucket());
    }

    /** Local convenience (MinIO). In AWS the bucket is created by Terraform. */
    private static void ensureBucket(S3Client s3, String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            try {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created bucket '{}'", bucket);
            } catch (BucketAlreadyOwnedByYouException ignored) {
                // raced with another instance
            }
        }
    }
}
