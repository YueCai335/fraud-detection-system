package com.yuecai.fraud.modelclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * HTTP client for the Python model service.
 *
 * <p>Replaces the JAX-WS + HttpURLConnection pair from the legacy system with a
 * single {@link RestClient} that has explicit connect/read timeouts. Every
 * failure mode (connection refused, timeout, non-2xx, malformed body) is mapped
 * to {@link ModelServiceException} so callers only have to handle one thing.
 *
 * <p>Transient failures ({@link ModelServiceUnavailableException}) are retried with
 * back-off and counted by a circuit breaker (Resilience4j instance {@code model}, see
 * application.yml). 4xx responses are not retried.
 */
@Component
public class ModelServiceClient {

    public static final String RESILIENCE_NAME = "model";

    private static final Logger log = LoggerFactory.getLogger(ModelServiceClient.class);

    private final RestClient restClient;
    private final int batchChunkSize;

    public ModelServiceClient(RestClient modelRestClient, ModelServiceProperties props) {
        this.restClient = modelRestClient;
        this.batchChunkSize = props.batchChunkSize();
    }

    @Retry(name = RESILIENCE_NAME)
    @CircuitBreaker(name = RESILIENCE_NAME)
    public ModelScore predict(ModelFeatures features) {
        ModelScore score = exchange("/predict", features, ModelScore.class);
        if (score == null) {
            throw new ModelServiceException("Model service returned an empty response");
        }
        return score;
    }

    /** Scores many transactions, chunking the request so a large CSV cannot produce one huge HTTP call. */
    @Retry(name = RESILIENCE_NAME)
    @CircuitBreaker(name = RESILIENCE_NAME)
    public List<ModelScore> predictBatch(List<ModelFeatures> features) {
        List<ModelScore> all = new ArrayList<>(features.size());
        for (int from = 0; from < features.size(); from += batchChunkSize) {
            List<ModelFeatures> chunk = features.subList(from, Math.min(from + batchChunkSize, features.size()));
            ModelBatchResponse response =
                    exchange("/batch_predict", Map.of("records", chunk), ModelBatchResponse.class);
            if (response == null || response.results() == null || response.results().size() != chunk.size()) {
                throw new ModelServiceException("Model service returned " +
                        (response == null || response.results() == null ? 0 : response.results().size())
                        + " results for " + chunk.size() + " records");
            }
            all.addAll(response.results());
        }
        return all;
    }

    /** Lightweight probe used by the actuator health indicator. */
    public boolean isHealthy() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            log.debug("Model service health probe failed: {}", e.getMessage());
            return false;
        }
    }

    private <T> T exchange(String path, Object body, Class<T> type) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException e) {
            log.warn("Model service {} returned {}: {}", path, e.getStatusCode(), e.getResponseBodyAsString());
            String msg = "Model service rejected the request (" + e.getStatusCode().value() + ")";
            if (e.getStatusCode().is5xxServerError()) {
                throw new ModelServiceUnavailableException(msg, e);
            }
            throw new ModelServiceException(msg, e);
        } catch (ResourceAccessException e) {
            log.warn("Model service {} unreachable: {}", path, e.getMessage());
            throw new ModelServiceUnavailableException("Model service is unavailable or timed out", e);
        }
    }
}
