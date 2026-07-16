package com.example.looplock_engine.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single directed financial transfer between two accounts.
 *
 * The processed flag acts as the ingestion cursor for the engine,
 * ensuring edges are only analyzed once.
 */
@Entity
@Table(
    name = "transaction_records",
    indexes = {
        @Index(name = "idx_txn_source",      columnList = "source_account_id"),
        @Index(name = "idx_txn_destination",  columnList = "destination_account_id"),
        @Index(name = "idx_txn_processed",    columnList = "processed")
    }
)
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "source_account_id", nullable = false)
    private String sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private String destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant timestamp;

    /** Marks whether this edge has been ingested into a DSU scan cycle. */
    @Column(nullable = false)
    private Boolean processed = false;

    protected TransactionRecord() {}

    public TransactionRecord(String sourceAccountId, String destinationAccountId,
                             BigDecimal amount, Instant timestamp) {
        this.sourceAccountId      = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount               = amount;
        this.timestamp            = timestamp;
    }

    public UUID getId()                    { return id; }
    public String getSourceAccountId()     { return sourceAccountId; }
    public String getDestinationAccountId(){ return destinationAccountId; }
    public BigDecimal getAmount()          { return amount; }
    public Instant getTimestamp()          { return timestamp; }
    public Boolean getProcessed()          { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }
}
