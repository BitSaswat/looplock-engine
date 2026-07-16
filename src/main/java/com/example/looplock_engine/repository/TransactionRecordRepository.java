package com.example.looplock_engine.repository;

import com.example.looplock_engine.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistence contract for {@link TransactionRecord}.
 *
 * <p>The {@code findByProcessedFalse} and time-windowed variant are the primary
 * ingestion queries for the DSU scan pipeline. They define the batch boundary:
 * only unprocessed edges within the requested time window are loaded into
 * the in-memory graph, bounding heap usage regardless of total table size.
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, UUID> {

    /** Returns all edges not yet consumed by a DSU scan cycle. */
    List<TransactionRecord> findByProcessedFalse();

    /**
     * Returns unprocessed edges within a closed time window.
     * Used by the sliding-window scan to scope detection to a bounded period
     * (e.g., last 24 h) without loading the full unprocessed backlog.
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.processed = false " +
           "AND t.timestamp >= :windowStart AND t.timestamp <= :windowEnd")
    List<TransactionRecord> findUnprocessedInWindow(Instant windowStart, Instant windowEnd);
}
