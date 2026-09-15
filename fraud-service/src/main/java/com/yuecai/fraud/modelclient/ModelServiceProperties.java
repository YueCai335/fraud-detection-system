package com.yuecai.fraud.modelclient;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Connection settings for the Python model service (see application.yml, prefix {@code model-service}). */
@Validated
@ConfigurationProperties(prefix = "model-service")
public record ModelServiceProperties(
        @NotBlank String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        @Min(1) int batchChunkSize) {

    public ModelServiceProperties {
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(2);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(10);
        if (batchChunkSize == 0) batchChunkSize = 500;
    }
}
