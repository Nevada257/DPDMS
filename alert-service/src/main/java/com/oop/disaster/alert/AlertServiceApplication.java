package com.oop.disaster.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Shared alerting service for all DPDMS hazard services.
 * Hazard services post incidents here; this service applies the alerting
 * criteria and dispatches email / WhatsApp alerts asynchronously.
 */
@SpringBootApplication
@EnableAsync
public class AlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}
