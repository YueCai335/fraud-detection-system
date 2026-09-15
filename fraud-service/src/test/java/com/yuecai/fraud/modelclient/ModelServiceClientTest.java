package com.yuecai.fraud.modelclient;

import static com.yuecai.fraud.TestFixtures.FRAUD_JSON;
import static com.yuecai.fraud.TestFixtures.LEGIT_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Exercises the HTTP contract with a fake model service (no Spring context needed). */
class ModelServiceClientTest {

    private static final String BASE = "http://model-service.test";

    private MockRestServiceServer server;
    private ModelServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        ModelServiceProperties props =
                new ModelServiceProperties(BASE, Duration.ofSeconds(1), Duration.ofSeconds(1), 2);
        client = new ModelServiceClient(builder.build(), props);
    }

    private static ModelFeatures features(int typeCode) {
        return ModelFeatures.of(1, typeCode, 100.0, 100.0, 0.0, 0.0, 100.0);
    }

    @Test
    void predictSendsSnakeCaseTypeCodeAndParsesResponse() {
        server.expect(requestTo(BASE + "/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.type_code").value(1))
                .andExpect(jsonPath("$.balanceDiffOrg").value(100.0))
                .andRespond(withSuccess(FRAUD_JSON, MediaType.APPLICATION_JSON));

        ModelScore score = client.predict(features(1));

        assertThat(score.isFraud()).isTrue();
        assertThat(score.probFraud()).isEqualTo(0.39);
        assertThat(score.reasons()).hasSize(3);
        server.verify();
    }

    @Test
    void batchIsChunkedAndReassembledInOrder() {
        String twoResults = "{\"results\":[" + LEGIT_JSON + "," + FRAUD_JSON + "]}";
        String oneResult = "{\"results\":[" + LEGIT_JSON + "]}";
        server.expect(requestTo(BASE + "/batch_predict"))
                .andExpect(jsonPath("$.records.length()").value(2))
                .andRespond(withSuccess(twoResults, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/batch_predict"))
                .andExpect(jsonPath("$.records.length()").value(1))
                .andRespond(withSuccess(oneResult, MediaType.APPLICATION_JSON));

        List<ModelScore> scores = client.predictBatch(List.of(features(3), features(1), features(3)));

        assertThat(scores).extracting(ModelScore::fraud).containsExactly(0, 1, 0);
        server.verify();
    }

    @Test
    void batchResultCountMismatchIsAnError() {
        server.expect(requestTo(BASE + "/batch_predict"))
                .andRespond(withSuccess("{\"results\":[" + LEGIT_JSON + "]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.predictBatch(List.of(features(3), features(1))))
                .isInstanceOf(ModelServiceException.class)
                .hasMessageContaining("1 results for 2 records");
    }

    @Test
    void serverErrorIsWrapped() {
        server.expect(requestTo(BASE + "/predict")).andRespond(withServerError());

        assertThatThrownBy(() -> client.predict(features(1)))
                .isInstanceOf(ModelServiceException.class)
                .hasMessageContaining("500");
    }

    @Test
    void timeoutIsWrapped() {
        server.expect(requestTo(BASE + "/predict"))
                .andRespond(req -> { throw new ResourceAccessException("read timed out", new SocketTimeoutException()); });

        assertThatThrownBy(() -> client.predict(features(1)))
                .isInstanceOf(ModelServiceException.class)
                .hasMessageContaining("unavailable or timed out");
    }

    @Test
    void healthProbeIsFalseWhenDown() {
        server.expect(requestTo(BASE + "/health")).andRespond(withServerError());
        assertThat(client.isHealthy()).isFalse();
    }
}
