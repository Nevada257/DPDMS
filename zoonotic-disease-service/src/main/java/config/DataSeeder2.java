package zoonotic_disease_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import zoonotic_disease_service.enums.Role;
import zoonotic_disease_service.user.User;
import zoonotic_disease_service.user.UserRepository;

@Configuration
public class DataSeeder2 {

    @Bean
    CommandLineRunner seedZoonoticUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            // Local profile for the provincial administrator (added later, so seeded independently)
            if (userRepository.findByUsername("provincial_admin").isEmpty()) {
                userRepository.save(new User(
                        "provincial_admin",
                        passwordEncoder.encode("password123"),
                        Role.PROVINCIAL_ADMIN,
                        null,
                        "Mashonaland Central"
                ));
            }

            if (userRepository.findByUsername("zoonotic_recorder").isPresent()) {
                System.out.println(">>> Zoonotic local users already exist. Skipping seed.");
                return;
            }

            userRepository.save(new User(
                    "zoonotic_recorder",
                    passwordEncoder.encode("password123"),
                    Role.WARD_RECORDER,
                    "Ward 1",
                    "Mashonaland Central"
            ));

            userRepository.save(new User(
                    "zoonotic_supervisor",
                    passwordEncoder.encode("password123"),
                    Role.PROVINCIAL_SUPERVISOR,
                    null,
                    "Mashonaland Central"
            ));

            userRepository.save(new User(
                    "national_user",
                    passwordEncoder.encode("password123"),
                    Role.NATIONAL_USER,
                    null,
                    null
            ));

            System.out.println(">>> Seeded 3 local zoonotic user accounts.");
        };
    }
}