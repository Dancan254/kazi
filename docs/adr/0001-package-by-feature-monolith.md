# ADR-0001 — Package-by-feature monolith over microservices

- Status: Accepted
- Date: 2026-11-02

## Context
Small, early-stage project owned by one team. Cohesion and speed of iteration matter more than independent deployability right now — there's no operational need to split services yet.

## Decision
Ship a single Spring Boot deployable, packaged by feature plus a `shared` package for cross-cutting code. Keep feature boundaries clean so any one can be extracted into its own service later.

## Consequences
- + Simple to run, test, and reason about; no distributed-systems tax up front.
- + Clean seams make a future service extraction mechanical rather than surgical.
- − Boundary discipline is manual today. Revisit if the app grows enough to need enforced module boundaries.
