package com.oop.disaster.alert.config;

import com.oop.disaster.alert.model.Subscriber;
import com.oop.disaster.alert.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keeps the demo subscriber ("Provincial Duty Officer", all hazards) in line with
 * ALERT_DEMO_EMAIL and ALERT_DEMO_PHONE: created on first start, and updated on
 * every start, so changing dpdms.env is enough to receive the demo alerts.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedSubscribers(SubscriberRepository repo,
                                      @Value("${alerts.seed.name}") String name,
                                      @Value("${alerts.seed.email}") String email,
                                      @Value("${alerts.seed.phone}") String phone) {
        return args -> {
            Subscriber officer = repo.findAll().stream()
                    .filter(s -> name.equals(s.getName()))
                    .findFirst()
                    .orElseGet(() -> new Subscriber(name, email, phone, "ALL"));
            officer.setEmail(email);
            officer.setPhone(phone);
            officer.setActive(true);
            repo.save(officer);
        };
    }
}
