-- =============================================================================
-- DPDMS - zoonotic-disease-service
-- Schema and seed data for the zoonotic_db database (MySQL 8).
-- The service also creates / updates these tables itself on start-up
-- (spring.jpa.hibernate.ddl-auto=update); this script lets you inspect the
-- schema and load demo data. Safe to re-run (INSERT IGNORE on fixed ids).
--
-- Seeded incidents use the recorder accounts created by auth-service:
--   <hazard>_recorder (Ward 1). Records from other wards were captured by
--   ward officers. Mix of APPROVED and PENDING so the approval workflow,
--   dashboard and map can all be demonstrated.
-- =============================================================================
CREATE DATABASE IF NOT EXISTS zoonotic_db;
USE zoonotic_db;

CREATE TABLE IF NOT EXISTS zoonotic_incidents (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    ward                    VARCHAR(255),
    district                VARCHAR(255),
    province                VARCHAR(255),
    occurrence_date_time    DATETIME(6),
    reporter                VARCHAR(255),
    severity                VARCHAR(255),   -- LOW | MEDIUM | HIGH | CRITICAL
    status                  VARCHAR(255),   -- PENDING | APPROVED | REJECTED | CORRECTION_REQUIRED
    latitude                DOUBLE,
    longitude               DOUBLE,
    disease_name            VARCHAR(255),   -- e.g. Anthrax, Rabies, Brucellosis
    animal_species          VARCHAR(255),
    confirmed_human_cases   INT,
    confirmed_animal_cases  INT,
    event_classification    VARCHAR(255),   -- CLUSTER | OUTBREAK
    created_by_username     VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS audit_trails (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    incident_id      BIGINT,
    action           VARCHAR(255),
    previous_status  VARCHAR(255),
    new_status       VARCHAR(255),
    performed_by     VARCHAR(255),
    performed_at     DATETIME(6),
    PRIMARY KEY (id)
);

-- Local user profiles (ward / province) used by this service's own scope checks.
-- Seeded on start-up by DataSeeder2 with BCrypt-hashed passwords.
CREATE TABLE IF NOT EXISTS users (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    username  VARCHAR(255) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    role      VARCHAR(255) NOT NULL,  -- WARD_RECORDER | PROVINCIAL_SUPERVISOR | PROVINCIAL_ADMIN | NATIONAL_USER
    ward      VARCHAR(255),
    province  VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_zoonotic_users_username (username)
);

INSERT IGNORE INTO zoonotic_incidents (id, ward, district, province, occurrence_date_time, reporter, severity, status,
    latitude, longitude, disease_name, animal_species, confirmed_human_cases, confirmed_animal_cases,
    event_classification, created_by_username) VALUES
(1, 'Ward 5', 'Rushinga', 'Mashonaland Central', '2025-11-22 14:00:00', 'ward_officer', 'LOW', 'APPROVED', -16.76943, 32.22668, 'Rabies', 'Dogs', 0, 20, 'CLUSTER', 'ward_officer'),
(2, 'Ward 7', 'Rushinga', 'Mashonaland Central', '2026-02-09 15:30:00', 'ward_officer', 'LOW', 'APPROVED', -16.6092, 32.07853, 'Brucellosis', 'Goats', 0, 14, 'CLUSTER', 'ward_officer'),
(3, 'Ward 5', 'Rushinga', 'Mashonaland Central', '2026-03-20 11:00:00', 'ward_officer', 'HIGH', 'APPROVED', -16.78878, 32.23926, 'Rabies', 'Dogs', 2, 15, 'CLUSTER', 'ward_officer'),
(4, 'Ward 5', 'Rushinga', 'Mashonaland Central', '2026-04-25 07:30:00', 'ward_officer', 'LOW', 'APPROVED', -16.75723, 32.24868, 'Rabies', 'Dogs', 0, 20, 'CLUSTER', 'ward_officer'),
(5, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2026-05-05 06:00:00', 'ward_officer', 'LOW', 'APPROVED', -16.48811, 32.37593, 'Anthrax', 'Cattle', 0, 22, 'CLUSTER', 'ward_officer'),
(6, 'Ward 3', 'Rushinga', 'Mashonaland Central', '2026-06-05 16:30:00', 'ward_officer', 'CRITICAL', 'APPROVED', -16.68566, 32.10874, 'Rabies', 'Dogs', 4, 26, 'OUTBREAK', 'ward_officer'),
(7, 'Ward 3', 'Rushinga', 'Mashonaland Central', '2026-08-22 19:45:00', 'ward_officer', 'CRITICAL', 'APPROVED', -16.6746, 32.10988, 'Brucellosis', 'Goats', 4, 8, 'OUTBREAK', 'ward_officer'),
(8, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-09-23 10:10:00', 'zoonotic_recorder', 'HIGH', 'PENDING', -16.615, 32.2, 'Anthrax', 'Cattle', 2, 11, 'CLUSTER', 'zoonotic_recorder');

INSERT IGNORE INTO audit_trails (id, incident_id, action, previous_status, new_status, performed_by, performed_at) VALUES
(1, 1, 'APPROVED', 'PENDING', 'APPROVED', 'zoonotic_supervisor', '2025-11-22 14:00:00'),
(2, 2, 'APPROVED', 'PENDING', 'APPROVED', 'zoonotic_supervisor', '2026-02-09 15:30:00'),
(3, 3, 'APPROVED', 'PENDING', 'APPROVED', 'zoonotic_supervisor', '2026-03-20 11:00:00'),
(4, 4, 'APPROVED', 'PENDING', 'APPROVED', 'zoonotic_supervisor', '2026-04-25 07:30:00'),
(5, 5, 'APPROVED', 'PENDING', 'APPROVED', 'zoonotic_supervisor', '2026-05-05 06:00:00'),
(6, 6, 'APPROVED', 'PENDING', 'APPROVED', 'zoonotic_supervisor', '2026-06-05 16:30:00'),
(7, 7, 'APPROVED', 'PENDING', 'APPROVED', 'zoonotic_supervisor', '2026-08-22 19:45:00');
