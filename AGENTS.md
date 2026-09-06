# BetterGameTracker — Codex Instructions

## Priorities

When instructions conflict, use this order:

1. Current user task
2. `AGENTS.md`
3. `PROJECT_SPEC.md`
4. Existing code
5. Assumptions

Do not invent requirements. Do not make unrelated architectural or product changes.

## Implementation Style

Keep the code simple, clean, minimal, and efficient.

Prefer the smallest correct implementation that fits the existing architecture.

- Write straightforward, idiomatic Java.
- Prefer simple control flow that is easy to follow locally.
- Avoid unnecessary abstraction, indirection, and boilerplate.
- Do not add helper methods unless they improve readability or are reused.
- Avoid named local variables used only once when the expression is clearer inline.
- Do not introduce interfaces, wrappers, utilities, builders, generic base classes, or other abstractions without a concrete need.
- Prefer existing project patterns over introducing new ones.
- Do not add defensive complexity for hypothetical requirements.
- Do not refactor unrelated code while implementing a task.
- Do not optimize speculatively, but avoid obviously wasteful work.
- Keep relationship management explicit. Avoid methods that call back into each other or require tracing several methods to understand one operation.
- Remove code made obsolete by the requested change rather than keeping unnecessary compatibility helpers, unless compatibility is required.

When multiple solutions are valid, prefer the one with fewer moving parts and less code while remaining clear and correct.

## Backend

Use:

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA / Hibernate
- Flyway
- SQLite for local deployments
- PostgreSQL for hosted deployments

Use constructor injection.

Controllers handle HTTP concerns, services handle application/business logic, and repositories handle persistence.

Do not put business logic or persistence logic in controllers.

Flyway owns schema evolution. Hibernate validates the schema; it does not mutate it.

Never modify an already released Flyway migration. Preserve existing user data.

## Domain

Persistent domain objects use UUID identifiers unless explicitly specified otherwise.

Relationships:

```text
Game 1 ---- N PlayEntry
PlayEntry 1 ---- N Review
PlayEntry 1 ---- N PlayTimeEntry
Game 1 ---- 0..1 Cover
```

A `PlayEntry` belongs to exactly one `Game`.

A `Review` belongs to exactly one `PlayEntry`, not directly to a `Game`.

A `PlayTimeEntry` belongs to exactly one `PlayEntry`.

`PlayTimeEntry` contains:

- UUID id
- required `LocalDate date`
- positive integer `durationMinutes`
- optional notes

Multiple `PlayTimeEntry` records for the same play entry and date are valid.

Do not remove or reinterpret existing domain data without explicit approval.

## Local and Hosted Modes

The same backend codebase supports both modes. Do not duplicate business logic between them.

Local:

- SQLite
- no authentication
- loopback binding by default
- local cover storage
- no external Java, Docker, or database requirement for the end user
- GraalVM native executable

Hosted:

- PostgreSQL
- Google OAuth/OIDC with Spring Security
- server-configured network binding
- JAR or container deployment

## API and Frontend

The API lives under `/api/v1/`.

Use resource-oriented REST APIs rather than action-style routes.

The backend must remain independent of the frontend framework and may serve compiled frontend assets.

Do not expose frontend-framework-specific concepts in backend domain or service code.

## Scope Discipline

Implement only what the current task requires.

Do not proactively implement future features such as synchronization, local/hosted migration, remote endpoints, multiple libraries, LAN mode, backup formats, conflict resolution, or automatic updates.

Architecture should not unnecessarily block likely future work, but do not add abstractions solely for hypothetical future requirements.

## Before Significant Changes

For changes to architecture, persistence strategy, domain relationships, authentication, or deployment:

1. Check `PROJECT_SPEC.md`.
2. Inspect the existing implementation.
3. Preserve existing data semantics and agreed architecture.
4. Surface conflicts instead of silently changing architecture.

For ordinary implementation tasks, do not perform unnecessary architecture analysis when the existing design and task are clear.

## Validation

Run the relevant existing tests after changes.

Add or update tests when behavior changes or when the task explicitly requires them.

Keep tests focused on observable behavior. Do not add tests solely to exercise implementation details.