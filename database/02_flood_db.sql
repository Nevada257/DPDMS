-- =============================================================================
-- DPDMS - flood-service
-- Schema and seed data for the flood_db database (MySQL 8).
-- The service also creates / updates these tables itself on start-up
-- (spring.jpa.hibernate.ddl-auto=update); this script lets you inspect the
-- schema and load demo data. Safe to re-run (INSERT IGNORE on fixed ids).
--
-- Seeded incidents use the recorder accounts created by auth-service:
--   <hazard>_recorder (Ward 1). Records from other wards were captured by
--   ward officers. Mix of APPROVED and PENDING so the approval workflow,
--   dashboard and map can all be demonstrated.
-- =============================================================================
CREATE DATABASE IF NOT EXISTS flood_db;
USE flood_db;

CREATE TABLE IF NOT EXISTS flood_incident (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    ward                   VARCHAR(255),
    district               VARCHAR(255),
    province               VARCHAR(255),
    occurrence_date_time   VARCHAR(255),
    reporter               VARCHAR(255),
    severity               VARCHAR(255),   -- LOW | MEDIUM | HIGH | CRITICAL
    status                 VARCHAR(255),   -- operational: ONGOING | RECEDED
    approval_status        VARCHAR(255),   -- PENDING | APPROVED | REJECTED | CORRECTIONS_REQUESTED
    rejection_reason       VARCHAR(255),
    latitude               DOUBLE       NOT NULL,
    longitude              DOUBLE       NOT NULL,
    peak_water_level       DOUBLE       NOT NULL,  -- metres
    river_basin            VARCHAR(255),
    households_displaced   INT          NOT NULL,
    area_flooded           DOUBLE       NOT NULL,  -- hectares
    duration_of_inundation INT          NOT NULL,  -- days
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS flood_audit_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    incident_id  BIGINT,
    action       VARCHAR(255),   -- SUBMITTED | UPDATED | RESUBMITTED | APPROVED | REJECTED | CORRECTIONS_REQUESTED | DELETED
    performed_by VARCHAR(255),   -- username from the JWT
    details      VARCHAR(255),
    timestamp    DATETIME(6),
    PRIMARY KEY (id)
);

INSERT IGNORE INTO flood_incident (id, ward, district, province, occurrence_date_time, reporter, severity, status,
    approval_status, rejection_reason, latitude, longitude, peak_water_level, river_basin, households_displaced,
    area_flooded, duration_of_inundation) VALUES
(1, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2025-10-18T06:15', 'ward_officer', 'MEDIUM', 'RECEDED', 'APPROVED', NULL, -16.5075, 32.391, 2.6, 'Ruya', 40, 78.5, 4),
(2, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2025-11-01T16:15', 'ward_officer', 'CRITICAL', 'ONGOING', 'APPROVED', NULL, -16.50224, 32.38585, 4.0, 'Musengezi', 23, 45.4, 10),
(3, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2025-11-24T10:00', 'flood_recorder', 'LOW', 'ONGOING', 'APPROVED', NULL, -16.61134, 32.20786, 1.1, 'Nyadiri', 52, 58.8, 11),
(4, 'Ward 7', 'Rushinga', 'Mashonaland Central', '2025-12-07T06:30', 'ward_officer', 'CRITICAL', 'ONGOING', 'APPROVED', NULL, -16.55361, 32.04306, 4.2, 'Musengezi', 38, 94.3, 1),
(5, 'Ward 2', 'Rushinga', 'Mashonaland Central', '2025-12-18T14:00', 'ward_officer', 'LOW', 'ONGOING', 'APPROVED', NULL, -16.55559, 32.2922, 1.3, 'Mazowe', 54, 98.8, 2),
(6, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-01-23T13:00', 'flood_recorder', 'CRITICAL', 'ONGOING', 'APPROVED', NULL, -16.62886, 32.22053, 4.6, 'Mazowe', 8, 122.8, 5),
(7, 'Ward 6', 'Rushinga', 'Mashonaland Central', '2026-01-26T18:45', 'ward_officer', 'CRITICAL', 'RECEDED', 'APPROVED', NULL, -16.65873, 32.45618, 4.3, 'Mazowe', 2, 29.0, 1),
(8, 'Ward 2', 'Rushinga', 'Mashonaland Central', '2026-01-20T19:45', 'ward_officer', 'CRITICAL', 'ONGOING', 'APPROVED', NULL, -16.54508, 32.31893, 4.7, 'Mazowe', 44, 5.4, 7),
(9, 'Ward 5', 'Rushinga', 'Mashonaland Central', '2026-02-06T07:30', 'ward_officer', 'CRITICAL', 'ONGOING', 'APPROVED', NULL, -16.75607, 32.224, 4.7, 'Mazowe', 56, 5.7, 11),
(10, 'Ward 7', 'Rushinga', 'Mashonaland Central', '2026-02-26T10:45', 'ward_officer', 'HIGH', 'RECEDED', 'APPROVED', NULL, -16.57109, 32.05282, 3.1, 'Musengezi', 1, 19.9, 4),
(11, 'Ward 4', 'Rushinga', 'Mashonaland Central', '2026-03-05T19:45', 'ward_officer', 'MEDIUM', 'RECEDED', 'APPROVED', NULL, -16.47259, 32.36898, 2.2, 'Mazowe', 32, 165.8, 2),
(12, 'Ward 3', 'Rushinga', 'Mashonaland Central', '2026-03-02T06:15', 'ward_officer', 'CRITICAL', 'RECEDED', 'APPROVED', NULL, -16.71546, 32.09404, 4.5, 'Nyadiri', 20, 79.5, 4),
(13, 'Ward 8', 'Rushinga', 'Mashonaland Central', '2026-03-17T17:00', 'ward_officer', 'LOW', 'ONGOING', 'APPROVED', NULL, -16.87861, 32.33271, 1.9, 'Mazowe', 9, 100.8, 3),
(14, 'Ward 3', 'Rushinga', 'Mashonaland Central', '2026-05-02T06:45', 'ward_officer', 'CRITICAL', 'RECEDED', 'APPROVED', NULL, -16.69778, 32.11502, 4.6, 'Ruya', 21, 31.9, 1),
(15, 'Ward 5', 'Rushinga', 'Mashonaland Central', '2026-07-22T14:45', 'ward_officer', 'MEDIUM', 'RECEDED', 'APPROVED', NULL, -16.79054, 32.23536, 2.6, 'Nyadiri', 57, 117.4, 14),
(16, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-08-13T06:00', 'flood_recorder', 'CRITICAL', 'RECEDED', 'APPROVED', NULL, -16.63884, 32.1978, 4.2, 'Ruya', 36, 46.8, 12),
(17, 'Ward 7', 'Rushinga', 'Mashonaland Central', '2026-08-01T15:30', 'ward_officer', 'HIGH', 'ONGOING', 'APPROVED', NULL, -16.58192, 32.02568, 3.6, 'Mazowe', 48, 176.3, 2),
(18, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-09-25T07:30', 'flood_recorder', 'HIGH', 'ONGOING', 'PENDING', NULL, -16.618, 32.214, 3.6, 'Mazowe', 42, 95.0, 3),
(19, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-09-20T16:00', 'flood_recorder', 'MEDIUM', 'ONGOING', 'CORRECTIONS_REQUESTED', 'GPS points outside ward boundary - please re-capture', -16.4, 32.9, 2.1, 'Ruya', 5, 12.0, 2);

INSERT IGNORE INTO flood_audit_log (id, incident_id, action, performed_by, details, timestamp) VALUES
(1, 1, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2025-10-18 06:15:00'),
(2, 2, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2025-11-01 16:15:00'),
(3, 3, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2025-11-24 10:00:00'),
(4, 4, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2025-12-07 06:30:00'),
(5, 5, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2025-12-18 14:00:00'),
(6, 6, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-01-23 13:00:00'),
(7, 7, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-01-26 18:45:00'),
(8, 8, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-01-20 19:45:00'),
(9, 9, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-02-06 07:30:00'),
(10, 10, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-02-26 10:45:00'),
(11, 11, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-03-05 19:45:00'),
(12, 12, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-03-02 06:15:00'),
(13, 13, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-03-17 17:00:00'),
(14, 14, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-05-02 06:45:00'),
(15, 15, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-07-22 14:45:00'),
(16, 16, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-08-13 06:00:00'),
(17, 17, 'APPROVED', 'flood_supervisor', 'Flood incident approved', '2026-08-01 15:30:00');
