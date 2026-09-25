package zoonotic_disease_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import zoonotic_disease_service.enums.Role;
import zoonotic_disease_service.user.User;
import zoonotic_disease_service.user.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByUsername("recorder1").isEmpty()) {

                User recorder = new User(
                        "recorder1",
                        passwordEncoder.encode("password123"),
                        Role.WARD_RECORDER,
                        "Ward 5",
                        "Harare"
                );

                userRepository.save(recorder);
            }

            if (userRepository.findByUsername("supervisor1").isEmpty()) {

                User supervisor = new User(
                        "supervisor1",
                        passwordEncoder.encode("password123"),
                        Role.PROVINCIAL_SUPERVISOR,
                        null,
                        "Harare"
                );

                userRepository.save(supervisor);
            }

            if (userRepository.findByUsername("national1").isEmpty()) {

                User national = new User(
                        "national1",
                        passwordEncoder.encode("password123"),
                        Role.NATIONAL_USER,
                        null,
                        null
                );

                userRepository.save(national);
            }
        };
    }
}