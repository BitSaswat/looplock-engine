package com.example.looplock_engine.algorithm;

import java.math.BigDecimal;

/**
 * Immutable in-memory representation of a directed financial transfer edge.
 *
 * <p>Vertices are pre-mapped to dense integer indices [0, maxVertices) by the
 * service layer before construction, keeping all algorithm internals primitive
 * and free of {@code String} account-ID allocations.
 *
 * <p>{@code timestampEpochMilli} is the sole temporal anchor used by the
 * sliding-window scan to scope detection to a bounded time horizon without
 * pulling full {@link com.example.looplock_engine.entity.TransactionRecord}
 * objects into algorithm memory.
 */
public record TemporalEdge(
        int sourceVertex,
        int destinationVertex,
        BigDecimal amount,
        long timestampEpochMilli
) {}
