package com.example.looplock_engine.service;

import com.example.looplock_engine.algorithm.TemporalEdge;
import com.example.looplock_engine.entity.TransactionRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O(1) bi-directional mapping between String account IDs and integer vertex indices.
 *
 * Instantiated once per scan to ensure internal state remains request-scoped.
 */
final class AccountVertexMapper {

    private final Map<String, Integer> accountToVertex = new HashMap<>();
    private final List<String> vertexToAccount = new ArrayList<>();

    /**
     * Converts a list of {@link TransactionRecord} entities into
     * {@link TemporalEdge} primitives, assigning integer vertex IDs
     * to each previously unseen account ID.
     *
     * @param records unprocessed transaction edges from the repository
     * @return list of edges ready for {@code CycleDetectionEngine.detect()}
     */
    List<TemporalEdge> buildEdges(List<TransactionRecord> records) {
        List<TemporalEdge> edges = new ArrayList<>(records.size());
        for (TransactionRecord r : records) {
            int src  = intern(r.getSourceAccountId());
            int dst  = intern(r.getDestinationAccountId());
            edges.add(new TemporalEdge(src, dst, r.getAmount(),
                                       r.getTimestamp().toEpochMilli()));
        }
        return edges;
    }

    /**
     * Translates a list of integer vertex IDs (as returned by the engine)
     * back into their original String account IDs.
     *
     * @param vertices global vertex indices produced by the detection engine
     * @return the corresponding account ID strings in the same order
     */
    List<String> resolveAccounts(List<Integer> vertices) {
        List<String> accounts = new ArrayList<>(vertices.size());
        for (int v : vertices) accounts.add(vertexToAccount.get(v));
        return accounts;
    }

    /**
     * Returns the total number of distinct account IDs registered so far.
     * This value is the {@code maxVertices} argument to pass to
     * {@code CycleDetectionEngine.detect()}.
     */
    int vertexCount() {
        return vertexToAccount.size();
    }

    private int intern(String accountId) {
        return accountToVertex.computeIfAbsent(accountId, id -> {
            int index = vertexToAccount.size();
            vertexToAccount.add(id);
            return index;
        });
    }
}
