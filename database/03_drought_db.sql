-- =============================================================================
-- DPDMS - drought-service
-- Schema and seed data for the drought_db database (MySQL 8).
-- The service also creates / updates these tables itself on start-up
-- (spring.jpa.hibernate.ddl-auto=update); this script lets you inspect the
-- schema and load demo data. Safe to re-run (INSERT IGNORE on fixed ids).
--
-- Seeded incidents use the recorder accounts created by auth-service:
--   <hazard>_recorder (Ward 1). Records from other wards were captured by
--   ward officers. Mix of APPROVED and PENDING so the approval workflow,
--   dashboard and map can all be demonstrated.
-- =============================================================================
CREATE DATABASE IF NOT EXISTS drought_db;
USE drought_db;

CREATE TABLE IF NOT EXISTS drought_incidents (
    id                             BIGINT       NOT NULL AUTO_INCREMENT,
    ward                           VARCHAR(255),
    district                       VARCHAR(255),
    province                       VARCHAR(255),
    date_time_of_occurrence        DATETIME(6),
    reporter                       VARCHAR(255),
    severity                       VARCHAR(255),
    status                         VARCHAR(255),   -- PENDING | APPROVED | REJECTED | CORRECTION_REQUESTED
    latitude                       DOUBLE,
    longitude                      DOUBLE,
    rainfall_deficit_mm            DOUBLE,          -- vs seasonal norm
    consecutive_dry_days           INT,
    crop_failure_percentage        DOUBLE,
    people_facing_water_shortages  INT,
    livestock_mortality_count      INT,
    rejection_reason               VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS drought_audit_trail (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    incident_id   BIGINT,
    action        VARCHAR(255),
    old_status    VARCHAR(255),
    new_status    VARCHAR(255),
    performed_by  VARCHAR(255),
    reason        VARCHAR(255),
    performed_at  DATETIME(6),
    PRIMARY KEY (id)
);

INSERT IGNORE INTO drought_incidents (id, ward, district, province, date_time_of_occurrence, reporter, severity, status,
    latitude, longitude, rainfall_deficit_mm, consecutive_dry_days, crop_failure_percentage,
    people_facing_water_shortages, livestock_mortality_count, rejection_reason) VALUES
(1, 'Ward 2', 'Rushinga', 'Mashonaland Central', '2025-10-22 16:00:00', 'ward_officer', 'LOW', 'APPROVED', -16.53416, 32.31155, 61.3, 45, 24.4, 1064, 60, NULL),
(2, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2025-10-09 15:30:00', 'ward_officer', 'HIGH', 'APPROVED', -16.46473, 32.3881, 128.6, 24, 69.2, 805, 52, NULL),
(3, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2025-11-23 07:30:00', 'ward_officer', 'MEDIUM', 'APPROVED', -16.46419, 32.36321, 171.8, 56, 33.7, 769, 8, NULL),
(4, 'Ward 8', 'Rushinga', 'Mashonaland Central', '2025-11-19 18:45:00', 'ward_officer', 'CRITICAL', 'APPROVED', -16.87355, 32.34895, 80.1, 73, 83.1, 961, 47, NULL),
(5, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2026-07-27 16:15:00', 'ward_officer', 'HIGH', 'APPROVED', -16.46376, 32.35637, 180.0, 61, 54.9, 553, 36, NULL),
(6, 'Ward 2', 'Rushinga', 'Mashonaland Central', '2026-08-12 15:45:00', 'ward_officer', 'LOW', 'APPROVED', -16.55357, 32.32934, 131.5, 37, 27.4, 493, 43, NULL),
(7, 'Ward 2', 'Rushinga', 'Mashonaland Central', '2026-08-20 18:15:00', 'ward_officer', 'CRITICAL', 'APPROVED', -16.54689, 32.28788, 155.6, 24, 81.6, 972, 33, NULL),
(8, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-09-05 15:30:00', 'drought_recorder', 'CRITICAL', 'APPROVED', -16.61153, 32.2098, 104.8, 42, 81.2, 1363, 45, NULL),
(9, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-09-24 18:45:00', 'drought_recorder', 'HIGH', 'APPROVED', -16.60739, 32.22461, 42.0, 30, 62.2, 763, 23, NULL),
(10, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2026-09-17 06:30:00', 'ward_officer', 'HIGH', 'APPROVED', -16.48534, 32.35469, 65.8, 20, 56.4, 528, 33, NULL),
(11, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-09-24 09:00:00', 'drought_recorder', 'HIGH', 'PENDING', -16.625, 32.205, 160.0, 48, 62.5, 820, 14, NULL);

INSERT IGNORE INTO drought_audit_trail (id, incident_id, action, old_status, new_status, performed_by, reason, performed_at) VALUES
(1, 1, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2025-10-22 16:00:00'),
(2, 2, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2025-10-09 15:30:00'),
(3, 3, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2025-11-23 07:30:00'),
(4, 4, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2025-11-19 18:45:00'),
(5, 5, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2026-07-27 16:15:00'),
(6, 6, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2026-08-12 15:45:00'),
(7, 7, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2026-08-20 18:15:00'),
(8, 8, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2026-09-05 15:30:00'),
(9, 9, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2026-09-24 18:45:00'),
(10, 10, 'APPROVED', 'PENDING', 'APPROVED', 'drought_supervisor', NULL, '2026-09-17 06:30:00');
