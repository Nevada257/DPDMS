package com.oop.disaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FireServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FireServiceApplication.class, args);
    }
}
