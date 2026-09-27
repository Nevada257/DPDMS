package com.dpdms.auth_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds one recorder and one provincial supervisor per hazard, a provincial
 * administrator and a national user. Each account is created only if its
 * username does not exist yet, so new accounts are added to an existing database.
 *
 * Roles:
 *   RECORDER   - ward-level data capturer, scoped to exactly one (ward, hazard)
 *   SUPERVISOR - provincial supervisor, approves one hazard only
 *   ADMIN      - provincial administrator, may view pending records (read only)
 *   NATIONAL   - read-only, approved records across all hazards
 */
@Configuration
public class DataSeeder {

    private static final String DEFAULT_PASSWORD = "password123";

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String[] hazards = {"FLOOD", "DROUGHT", "FIRE", "ZOONOTIC", "MINING"};
            int created = 0;

            for (String hazard : hazards) {
                String prefix = hazard.toLowerCase();
                created += createIfMissing(userRepository, passwordEncoder,
                        prefix + "_recorder", "RECORDER", hazard, "Ward 1");
                created += createIfMissing(userRepository, passwordEncoder,
                        prefix + "_supervisor", "SUPERVISOR", hazard, null);
            }

            // Second-ward recorder, used to demonstrate ward-level scoping
            created += createIfMissing(userRepository, passwordEncoder,
                    "flood_recorder_w2", "RECORDER", "FLOOD", "Ward 2");

            created += createIfMissing(userRepository, passwordEncoder,
                    "provincial_admin", "ADMIN", "ALL", null);
            created += createIfMissing(userRepository, passwordEncoder,
                    "national_user", "NATIONAL", "ALL", null);

            System.out.println(">>> Seeded " + created + " new user account(s).");
        };
    }

    private int createIfMissing(UserRepository repo, PasswordEncoder encoder,
                                String username, String role,
                                String hazardScope, String ward) {

        if (repo.findByUsername(username) != null) {
            return 0;
        }
        repo.save(new User(username, encoder.encode(DEFAULT_PASSWORD), role, hazardScope, ward));
        return 1;
    }
}
