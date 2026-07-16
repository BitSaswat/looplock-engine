package com.example.looplock_engine.service;

import com.example.looplock_engine.entity.TransactionRecord;

import java.util.List;

/**
 * Immutable carrier for a single confirmed fraud ring detection result.
 */
public record FraudRingResult(
        List<String> involvedAccounts,
        List<TransactionRecord> evidenceTransactions,
        double riskScore
) {}
