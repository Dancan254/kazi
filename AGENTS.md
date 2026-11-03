# Kazi

## What this is
An AI job aggregator backend that will normalize and deduplicate jobs from public APIs, then expose search through REST and MCP.

## What it is NOT
No frontend or user accounts. Source adapters beyond Adzuna, Greenhouse, Lever, Ashby, Workday, SmartRecruiters, Workable, Recruitee, Personio, and BambooHR, fuzzy deduplication, semantic search, Redis, and production deployment are not implemented yet. The MCP endpoint is unauthenticated and intended for local development.

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
- `ingestion/workday/` — configurable Workday career sites, paginated declarative client, detail mapping, per-site provenance, and scheduler
- `ingestion/smartrecruiters/` — configurable SmartRecruiters companies, paginated declarative client, public detail mapping, per-company provenance, and scheduler
- `ingestion/workable/` — configurable Workable accounts, declarative client, response mapping, per-account provenance, and scheduler
- `ingestion/recruitee/` — configurable Recruitee companies, declarative client, response mapping, per-company provenance, and scheduler
- `ingestion/personio/` — configurable Personio companies, secure XML parsing, response mapping, per-company provenance, and scheduler
- `ingestion/bamboohr/` — configurable BambooHR companies, declarative list/detail client, response mapping, per-company provenance, and scheduler
- `ingestion/discovery/` — public career-page link discovery, ATS URL routing, private-address protection, and scheduled ingestion
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
- Workday multi-site ingestion with 20-item pagination, public detail mapping, original job links, and `workday:<tenant>/<site>` provenance
- SmartRecruiters multi-company ingestion with offset pagination, public detail mapping, original apply links, and `smartrecruiters:<identifier>` provenance
- Workable multi-account ingestion with full descriptions, publication dates, remote and multi-location mapping, original apply links, and `workable:<account>` provenance
- Recruitee multi-company ingestion with salary, publication-time, remote and office-location mapping, original career links, and `recruitee:<site>` provenance
- Personio multi-company ingestion with secure XML parsing, descriptions, schedules, office mapping, original job links, and `personio:<site>` provenance
- BambooHR multi-company ingestion with public detail mapping, compensation, remote locations, original job links, and `bamboohr:<site>` provenance
- Career-page ATS link discovery and routing for all nine public platforms with redirect and private-address safeguards
- Opt-in scheduling, per-source failure isolation, and persistent `SourceRun` audits
- Canonical job persistence with deterministic deduplication, merging, and distinct per-posting source provenance
- Full-snapshot reconciliation for Greenhouse, Lever, Ashby, Workday, SmartRecruiters, Workable, Recruitee, Personio, and BambooHR with per-source activity, multi-source protection, failed-fetch safety, and reactivation
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
