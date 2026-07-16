-- ============================================================
-- V1__init_looplock_schema.sql
-- Core domain tables for the LoopLock fraud detection engine.
-- ============================================================

CREATE TABLE transaction_records (
    id                     UUID         NOT NULL,
    source_account_id      VARCHAR(255) NOT NULL,
    destination_account_id VARCHAR(255) NOT NULL,
    amount                 NUMERIC(19, 4) NOT NULL,
    timestamp              TIMESTAMPTZ  NOT NULL,
    processed              BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_transaction_records PRIMARY KEY (id),
    CONSTRAINT chk_txn_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_txn_no_self_loop    CHECK (source_account_id <> destination_account_id)
);

CREATE INDEX idx_txn_source      ON transaction_records (source_account_id);
CREATE INDEX idx_txn_destination ON transaction_records (destination_account_id);
CREATE INDEX idx_txn_processed   ON transaction_records (processed);

-- ============================================================

CREATE TABLE fraud_ring_alerts (
    id              UUID           NOT NULL,
    ring_identifier VARCHAR(255)   NOT NULL,
    total_volume    NUMERIC(19, 4) NOT NULL,
    node_count      INTEGER        NOT NULL,
    detected_at     TIMESTAMPTZ    NOT NULL,

    CONSTRAINT pk_fraud_ring_alerts    PRIMARY KEY (id),
    CONSTRAINT chk_alert_node_count    CHECK (node_count >= 2),
    CONSTRAINT chk_alert_volume_positive CHECK (total_volume > 0)
);

CREATE INDEX idx_alert_ring_id     ON fraud_ring_alerts (ring_identifier);
CREATE INDEX idx_alert_detected_at ON fraud_ring_alerts (detected_at);
