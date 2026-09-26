package com.oop.disaster.drought.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI droughtServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Drought Service API")
                        .description("REST API for the Rushinga Provincial Disaster Monitoring "
                                + "and Management System (DPDMS) — Drought Hazard")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OOP Group - Drought Team")
                                .email("drought-team@example.com")));
    }
}