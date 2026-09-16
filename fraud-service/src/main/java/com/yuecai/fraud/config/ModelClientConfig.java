package com.yuecai.fraud.config;

import com.yuecai.fraud.modelclient.ModelServiceProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ModelClientConfig {

    /**
     * Timeouts for the auto-configured {@link RestClient.Builder}. Setting them here (instead of
     * building our own request factory) keeps Boot in charge of the factory, so test slices such as
     * {@code @AutoConfigureMockRestServiceServer} can still swap it for a mock.
     */
    @Bean
    ClientHttpRequestFactorySettings clientHttpRequestFactorySettings(ModelServiceProperties props) {
        return ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(props.connectTimeout())
                .withReadTimeout(props.readTimeout());
    }

    @Bean
    RestClient modelRestClient(RestClient.Builder builder, ModelServiceProperties props) {
        return builder.baseUrl(props.baseUrl()).build();
    }
}
