-- ============================================================
-- V2__add_risk_score_and_accounts.sql
-- Augment fraud_ring_alerts with scoring and account data.
-- ============================================================

ALTER TABLE fraud_ring_alerts
    ADD COLUMN risk_score        NUMERIC(5, 2)  NOT NULL DEFAULT 0.00,
    ADD COLUMN involved_accounts TEXT           NOT NULL DEFAULT '';

-- Relax total_volume constraint to permit zero-volume detection results
ALTER TABLE fraud_ring_alerts
    DROP CONSTRAINT chk_alert_volume_positive;

ALTER TABLE fraud_ring_alerts
    ADD CONSTRAINT chk_alert_volume_non_negative CHECK (total_volume >= 0);

-- High-severity-first index for analyst dashboard pagination
CREATE INDEX idx_alert_risk_score ON fraud_ring_alerts (risk_score DESC);
