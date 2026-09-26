package com.dpdms.auth_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                System.out.println(">>> Users already exist. Skipping seed.");
                return;
            }

            createUser(userRepository, passwordEncoder, "flood_recorder", "password123", "RECORDER", "FLOOD", "Ward 1");
            createUser(userRepository, passwordEncoder, "flood_supervisor", "password123", "SUPERVISOR", "FLOOD", null);

            createUser(userRepository, passwordEncoder, "drought_recorder", "password123", "RECORDER", "DROUGHT", "Ward 1");
            createUser(userRepository, passwordEncoder, "drought_supervisor", "password123", "SUPERVISOR", "DROUGHT", null);

            createUser(userRepository, passwordEncoder, "fire_recorder", "password123", "RECORDER", "FIRE", "Ward 1");
            createUser(userRepository, passwordEncoder, "fire_supervisor", "password123", "SUPERVISOR", "FIRE", null);

            createUser(userRepository, passwordEncoder, "mining_recorder", "password123", "RECORDER", "MINING", "Ward 1");
            createUser(userRepository, passwordEncoder, "mining_supervisor", "password123", "SUPERVISOR", "MINING", null);

            createUser(userRepository, passwordEncoder, "national_user", "password123", "NATIONAL", "ALL", null);

            System.out.println(">>> Seeded 9 test user accounts.");
        };
    }

    private void createUser(UserRepository repo, PasswordEncoder encoder,
                             String username, String rawPassword, String role,
                             String hazardScope, String ward) {

        User user = new User(username, encoder.encode(rawPassword), role, hazardScope, ward);
        repo.save(user);
    }
}