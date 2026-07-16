package com.example.looplock_engine.algorithm;

import java.util.*;

/**
 * Tarjan's Strongly Connected Components algorithm.
 *
 * Extracts directed cycles from a given subgraph. Returns only SCCs of size ≥ 2.
 * Uses a fully iterative DFS with an explicit call stack to prevent 
 * StackOverflowError on deep subgraphs.
 *
 * Stateful and not thread-safe. Can be reused sequentially across subgraphs.
 */
final class TarjanSccAnalyzer {

    private int timer;
    private int[] ids;
    private int[] low;
    private boolean[] onStack;
    private Deque<Integer> sccStack;
    private Deque<int[]> callStack;
    private List<List<Integer>> result;
    private List<Integer>[] adjList;

    /**
     * Executes Tarjan's SCC algorithm on the given subgraph.
     *
     * @param adjList local adjacency list; vertices are dense indices [0, n)
     * @param n       number of vertices in the subgraph
     * @return list of SCCs where each SCC contains ≥ 2 vertices
     */
    @SuppressWarnings("unchecked")
    List<List<Integer>> findCycles(List<Integer>[] adjList, int n) {
        this.adjList   = adjList;
        this.timer     = 0;
        this.ids       = new int[n];
        this.low       = new int[n];
        this.onStack   = new boolean[n];
        this.sccStack  = new ArrayDeque<>();
        this.callStack = new ArrayDeque<>();
        this.result    = new ArrayList<>();

        Arrays.fill(ids, -1);

        for (int i = 0; i < n; i++) {
            if (ids[i] == -1) iterativeDfs(i);
        }
        return result;
    }

    private void iterativeDfs(int start) {
        ids[start] = low[start] = timer++;
        sccStack.push(start);
        onStack[start] = true;
        callStack.push(new int[]{start, 0});

        while (!callStack.isEmpty()) {
            int[] frame      = callStack.peek();
            int   v          = frame[0];
            int   idx        = frame[1];
            List<Integer> nb = adjList[v];

            if (idx < nb.size()) {
                int w = nb.get(idx);
                frame[1]++;

                if (ids[w] == -1) {
                    // Tree edge: initialise and descend
                    ids[w] = low[w] = timer++;
                    sccStack.push(w);
                    onStack[w] = true;
                    callStack.push(new int[]{w, 0});
                } else if (onStack[w]) {
                    // Back edge: tighten low-link without descending
                    low[v] = Math.min(low[v], ids[w]);
                }
            } else {
                // All neighbours exhausted: pop and propagate low-link to parent
                callStack.pop();
                if (!callStack.isEmpty()) {
                    int parent = callStack.peek()[0];
                    low[parent] = Math.min(low[parent], low[v]);
                }
                // v is the root of an SCC if its low-link equals its discovery id
                if (low[v] == ids[v]) {
                    List<Integer> scc = new ArrayList<>();
                    int w;
                    do {
                        w = sccStack.pop();
                        onStack[w] = false;
                        scc.add(w);
                    } while (w != v);
                    if (scc.size() >= 2) result.add(scc);
                }
            }
        }
    }
}
