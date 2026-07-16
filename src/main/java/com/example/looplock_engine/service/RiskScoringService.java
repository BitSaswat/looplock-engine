package com.example.looplock_engine.service;

import com.example.looplock_engine.entity.TransactionRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Computes a [0.0, 100.0] risk score for a confirmed fraud ring.
 *
 * The score combines three signals:
 * 1. Velocity (40%): Temporal span between the earliest and latest transaction.
 * 2. Amount Asymmetry (35%): Ratio of minimum to maximum transaction amount.
 * 3. Cycle Length (25%): Penalty for length-2 rings (common refunds).
 */
@Service
public class RiskScoringService {

    private static final double VELOCITY_WEIGHT   = 0.40;
    private static final double ASYMMETRY_WEIGHT  = 0.35;
    private static final double LENGTH_WEIGHT     = 0.25;

    /** Transactions spanning less than this are treated as maximally suspicious. */
    private static final long   HIGH_VELOCITY_MS  = 60L * 60 * 1_000;          // 1 hour

    /** Transactions spanning more than this are treated as low-velocity baseline. */
    private static final long   LOW_VELOCITY_MS   = 30L * 24 * 60 * 60 * 1_000; // 30 days

    /**
     * @param ringTransactions the transactions forming one confirmed fraud ring,
     *                         in any order; must be non-empty
     * @return a risk score in [0.0, 100.0]; higher values indicate greater suspicion
     */
    public double score(List<TransactionRecord> ringTransactions) {
        double velocityScore   = computeVelocityScore(ringTransactions);
        double asymmetryScore  = computeAsymmetryScore(ringTransactions);
        double lengthScore     = computeLengthScore(ringTransactions.size());

        double composite = (velocityScore  * VELOCITY_WEIGHT)
                         + (asymmetryScore * ASYMMETRY_WEIGHT)
                         + (lengthScore    * LENGTH_WEIGHT);

        return Math.min(100.0, Math.max(0.0, composite * 100.0));
    }

    /**
     * High velocity (tight time window) → score near 1.0.
     * Decays logarithmically from 1h to 30 days, then floors at 0.
     */
    private double computeVelocityScore(List<TransactionRecord> txns) {
        var stats = txns.stream()
                .mapToLong(t -> t.getTimestamp().toEpochMilli())
                .summaryStatistics();

        long spanMs = stats.getMax() - stats.getMin();
        if (spanMs <= HIGH_VELOCITY_MS) return 1.0;
        if (spanMs >= LOW_VELOCITY_MS)  return 0.0;

        double logSpan  = Math.log((double) spanMs / HIGH_VELOCITY_MS);
        double logRange = Math.log((double) LOW_VELOCITY_MS / HIGH_VELOCITY_MS);
        return 1.0 - (logSpan / logRange);
    }

    /**
     * Low min/max ratio (layering pattern) → score near 1.0.
     * A perfectly symmetric ring (ratio = 1.0) scores 0.
     */
    private double computeAsymmetryScore(List<TransactionRecord> txns) {
        BigDecimal min = txns.stream()
                .map(TransactionRecord::getAmount)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);
        BigDecimal max = txns.stream()
                .map(TransactionRecord::getAmount)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);

        if (max.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        double ratio = min.doubleValue() / max.doubleValue();
        return 1.0 - ratio; // ratio=1 (symmetric) → 0.0; ratio→0 (heavy layering) → 1.0
    }

    /**
     * Length-2 rings receive a 0.5 dampener (common refund pattern).
     * Length ≥ 3 receives no dampener — deliberate multi-hop obfuscation.
     */
    private double computeLengthScore(int length) {
        return (length == 2) ? 0.5 : 1.0;
    }
}
