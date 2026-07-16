package com.example.looplock_engine.algorithm;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Facade for the two-phase fraud ring detection pipeline.
 *
 * Uses DSU pre-filtering (O(E · α(V))) and Tarjan's SCC algorithm (O(V + E))
 * to extract directed cycles representing fraud rings.
 *
 * Stateless and thread-safe. Collaborators are instantiated fresh per detect call.
 */
@Component
public final class CycleDetectionEngine {

    /**
     * Detects all directed fraud rings in the supplied edge list.
     *
     * @param edges       directed financial transfer edges with pre-mapped integer vertex IDs
     * @param maxVertices upper bound on vertex ID (exclusive); must be &gt; all vertex IDs in {@code edges}
     * @return list of detected SCCs; each inner list contains the global vertex IDs of one fraud ring
     */
    @SuppressWarnings("unchecked")
    public List<List<Integer>> detect(List<TemporalEdge> edges, int maxVertices) {
        if (edges == null || edges.isEmpty()) return Collections.emptyList();

        // --- Phase 1: DSU coarse partitioning ---
        Map<Integer, List<Integer>> components =
                new DsuPreFilter(maxVertices).computeComponents(edges);

        if (components.isEmpty()) return Collections.emptyList();

        // Build a global source → destination lookup once, shared across all subgraphs
        Map<Integer, List<Integer>> globalAdj = new HashMap<>(edges.size() * 2);
        for (TemporalEdge e : edges) {
            globalAdj.computeIfAbsent(e.sourceVertex(), k -> new ArrayList<>())
                     .add(e.destinationVertex());
        }

        // --- Phase 2: Tarjan's on each isolated subgraph ---
        TarjanSccAnalyzer tarjan = new TarjanSccAnalyzer();
        List<List<Integer>> allRings = new ArrayList<>();

        for (List<Integer> component : components.values()) {
            int k = component.size();

            // Remap global vertex IDs to dense local indices [0, k)
            // to constrain Tarjan's array sizes to the subgraph, not maxVertices
            Map<Integer, Integer> globalToLocal = new HashMap<>(k * 2);
            int[] localToGlobal = new int[k];
            for (int i = 0; i < k; i++) {
                globalToLocal.put(component.get(i), i);
                localToGlobal[i] = component.get(i);
            }

            List<Integer>[] localAdj = new List[k];
            for (int i = 0; i < k; i++) localAdj[i] = new ArrayList<>();

            Set<Integer> componentSet = new HashSet<>(component);
            for (int globalSrc : component) {
                List<Integer> neighbours = globalAdj.get(globalSrc);
                if (neighbours == null) continue;
                int localSrc = globalToLocal.get(globalSrc);
                for (int globalDst : neighbours) {
                    if (componentSet.contains(globalDst)) {
                        localAdj[localSrc].add(globalToLocal.get(globalDst));
                    }
                }
            }

            // Translate SCCs back to global vertex IDs
            for (List<Integer> localRing : tarjan.findCycles(localAdj, k)) {
                List<Integer> globalRing = new ArrayList<>(localRing.size());
                for (int localId : localRing) globalRing.add(localToGlobal[localId]);
                allRings.add(globalRing);
            }
        }

        return allRings;
    }
}
