# LoopLock: Autonomous Fraud Ring Detection Engine

A high-performance Early Warning System (EWS) that utilizes RAM-based Disjoint Set Union (DSU) and flow-state pruning to detect cyclical layering in financial ledgers in O(N) time.

## Architecture & Efficiency
* **Engine:** Pure primitive Java array DSU, abandoning OOP overhead to keep memory strictly O(V).
* **Speed:** Leverages path compression and union-by-size (Inverse Ackermann function) for near-instant cycle detection.
* **Pruning:** Implements a linear "Flow-State Filter" to eliminate dead-end nodes before algorithmic processing, drastically reducing false positives without sacrificing performance.
* **Stack:** Java 17, Spring Boot, PostgreSQL, Spring Data JPA.