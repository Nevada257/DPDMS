package com.oop.disaster.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fireServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FIRE SERVICE")
                        .description("Simple Fire Incident Management API")
                        .version("1.0"));
    }
}
