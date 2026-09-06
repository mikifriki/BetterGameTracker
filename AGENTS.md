# BetterGameTracker — Codex Instructions

## Purpose

BetterGameTracker is a game-tracking application that can run:

1. Locally as a self-contained native application.
2. As a hosted web service.

The same backend codebase must support both modes.

---

## Source of Truth

When instructions conflict, use this order:

1. The current user task.
2. This `AGENTS.md`.
3. `PROJECT_SPEC.md`.
4. Existing BetterGameTracker source code.
5. Assumptions.

Do not invent product requirements when the specification is silent.

Ask or document uncertainty instead of silently introducing major architectural changes.

---

## Required Backend Stack

Use:

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA
- Hibernate
- Flyway

Local database:

- SQLite

Hosted database:

- PostgreSQL

Hosted authentication:

- Google OAuth / OpenID Connect
- Spring Security

Local authentication:

- None

---

## Deployment Requirements

### Local

The local application must:

- Run without requiring Java to be installed.
- Run without Docker.
- Run without a separately installed database.
- Use SQLite.
- Bind to loopback/localhost only by default.
- Serve the compiled web frontend.
- Be distributable as a GraalVM native executable.
- Require no external runtime from the end user.

### Hosted

The hosted application may:

- Run as a regular Spring Boot JAR.
- Run inside a container.
- Use PostgreSQL.
- Use Google OAuth/OIDC.

The hosted version does not need to use GraalVM Native Image.

---

## Architecture

Use this dependency direction:

```text
HTTP Controller
      |
      v
Application Service
      |
      v
Repository
      |
      v
JPA / Hibernate
      |
      v
Database
```

Controllers handle HTTP concerns.

Services contain application/business logic.

Repositories handle persistence.

Do not put database queries or filesystem persistence logic directly in controllers.

---

## Domain Model

The initial domain data must be based on the existing Go BetterGameTracker implementation.

### Game

- Represents one game in the library.
- Has zero or more `PlayEntry` objects.
- Has zero or one cover image.

### PlayEntry

- Represents one playthrough/play instance of a Game.
- Belongs to exactly one Game.
- Has zero or more Reviews.
- Has zero or more optional `PlayTimeEntry` objects.

### PlayTimeEntry

- Belongs to exactly one PlayEntry.
- Has a UUID id, required `LocalDate date`, positive integer `durationMinutes`, and optional notes.
- Multiple entries for the same PlayEntry and date are allowed.
- Currently limited to domain/JPA and schema support; repository, service, API, DTO, and frontend functionality are deferred.

### Review

- Belongs to exactly one PlayEntry.
- Does NOT belong directly to Game.
- A PlayEntry may have multiple Reviews.

Relationship:

```text
Game 1 ---- N PlayEntry

PlayEntry 1 ---- N Review

PlayEntry 1 ---- N PlayTimeEntry

Game 1 ---- 0..1 Cover
```

Use stable UUID identifiers for persistent domain objects unless a specific requirement says otherwise.

---

## Legacy Go Code

The existing Go implementation is the source of truth for the initial data fields.

Preserve the meaning of existing fields.

Expected conceptual mapping:

```text
Go                         Java

GameEntry              ->  Game
GameEntryDetails       ->  PlayEntry
PlayEntry              ->  PlayEntry-related API/domain model
Review                 ->  Review
Cover image handling   ->  Cover handling
```

Do not blindly port the old Go architecture.

In particular:

- Do not keep JSON files as the primary database.
- Do not reproduce persistence logic inside HTTP handlers.
- Do not use game titles as stable identifiers when UUIDs are available.
- Do not remove existing data fields without explicit approval.

---

## Database Rules

Flyway owns schema creation and migration.

Hibernate does not own schema evolution.

Use schema validation rather than automatic schema mutation.

Expected configuration principle:

```text
hibernate.ddl-auto = validate
```

Rules:

- Prefer Flyway migrations that work on both SQLite and PostgreSQL.
- Database-specific migrations are allowed when genuinely necessary.
- Never edit an already released migration.
- Add a new migration for every schema change.
- Preserve user data across application upgrades.

---

## Frontend Independence

The backend must not depend on a specific frontend framework.

Angular, React, Vue, or another frontend may be used independently.

The backend only needs the compiled static frontend output.

The backend should serve:

```text
/
```

for frontend assets.

The application API must live under:

```text
/api/v1/
```

The frontend communicates with the backend through the HTTP API.

Do not expose frontend-framework-specific concepts in backend services or domain logic.

---

## API Design

Prefer resource-oriented REST endpoints.

Examples:

```text
GET    /api/v1/games
POST   /api/v1/games

GET    /api/v1/games/{gameId}
PUT    /api/v1/games/{gameId}
DELETE /api/v1/games/{gameId}

GET    /api/v1/games/{gameId}/plays
POST   /api/v1/games/{gameId}/plays

GET    /api/v1/games/{gameId}/plays/{playId}
PUT    /api/v1/games/{gameId}/plays/{playId}
DELETE /api/v1/games/{gameId}/plays/{playId}

GET    /api/v1/games/{gameId}/plays/{playId}/reviews
POST   /api/v1/games/{gameId}/plays/{playId}/reviews
```

Avoid action-style routes such as:

```text
/getAllGames
/newGameEntry
/deleteGameEntry
```

unless required for compatibility.

---

## Current Scope

The backend should support:

- Game CRUD
- PlayEntry CRUD
- Review CRUD
- Cover image support
- SQLite local persistence
- PostgreSQL hosted persistence
- Flyway migrations
- Framework-independent REST API
- Serving compiled frontend assets
- Local native packaging
- Hosted JAR/container deployment

---

## Future Scope — Do Not Implement Unless Explicitly Requested

Do not proactively implement:

- Configurable remote BetterGameTracker endpoints
- Local request proxying to remote BetterGameTracker servers
- Local-to-hosted migration
- Hosted-to-local migration
- Portable backup format
- Bidirectional synchronization
- Conflict resolution
- Offline cloud synchronization
- Multiple libraries per user
- LAN access mode
- Automatic application updates

The architecture should avoid blocking these features, but they are not part of the initial implementation.

---

## Local vs Hosted Behavior

### Local profile

Use:

```text
SQLite
No authentication
127.0.0.1 binding
Local cover storage
GraalVM native distribution
```

### Hosted profile

Use:

```text
PostgreSQL
Google OAuth/OIDC
Server-configured network binding
Hosted cover storage
JAR or container distribution
```

Do not duplicate business logic between profiles.

---

## Coding Guidance

Prefer:

- Clear Java types.
- Small services with explicit responsibilities.
- Constructor injection.
- Immutable DTOs where practical.
- Explicit transaction boundaries.
- Domain-relevant names.
- Tests for business behavior.
- Integration tests for persistence when database behavior matters.

Avoid:

- Unnecessary abstraction layers.
- Generic base services/repositories without demonstrated need.
- Hidden schema changes.
- Business logic in controllers.
- Frontend-framework coupling.
- Premature implementation of future cloud/sync features.

---

## Before Making a Significant Change

Before changing architecture, persistence strategy, domain relationships, authentication behavior, or deployment behavior:

1. Check this file.
2. Check `PROJECT_SPEC.md`.
3. Inspect the current implementation.
4. Preserve existing data semantics.
5. Do not silently change agreed architecture.

If a requested implementation conflicts with these rules, surface the conflict explicitly.