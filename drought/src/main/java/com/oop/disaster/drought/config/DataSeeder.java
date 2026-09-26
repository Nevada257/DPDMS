package com.oop.disaster.drought.config;

import com.oop.disaster.drought.entity.DroughtIncident;
import com.oop.disaster.drought.repository.DroughtIncidentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDroughtData(DroughtIncidentRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                System.out.println(">>> Drought DB already has data. Skipping seed.");
                return;
            }

            repo.saveAll(List.of(
                    incident("Ward 1", "Rushinga", "Mashonaland Central",
                            -16.7500, 31.9500, "HIGH", "APPROVED",
                            110.0, 40, 65.0, 280, 12, "recorder1"),

                    incident("Ward 2", "Rushinga", "Mashonaland Central",
                            -16.7800, 31.9800, "MEDIUM", "APPROVED",
                            80.5, 30, 45.0, 150, 5, "recorder2"),

                    incident("Ward 3", "Mt Darwin", "Mashonaland Central",
                            -16.7833, 31.5833, "CRITICAL", "APPROVED",
                            150.0, 60, 85.0, 600, 40, "recorder3"),

                    incident("Ward 7", "Mudzi", "Mashonaland Central",
                            -17.0667, 32.2333, "LOW", "APPROVED",
                            45.0, 15, 20.0, 60, 2, "recorder1"),

                    incident("Ward 4", "Rushinga", "Mashonaland Central",
                            -16.8000, 32.0000, "HIGH", "PENDING",
                            95.0, 35, 55.0, 200, 8, "recorder1")
            ));

            System.out.println(">>> Seeded 5 drought incidents.");
        };
    }

    private DroughtIncident incident(String ward, String district, String province,
                                     double lat, double lon, String severity, String status,
                                     double rainfallDeficit, int dryDays, double cropFailure,
                                     int waterShortage, int livestock, String reporter) {
        DroughtIncident i = new DroughtIncident();
        i.setWard(ward);
        i.setDistrict(district);
        i.setProvince(province);
        i.setDateTimeOfOccurrence(LocalDateTime.now().minusDays(3));
        i.setReporter(reporter);
        i.setSeverity(severity);
        i.setStatus(status);
        i.setLatitude(lat);
        i.setLongitude(lon);
        i.setRainfallDeficitMm(rainfallDeficit);
        i.setConsecutiveDryDays(dryDays);
        i.setCropFailurePercentage(cropFailure);
        i.setPeopleFacingWaterShortages(waterShortage);
        i.setLivestockMortalityCount(livestock);
        return i;
    }
}