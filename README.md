# LoopLock

LoopLock is an Early Warning System (EWS) that detects fraud rings and circular transactions in financial ledgers.

## How it works

The engine runs a two-phase detection pipeline entirely in memory:
1. **DSU Pre-Filter:** A Disjoint Set Union pass groups transaction edges into weakly connected components, filtering out any accounts that cannot form a cycle (e.g., accounts with zero in-degree or out-degree).
2. **Tarjan's SCC:** An iterative implementation of Tarjan's Strongly Connected Components algorithm runs on the surviving subgraphs to extract exact directed cycles (fraud rings).

Confirmed rings are scored based on transaction velocity, amount asymmetry (layering), and cycle length, then persisted to the database.

## Tech Stack
* Java 21
* Spring Boot 3.3 (Web, Data JPA)
* PostgreSQL
* Flyway (Database migrations)
* OpenAPI / Swagger UI