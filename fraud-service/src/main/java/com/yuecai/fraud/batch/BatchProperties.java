package com.yuecai.fraud.batch;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "batch")
public record BatchProperties(Worker worker, Duration resultUrlTtl) {

    public BatchProperties {
        if (worker == null) worker = new Worker(true, Duration.ofSeconds(5), Duration.ofMinutes(5), 500, 100_000);
        if (resultUrlTtl == null) resultUrlTtl = Duration.ofMinutes(15);
    }

    public record Worker(boolean enabled, Duration pollInterval, Duration staleAfter, int chunkSize, int maxRows) {
        public Worker {
            if (pollInterval == null) pollInterval = Duration.ofSeconds(5);
            if (staleAfter == null) staleAfter = Duration.ofMinutes(5);
            if (chunkSize <= 0) chunkSize = 500;
            if (maxRows <= 0) maxRows = 100_000;
        }
    }
}
