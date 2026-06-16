package com.example.looplock_engine.algorithm;

import java.util.ArrayList;
import java.util.List;

public class FraudDetector {

    private int[] parent;
    private int[] size;
    private int[] inDeg;
    private int[] outDeg;

    public FraudDetector(int maxNodes) {
        parent = new int[maxNodes];
        size = new int[maxNodes];
        inDeg = new int[maxNodes];
        outDeg = new int[maxNodes];

        for (int i = 0; i < maxNodes; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    private int find(int i) {
        if (parent[i] == i) return i;
        return parent[i] = find(parent[i]); // Path compression
    }

    private void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        if (rootI != rootJ) {
            // Union by size
            if (size[rootI] < size[rootJ]) {
                parent[rootI] = rootJ;
                size[rootJ] += size[rootI];
            } else {
                parent[rootJ] = rootI;
                size[rootI] += size[rootJ];
            }
        }
    }

    // Accepts a flat 2D array: [sender, receiver]
    public List<int[]> analyze(int[][] txns) {
        List<int[]> cycles = new ArrayList<>();

        // 1. Flow-state pruning pass
        for (int[] t : txns) {
            outDeg[t[0]]++;
            inDeg[t[1]]++;
        }

        // 2. DSU Cycle Detection pass
        for (int[] t : txns) {
            int u = t[0], v = t[1];

            // The Genius Fix: Only process nodes capable of being in a cycle
            if (inDeg[u] > 0 && outDeg[u] > 0 && inDeg[v] > 0 && outDeg[v] > 0) {
                if (find(u) == find(v)) {
                    // Cycle detected! Add the transaction that tripped the wire
                    cycles.add(new int[]{u, v});
                } else {
                    union(u, v);
                }
            }
        }
        return cycles;
    }
}