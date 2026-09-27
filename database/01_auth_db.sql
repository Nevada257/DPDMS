-- =============================================================================
-- DPDMS - auth-service
-- Schema and seed data for the auth_db database (MySQL 8).
-- The service also creates / updates these tables itself on start-up
-- (spring.jpa.hibernate.ddl-auto=update); this script lets you inspect the
-- schema and load demo data. Safe to re-run (INSERT IGNORE on fixed ids).
--
-- Seeded incidents use the recorder accounts created by auth-service:
--   <hazard>_recorder (Ward 1). Records from other wards were captured by
--   ward officers. Mix of APPROVED and PENDING so the approval workflow,
--   dashboard and map can all be demonstrated.
-- =============================================================================
CREATE DATABASE IF NOT EXISTS auth_db;
USE auth_db;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,  -- BCrypt hash, never plain text
    role          VARCHAR(255) NOT NULL,  -- RECORDER | SUPERVISOR | ADMIN | NATIONAL
    hazard_scope  VARCHAR(255),           -- FLOOD | DROUGHT | FIRE | ZOONOTIC | MINING | ALL
    ward          VARCHAR(255),           -- recorders only
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
);

-- User accounts are seeded by auth-service on start-up (DataSeeder), because
-- passwords must be BCrypt-hashed by the application. Default password for
-- every demo account: password123
--
--   flood_recorder / drought_recorder / fire_recorder / zoonotic_recorder /
--   mining_recorder        RECORDER    one hazard, Ward 1
--   flood_recorder_w2      RECORDER    FLOOD, Ward 2 (to demo ward scoping)
--   <hazard>_supervisor    SUPERVISOR  one hazard, province-wide
--   provincial_admin       ADMIN       ALL hazards, read-only incl. pending
--   national_user          NATIONAL    ALL hazards, read-only, approved only
