package com.example.looplock_engine.repository;

import com.example.looplock_engine.entity.FraudRingAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistence contract for {@link FraudRingAlert}.
 *
 * <p>Provides ordered retrieval for the analyst dashboard and deduplication
 * checks by {@code ringIdentifier} — ensuring the same fraud ring cannot be
 * re-alerted within the same detection cycle.
 */
@Repository
public interface FraudRingAlertRepository extends JpaRepository<FraudRingAlert, UUID> {

    /** Returns all alerts for a given DSU root node, supporting deduplication. */
    List<FraudRingAlert> findByRingIdentifier(String ringIdentifier);

    /** Returns all alerts detected after a given instant, ordered newest-first. */
    List<FraudRingAlert> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since);
}
