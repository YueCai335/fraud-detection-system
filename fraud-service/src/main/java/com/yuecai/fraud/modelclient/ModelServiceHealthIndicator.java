package com.yuecai.fraud.modelclient;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Surfaces the downstream model service in {@code GET /actuator/health} as {@code components.modelService}. */
@Component("modelService")
public class ModelServiceHealthIndicator implements HealthIndicator {

    private final ModelServiceClient client;
    private final ModelServiceProperties props;

    public ModelServiceHealthIndicator(ModelServiceClient client, ModelServiceProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public Health health() {
        Health.Builder builder = client.isHealthy() ? Health.up() : Health.down();
        return builder.withDetail("baseUrl", props.baseUrl()).build();
    }
}
