-- =============================================================================
-- DPDMS - mining-accident-service
-- Schema and seed data for the mining_accident_db database (MySQL 8).
-- The service also creates / updates these tables itself on start-up
-- (spring.jpa.hibernate.ddl-auto=update); this script lets you inspect the
-- schema and load demo data. Safe to re-run (INSERT IGNORE on fixed ids).
--
-- Seeded incidents use the recorder accounts created by auth-service:
--   <hazard>_recorder (Ward 1). Records from other wards were captured by
--   ward officers. Mix of APPROVED and PENDING so the approval workflow,
--   dashboard and map can all be demonstrated.
-- =============================================================================
CREATE DATABASE IF NOT EXISTS mining_accident_db;
USE mining_accident_db;

CREATE TABLE IF NOT EXISTS mining_accident (
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    ward                       VARCHAR(255),
    district                   VARCHAR(255),
    province                   VARCHAR(255),
    occurrence_date_time       DATETIME(6),
    reporter                   VARCHAR(255),
    severity                   VARCHAR(255),   -- LOW | MEDIUM | HIGH | CRITICAL
    status                     VARCHAR(255),   -- PENDING | APPROVED | REJECTED | CORRECTION_REQUESTED
    latitude                   DOUBLE,
    longitude                  DOUBLE,
    mine_name                  VARCHAR(255),
    mine_type                  VARCHAR(255),   -- FORMAL | ARTISANAL
    accident_type              VARCHAR(255),   -- COLLAPSE | GAS_EXPLOSION | FLOODING | FALL_OF_GROUND
    trapped_or_injured_miners  INT,
    fatalities                 INT,
    rescue_operations_ongoing  BIT(1),
    rejection_reason           VARCHAR(255),
    created_at                 DATETIME(6),
    updated_at                 DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    accident_id      BIGINT,
    actor            VARCHAR(255),
    action           VARCHAR(255),
    previous_status  VARCHAR(255),
    new_status       VARCHAR(255),
    details          VARCHAR(255),
    timestamp        DATETIME(6),
    PRIMARY KEY (id)
);

INSERT IGNORE INTO mining_accident (id, ward, district, province, occurrence_date_time, reporter, severity, status,
    latitude, longitude, mine_name, mine_type, accident_type, trapped_or_injured_miners, fatalities,
    rescue_operations_ongoing, rejection_reason, created_at, updated_at) VALUES
(1, 'Ward 3', 'Rushinga', 'Mashonaland Central', '2025-10-17 11:00:00', 'ward_officer', 'CRITICAL', 'APPROVED', -16.68878, 32.11395, 'Ruya River Diggings', 'ARTISANAL', 'COLLAPSE', 8, 2, 0, NULL, '2025-10-17 11:00:00', '2025-10-17 11:00:00'),
(2, 'Ward 5', 'Rushinga', 'Mashonaland Central', '2026-01-02 15:30:00', 'ward_officer', 'CRITICAL', 'APPROVED', -16.80405, 32.23715, 'Nyamatikiti Mine', 'FORMAL', 'FLOODING', 8, 3, 0, NULL, '2026-01-02 15:30:00', '2026-01-02 15:30:00'),
(3, 'Ward 3', 'Rushinga', 'Mashonaland Central', '2026-04-12 06:30:00', 'ward_officer', 'HIGH', 'APPROVED', -16.67999, 32.13057, 'Mukosa Alluvial Site', 'ARTISANAL', 'FLOODING', 7, 0, 0, NULL, '2026-04-12 06:30:00', '2026-04-12 06:30:00'),
(4, 'Ward 8', 'Rushinga', 'Mashonaland Central', '2026-05-17 15:45:00', 'ward_officer', 'CRITICAL', 'APPROVED', -16.86675, 32.34059, 'Chimanda Gold Claim', 'ARTISANAL', 'GAS_EXPLOSION', 4, 2, 0, NULL, '2026-05-17 15:45:00', '2026-05-17 15:45:00'),
(5, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-07-21 13:15:00', 'mining_recorder', 'CRITICAL', 'APPROVED', -16.63312, 32.19595, 'Nyamatikiti Mine', 'FORMAL', 'COLLAPSE', 5, 2, 0, NULL, '2026-07-21 13:15:00', '2026-07-21 13:15:00'),
(6, 'Ward 1', 'Rushinga', 'Mashonaland Central', '2026-09-26 05:40:00', 'mining_recorder', 'CRITICAL', 'PENDING', -16.622, 32.208, 'Chimanda Gold Claim', 'ARTISANAL', 'COLLAPSE', 6, 1, 1, NULL, '2026-09-26 06:00:00', '2026-09-26 06:00:00');

INSERT IGNORE INTO audit_log (id, accident_id, actor, action, previous_status, new_status, details, timestamp) VALUES
(1, 1, 'mining_supervisor', 'APPROVE', 'PENDING', 'APPROVED', 'Approved', '2025-10-17 11:00:00'),
(2, 2, 'mining_supervisor', 'APPROVE', 'PENDING', 'APPROVED', 'Approved', '2026-01-02 15:30:00'),
(3, 3, 'mining_supervisor', 'APPROVE', 'PENDING', 'APPROVED', 'Approved', '2026-04-12 06:30:00'),
(4, 4, 'mining_supervisor', 'APPROVE', 'PENDING', 'APPROVED', 'Approved', '2026-05-17 15:45:00'),
(5, 5, 'mining_supervisor', 'APPROVE', 'PENDING', 'APPROVED', 'Approved', '2026-07-21 13:15:00');
