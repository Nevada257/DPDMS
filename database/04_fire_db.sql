-- =============================================================================
-- DPDMS - fire-service
-- Schema and seed data for the fire_db database (MySQL 8).
-- The service also creates / updates these tables itself on start-up
-- (spring.jpa.hibernate.ddl-auto=update); this script lets you inspect the
-- schema and load demo data. Safe to re-run (INSERT IGNORE on fixed ids).
--
-- Seeded incidents use the recorder accounts created by auth-service:
--   <hazard>_recorder (Ward 1). Records from other wards were captured by
--   ward officers. Mix of APPROVED and PENDING so the approval workflow,
--   dashboard and map can all be demonstrated.
-- =============================================================================
CREATE DATABASE IF NOT EXISTS fire_db;
USE fire_db;

CREATE TABLE IF NOT EXISTS fire_incidents (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    ward                    VARCHAR(255),
    district                VARCHAR(255),
    province                VARCHAR(255),
    occurrence_time         DATETIME(6),
    reporter                VARCHAR(255),
    reporter_email          VARCHAR(255),
    reporter_phone          VARCHAR(255),
    severity                VARCHAR(255),   -- LOW | MEDIUM | HIGH | CRITICAL
    status                  VARCHAR(255),   -- PENDING | APPROVED | REJECTED | NEEDS_CORRECTION
    latitude                DOUBLE       NOT NULL,
    longitude               DOUBLE       NOT NULL,
    area_burned             DOUBLE       NOT NULL,  -- hectares
    suspected_cause         VARCHAR(255),           -- NATURAL | ACCIDENTAL | DELIBERATE
    injuries_or_fatalities  INT          NOT NULL,
    structures_destroyed    INT          NOT NULL,
    active                  BIT(1)       NOT NULL,  -- 1 = still burning, 0 = contained
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    incident_id BIGINT,
    changed_by  VARCHAR(255),
    changed_at  DATETIME(6),
    old_status  VARCHAR(255),
    new_status  VARCHAR(255),
    reason      VARCHAR(255),
    PRIMARY KEY (id)
);

INSERT IGNORE INTO fire_incidents (id, ward, district, province, occurrence_time, reporter, reporter_email, reporter_phone,
    severity, status, latitude, longitude, area_burned, suspected_cause, injuries_or_fatalities,
    structures_destroyed, active) VALUES
(1, 'Ward 8', 'Rushinga', 'Mashonaland Central', '2025-10-19 14:00:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.83135, 32.36593, 59.5, 'ACCIDENTAL', 2, 0, 0),
(2, 'Ward 5', 'Rushinga', 'Mashonaland Central', '2025-10-19 12:15:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.80661, 32.23644, 50.1, 'DELIBERATE', 1, 1, 0),
(3, 'Ward 3', 'Rushinga', 'Mashonaland Central', '2025-10-12 14:45:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.69272, 32.09969, 44.8, 'ACCIDENTAL', 2, 5, 0),
(4, 'Ward 8', 'Rushinga', 'Mashonaland Central', '2025-11-11 12:45:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.86282, 32.35787, 33.4, 'DELIBERATE', 2, 3, 0),
(5, 'Ward 8', 'Rushinga', 'Mashonaland Central', '2025-12-22 15:00:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.82512, 32.34394, 58.8, 'DELIBERATE', 2, 6, 0),
(6, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2026-04-05 09:45:00', 'ward_officer', NULL, NULL, 'MEDIUM', 'APPROVED', -16.48954, 32.35352, 8.3, 'DELIBERATE', 3, 2, 0),
(7, 'Ward 7', 'Rushinga', 'Mashonaland Central', '2026-06-18 16:00:00', 'ward_officer', NULL, NULL, 'CRITICAL', 'APPROVED', -16.58719, 32.07738, 89.3, 'DELIBERATE', 0, 5, 0),
(8, 'Ward 6', 'Rushinga', 'Mashonaland Central', '2026-07-14 15:30:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.68709, 32.42482, 41.2, 'DELIBERATE', 2, 2, 0),
(9, 'Ward 2', 'Rushinga', 'Mashonaland Central', '2026-07-01 14:30:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.57285, 32.31611, 56.0, 'ACCIDENTAL', 2, 2, 1),
(10, 'Ward 8', 'Rushinga', 'Mashonaland Central', '2026-07-11 10:30:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.8408, 32.33834, 36.5, 'ACCIDENTAL', 3, 4, 0),
(11, 'Ward 6', 'Rushinga', 'Mashonaland Central', '2026-08-12 06:00:00', 'ward_officer', NULL, NULL, 'MEDIUM', 'APPROVED', -16.64732, 32.46263, 13.5, 'ACCIDENTAL', 1, 3, 1),
(12, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-08-14 09:15:00', 'fire_recorder', NULL, NULL, 'HIGH', 'APPROVED', -16.62054, 32.2084, 69.1, 'ACCIDENTAL', 1, 0, 1),
(13, 'Ward 2', 'Rushinga', 'Mashonaland Central', '2026-09-19 16:15:00', 'ward_officer', NULL, NULL, 'LOW', 'APPROVED', -16.54392, 32.29026, 3.4, 'ACCIDENTAL', 0, 6, 0),
(14, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2026-09-17 10:45:00', 'ward_officer', NULL, NULL, 'HIGH', 'APPROVED', -16.45118, 32.38281, 68.5, 'NATURAL', 1, 4, 0),
(15, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-09-26 13:20:00', 'fire_recorder', NULL, NULL, 'HIGH', 'PENDING', -16.63, 32.22, 42.0, 'DELIBERATE', 1, 2, 1);

INSERT IGNORE INTO audit_logs (id, incident_id, changed_by, changed_at, old_status, new_status, reason) VALUES
(1, 1, 'fire_supervisor', '2025-10-19 14:00:00', 'PENDING', 'APPROVED', 'Approved'),
(2, 2, 'fire_supervisor', '2025-10-19 12:15:00', 'PENDING', 'APPROVED', 'Approved'),
(3, 3, 'fire_supervisor', '2025-10-12 14:45:00', 'PENDING', 'APPROVED', 'Approved'),
(4, 4, 'fire_supervisor', '2025-11-11 12:45:00', 'PENDING', 'APPROVED', 'Approved'),
(5, 5, 'fire_supervisor', '2025-12-22 15:00:00', 'PENDING', 'APPROVED', 'Approved'),
(6, 6, 'fire_supervisor', '2026-04-05 09:45:00', 'PENDING', 'APPROVED', 'Approved'),
(7, 7, 'fire_supervisor', '2026-06-18 16:00:00', 'PENDING', 'APPROVED', 'Approved'),
(8, 8, 'fire_supervisor', '2026-07-14 15:30:00', 'PENDING', 'APPROVED', 'Approved'),
(9, 9, 'fire_supervisor', '2026-07-01 14:30:00', 'PENDING', 'APPROVED', 'Approved'),
(10, 10, 'fire_supervisor', '2026-07-11 10:30:00', 'PENDING', 'APPROVED', 'Approved'),
(11, 11, 'fire_supervisor', '2026-08-12 06:00:00', 'PENDING', 'APPROVED', 'Approved'),
(12, 12, 'fire_supervisor', '2026-08-14 09:15:00', 'PENDING', 'APPROVED', 'Approved'),
(13, 13, 'fire_supervisor', '2026-09-19 16:15:00', 'PENDING', 'APPROVED', 'Approved'),
(14, 14, 'fire_supervisor', '2026-09-17 10:45:00', 'PENDING', 'APPROVED', 'Approved');
