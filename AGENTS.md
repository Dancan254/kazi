# Kazi

## What this is
An AI job aggregator backend that will normalize and deduplicate jobs from public APIs, then expose search through REST and MCP.

## What it is NOT
No frontend or user accounts. Source adapters beyond Adzuna, Greenhouse, Lever, and Ashby, fuzzy deduplication, semantic search, Redis, and production deployment are not implemented yet. The MCP endpoint is unauthenticated and intended for local development.

## Package structure
`com.kazi.kazi`
- `health/` — health endpoint
- `ingestion/` — ingestion orchestration and scheduling configuration
- `ingestion/source/` — source contract, raw job model, and source errors
- `ingestion/normalization/` — normalized job model and normalization rules
- `ingestion/run/` — persistent source-run audits
- `ingestion/adzuna/` — Adzuna configuration, declarative `@HttpExchange` client, source adapter, response mapping, and scheduler
- `ingestion/greenhouse/` — configurable Greenhouse boards, declarative client, response mapping, per-board provenance, and scheduler
- `ingestion/lever/` — configurable Lever sites, paginated declarative client, response mapping, per-site provenance, and scheduler
- `ingestion/ashby/` — configurable Ashby organizations, declarative client, response mapping, per-board provenance, and scheduler
- `job/` — canonical jobs, source provenance, deterministic deduplication, merge rules, and Kafka job events
- `search/` — shared job-search use case, paginated REST endpoint, and response DTOs
- `mcp/` — MCP tool adapters over domain use cases
- `shared/config/` — cross-cutting configuration
- `KaziApplication.java` — application entry point

Add future domain code in feature packages.

## Current foundation
- Java 25, Spring Boot 4.1.2, Maven
- Spring AI 2.0.2 with streamable HTTP MCP at `/mcp`
- PostgreSQL 18 with pgvector and Flyway
- Apache Kafka with JSON job events
- `GET /api/health`
- `GET /api/jobs` with keyword, location, employment-type, and pagination filters
- `GET /api/jobs/{id}` with full job details and original source/application links
- `search_active_jobs` and `get_job_details` MCP tools with validated arguments
- Job source contract and raw-payload preservation
- Company, title, location, salary, and employment-type normalization
- Adzuna pagination and date filtering with timeouts, Spring Framework `@Retryable`, and Resilience4j circuit breaking
- Greenhouse multi-board ingestion with configured company names, `updated_at` filtering, JSON-safe raw payloads, and `greenhouse:<token>` provenance
- Lever multi-site ingestion with pagination, `createdAt` filtering, salary and remote-location mapping, original apply links, and `lever:<site>` provenance
- Ashby multi-organization ingestion with `publishedAt` filtering, compensation and workplace mapping, plain-text descriptions, original apply links, and `ashby:<board>` provenance
- Opt-in scheduling, per-source failure isolation, and persistent `SourceRun` audits
- Canonical job persistence with deterministic deduplication, merging, and source provenance
- Full-snapshot reconciliation for Greenhouse, Lever, and Ashby with per-source activity, multi-source protection, failed-fetch safety, and reactivation
- Docker Compose and GitHub Actions CI
- Integration tests use real PostgreSQL, Kafka, and an MCP client; mapping and normalization tests are plain JUnit

## How to run
```bash
docker compose up --build
./mvnw test
./mvnw spring-boot:test-run
```

`test-run` starts the app with temporary PostgreSQL and Kafka containers. Compose starts the app with a persistent local database and Kafka.

Ingestion publishes `job.events.indexed` for new jobs and `job.events.merged` for matches. JSON events contain `schemaVersion`, `jobId`, `sourceRunId`, and `occurredAt`.

## Working agreements
Keep changes small and feature-focused. Flyway owns the schema; Hibernate validates mappings but never creates tables. Use the existing `BaseIntegrationTest` for tests that need PostgreSQL or Kafka. Create a commit for each completed and verified feature.
