package com.oop.disaster.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aggregates approved incidents from the five hazard services for the live
 * dashboard and map. It stores nothing itself: every request is answered from
 * the hazard services, using the caller's own token, so their access rules apply.
 */
@SpringBootApplication
public class DashboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardServiceApplication.class, args);
    }
}
