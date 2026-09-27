-- =============================================================================
-- DPDMS - alert-service
-- Schema and seed data for the alert_db database (MySQL 8).
-- The service also creates / updates these tables itself on start-up
-- (spring.jpa.hibernate.ddl-auto=update); this script lets you inspect the
-- schema and load demo data. Safe to re-run (INSERT IGNORE on fixed ids).
--
-- =============================================================================
CREATE DATABASE IF NOT EXISTS alert_db;
USE alert_db;

CREATE TABLE IF NOT EXISTS alert_subscriber (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    name     VARCHAR(255),
    email    VARCHAR(255),
    phone    VARCHAR(255),   -- WhatsApp number, international format
    hazards  VARCHAR(255),   -- comma-separated hazards or ALL
    active   BIT(1)       NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS alert_log (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    hazard           VARCHAR(20)   NOT NULL,
    incident_id      BIGINT,
    ward             VARCHAR(255),
    district         VARCHAR(255),
    severity         VARCHAR(255),
    channel          VARCHAR(20)   NOT NULL,  -- EMAIL | WHATSAPP
    recipient        VARCHAR(255)  NOT NULL,
    message          VARCHAR(1000),
    trigger_reason   VARCHAR(255),
    delivery_status  VARCHAR(20)   NOT NULL,  -- SENT | SIMULATED | FAILED
    error_message    VARCHAR(500),
    attempts         INT           NOT NULL DEFAULT 1,
    triggered_by     VARCHAR(255),
    sent_at          DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_alert_hazard (hazard)
);

INSERT IGNORE INTO alert_subscriber (id, name, email, phone, hazards, active) VALUES
(1, 'Provincial Duty Officer', 'duty.officer@example.org', '+263770000000', 'ALL', 1),
(2, 'District Civil Protection - Rushinga', 'dcp.rushinga@example.org', '+263770000001', 'FLOOD,FIRE,MINING', 0),
(3, 'District Veterinary Officer', 'dvo.rushinga@example.org', '+263770000002', 'ZOONOTIC', 0);

-- Subscribers 2 and 3 are examples of hazard-specific recipients with placeholder
-- contacts, so they start inactive (no failed sends to fake numbers). Give them real
-- details and set active = 1 to use them. Subscriber 1 is kept in line with
-- ALERT_DEMO_EMAIL / ALERT_DEMO_PHONE by alert-service on every start.
UPDATE alert_subscriber SET active = 0 WHERE id IN (2, 3) AND email LIKE '%@example.org';

-- Older databases: add the delivery-attempts column if it is missing.
SET @has_attempts = (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = 'alert_db' AND TABLE_NAME = 'alert_log' AND COLUMN_NAME = 'attempts');
SET @sql = IF(@has_attempts = 0, 'ALTER TABLE alert_log ADD COLUMN attempts INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
