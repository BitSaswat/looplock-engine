package com.example.looplock_engine.algorithm;

import java.util.*;

/**
 * Disjoint Set Union pre-filter for the detection pipeline.
 *
 * Groups vertices into weakly connected components, filtering out vertices
 * that cannot form a directed cycle (e.g., zero in-degree or out-degree).
 * Uses union-by-size and iterative path-halving for amortized O(α(N)) performance.
 *
 * Single-use per scan batch.
 */
final class DsuPreFilter {

    private final int[] parent;
    private final int[] size;
    private final int[] inDeg;
    private final int[] outDeg;

    DsuPreFilter(int maxVertices) {
        this.parent = new int[maxVertices];
        this.size   = new int[maxVertices];
        this.inDeg  = new int[maxVertices];
        this.outDeg = new int[maxVertices];
        for (int i = 0; i < maxVertices; i++) {
            parent[i] = i;
            size[i]   = 1;
        }
    }

    /**
     * Iterative path-halving find. Achieves the same O(α(N)) amortized bound
     * as full recursive path compression with zero stack-frame overhead.
     */
    private int find(int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private void union(int i, int j) {
        int ri = find(i), rj = find(j);
        if (ri == rj) return;
        if (size[ri] < size[rj]) { parent[ri] = rj; size[rj] += size[ri]; }
        else                      { parent[rj] = ri; size[ri] += size[rj]; }
    }

    /**
     * Partitions the edge list into weakly connected subgraphs, filtering out
     * vertices that cannot participate in a directed cycle.
     *
     * <p>Three linear passes over the edge list:
     * <ol>
     *   <li>Degree counting — O(E)</li>
     *   <li>Conditional DSU union — O(E · α(V))</li>
     *   <li>Component aggregation on surviving vertices — O(V_active)</li>
     * </ol>
     *
     * @return map of DSU root vertex → list of all vertices in that component;
     *         components of size 1 are excluded as they cannot form cycles
     */
    Map<Integer, List<Integer>> computeComponents(List<TemporalEdge> edges) {
        // Pass 1: degree census and candidate collection
        Set<Integer> candidates = new HashSet<>(edges.size() * 2);
        for (TemporalEdge e : edges) {
            outDeg[e.sourceVertex()]++;
            inDeg[e.destinationVertex()]++;
            candidates.add(e.sourceVertex());
            candidates.add(e.destinationVertex());
        }

        // Pass 2: union only cycle-capable edges
        for (TemporalEdge e : edges) {
            int u = e.sourceVertex(), v = e.destinationVertex();
            if (inDeg[u] > 0 && outDeg[u] > 0 && inDeg[v] > 0 && outDeg[v] > 0) {
                union(u, v);
            }
        }

        // Pass 3: group surviving vertices by component root
        Map<Integer, List<Integer>> components = new HashMap<>();
        for (int vertex : candidates) {
            if (inDeg[vertex] > 0 && outDeg[vertex] > 0) {
                components.computeIfAbsent(find(vertex), k -> new ArrayList<>()).add(vertex);
            }
        }

        components.entrySet().removeIf(entry -> entry.getValue().size() < 2);
        return components;
    }
}
