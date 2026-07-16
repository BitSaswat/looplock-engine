package com.example.looplock_engine.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persists the result of a confirmed fraud ring detection event.
 *
 * The ringIdentifier is a sorted, pipe-delimited list of account IDs,
 * used as a stable business key for deduplication.
 */
@Entity
@Table(
    name = "fraud_ring_alerts",
    indexes = {
        @Index(name = "idx_alert_ring_id",    columnList = "ring_identifier"),
        @Index(name = "idx_alert_detected_at", columnList = "detected_at"),
        @Index(name = "idx_alert_risk_score",  columnList = "risk_score")
    }
)
public class FraudRingAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Stable business key for deduplication. */
    @Column(name = "ring_identifier", nullable = false)
    private String ringIdentifier;

    /** Sum of all transaction amounts within the detected ring. */
    @Column(name = "total_volume", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalVolume;

    /** Number of distinct accounts (nodes) participating in the ring. */
    @Column(name = "node_count", nullable = false)
    private Integer nodeCount;

    /** Composite risk severity score in [0.0, 100.0]. Higher = more suspicious. */
    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    /** Account IDs in detection order, stored as a pipe-delimited string. */
    @Convert(converter = StringListConverter.class)
    @Column(name = "involved_accounts", nullable = false, columnDefinition = "TEXT")
    private List<String> involvedAccounts;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    protected FraudRingAlert() {}

    public FraudRingAlert(String ringIdentifier, BigDecimal totalVolume, Integer nodeCount,
                          BigDecimal riskScore, List<String> involvedAccounts, Instant detectedAt) {
        this.ringIdentifier    = ringIdentifier;
        this.totalVolume       = totalVolume;
        this.nodeCount         = nodeCount;
        this.riskScore         = riskScore;
        this.involvedAccounts  = involvedAccounts;
        this.detectedAt        = detectedAt;
    }

    public UUID getId()                       { return id; }
    public String getRingIdentifier()         { return ringIdentifier; }
    public BigDecimal getTotalVolume()        { return totalVolume; }
    public Integer getNodeCount()             { return nodeCount; }
    public BigDecimal getRiskScore()          { return riskScore; }
    public List<String> getInvolvedAccounts() { return involvedAccounts; }
    public Instant getDetectedAt()            { return detectedAt; }
}
