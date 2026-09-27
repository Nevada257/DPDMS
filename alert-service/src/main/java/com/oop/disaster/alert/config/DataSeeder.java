package com.oop.disaster.alert.config;

import com.oop.disaster.alert.model.Subscriber;
import com.oop.disaster.alert.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Seeds one demo subscriber (all hazards) the first time the service starts. */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedSubscribers(SubscriberRepository repo,
                                      @Value("${alerts.seed.name}") String name,
                                      @Value("${alerts.seed.email}") String email,
                                      @Value("${alerts.seed.phone}") String phone) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Subscriber(name, email, phone, "ALL"));
            }
        };
    }
}
