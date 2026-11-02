# Kazi

## What this is
An AI job aggregator backend that will normalize and deduplicate jobs from public APIs, then expose search through REST and MCP.

## What it is NOT
No frontend or user accounts. Other source adapters, fuzzy deduplication, semantic search, MCP, Redis, Kafka, and production deployment are not implemented yet.

## Package structure
`com.kazi.kazi`
- `health/` — health endpoint
- `ingestion/` — ingestion orchestration and scheduling configuration
- `ingestion/source/` — source contract, raw job model, and source errors
- `ingestion/normalization/` — normalized job model and normalization rules
- `ingestion/run/` — persistent source-run audits
- `ingestion/adzuna/` — Adzuna configuration, declarative `@HttpExchange` client, source adapter, response mapping, and scheduler
- `job/` — canonical jobs, source provenance, deterministic deduplication, and merge rules
- `shared/config/` — cross-cutting configuration
- `KaziApplication.java` — application entry point

Add future domain code in feature packages such as `search/` and `mcp/`.

## Current foundation
- Java 25, Spring Boot 4.1.2, Maven
- PostgreSQL 18 with pgvector and Flyway
- `GET /api/health`
- Job source contract and raw-payload preservation
- Company, title, location, salary, and employment-type normalization
- Adzuna pagination and date filtering with timeouts, Spring Framework `@Retryable`, and Resilience4j circuit breaking
- Opt-in scheduling, per-source failure isolation, and persistent `SourceRun` audits
- Canonical job persistence with deterministic deduplication, merging, and source provenance
- Docker Compose and GitHub Actions CI
- Integration tests use real PostgreSQL through Testcontainers; mapping and normalization tests are plain JUnit

## How to run
```bash
docker compose up --build
./mvnw test
./mvnw spring-boot:test-run
```

`test-run` starts the app with a temporary PostgreSQL container. Compose starts the app and a persistent local database.

## Working agreements
Keep changes small and feature-focused. Flyway owns the schema; Hibernate validates mappings but never creates tables. Use the existing `BaseIntegrationTest` for tests that need PostgreSQL. Create a commit for each completed and verified feature.
