package com.yuecai.fraud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI fraudServiceOpenApi() {
        String scheme = "basicAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Fraud Detection API")
                        .version("v1")
                        .description("Scores PaySim-style transactions for fraud. "
                                + "Single and batch (CSV) scoring, backed by a Python model service."))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
