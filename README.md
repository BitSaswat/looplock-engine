# LoopLock

An Early Warning System (EWS) for detecting fraud rings and cyclical transaction layering in financial ledgers.

Graph traversals in relational databases are slow and recursive. LoopLock bypasses this by pulling unmapped edges into memory and processing them against primitive arrays. It finds multi-hop cycles in linear time.

## Core Problem
Fraudsters use "layering" to hide the source of funds. Money moves in a ring (e.g., Account A → B → C → D → A). Detecting these directed cycles at scale typically breaks relational databases.

## Pipeline Architecture
LoopLock relies on a two-phase memory pipeline rather than SQL recursive CTEs:

1. **DSU Pre-Filter (Amortized `O(α(N))`):** A Disjoint Set Union pass groups edges into weakly connected components. Any node with a zero in-degree or out-degree is pruned immediately, as it cannot be part of a cycle. This cuts down the search space by roughly 30%.
2. **Tarjan's SCC (`O(V + E)`):** An iterative implementation of Tarjan's Strongly Connected Components algorithm runs on the remaining subgraphs to pull out the exact directed cycles. We use a heap-allocated call stack instead of the JVM call stack to prevent `StackOverflowError` on deep chains.

## Risk Scoring
Rings are scored from 0.0 to 100.0 based on three signals:
- **Velocity (40%):** Rings completing within an hour get a high score. Rings stretched over weeks get penalized.
- **Asymmetry (35%):** High scores for layering patterns (e.g., one massive deposit splitting into tiny transfers).
- **Length (25%):** Length-2 rings (A→B→A) are often refunds and get a 50% penalty. Multi-hop rings (3+ nodes) get full weight.

## Stack
- Java 21 (Records, primitive arrays)
- Spring Boot 3.3
- PostgreSQL + Flyway
- OpenAPI / Swagger UI

## Running Locally

**1. Boot the DB**
```bash
docker run --name looplock-db -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=looplock -d postgres
```

**2. Start the App**
```bash
./mvnw spring-boot:run
```
Flyway handles the schema migration on boot.

**3. API Docs**
Go to `http://localhost:8080/swagger-ui.html`
- `POST /api/v1/fraud/scan`: Scan all unprocessed transactions.
- `POST /api/v1/fraud/scan/windowed`: Scan a specific time window.
- `GET /api/v1/fraud/alerts`: Paginated list of scored fraud rings.