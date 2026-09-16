package com.yuecai.fraud.modelclient;

import static com.yuecai.fraud.TestFixtures.FRAUD_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * The Resilience4j annotations only work through the Spring proxy, so this test goes through
 * the real application context and a fake HTTP model service.
 */
@SpringBootTest(properties = {
        "resilience4j.retry.instances.model.wait-duration=10ms",
        "resilience4j.retry.instances.model.enable-exponential-backoff=false"
})
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class ModelServiceClientResilienceTest {

    private static final String PREDICT = "http://model-service.test/predict";

    @Autowired ModelServiceClient client;
    @Autowired MockServerRestClientCustomizer customizer;
    MockRestServiceServer server;

    @BeforeEach
    void server() {
        server = customizer.getServer();
    }

    private static ModelFeatures features() {
        return ModelFeatures.of(100, 1, 10000.0, 10000.0, 0.0, 0.0, 10000.0);
    }

    @Test
    void retriesTransientFailuresThenSucceeds() {
        server.expect(times(2), requestTo(PREDICT)).andRespond(withServerError());
        server.expect(requestTo(PREDICT)).andRespond(withSuccess(FRAUD_JSON, MediaType.APPLICATION_JSON));

        ModelScore score = client.predict(features());

        assertThat(score.isFraud()).isTrue();
        server.verify(); // exactly 3 calls: 2 failures + 1 success
    }

    @Test
    void givesUpAfterThreeAttempts() {
        server.expect(times(3), requestTo(PREDICT)).andRespond(withServerError());

        assertThatThrownBy(() -> client.predict(features())).isInstanceOf(ModelServiceUnavailableException.class);
        server.verify();
    }

    @Test
    void doesNotRetryClientErrors() {
        server.expect(times(1), requestTo(PREDICT)).andRespond(withBadRequest());

        assertThatThrownBy(() -> client.predict(features()))
                .isInstanceOf(ModelServiceException.class)
                .isNotInstanceOf(ModelServiceUnavailableException.class);
        server.verify(); // one call, no retry on 400
    }
}
