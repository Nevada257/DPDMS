package com.oop.disaster.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI fireServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fire Service System")
                        .description("Fire incident management and alert system")
                        .version("1.0"));
    }
}