# BetterGameTracker — Project Specification

## 1. Purpose

BetterGameTracker is a self-hostable game tracking application.

The primary goal is to allow a user to maintain a personal video game library containing:

- Games
- Individual play entries/playthroughs for games
- Reviews associated with individual play entries
- Game cover images

BetterGameTracker must support two primary deployment models:

1. A completely local application for ordinary Windows and macOS users.
2. A hosted web application operated on a server.

The local version must not require the user to install:

- Java
- Docker
- PostgreSQL
- Node.js
- A database server
- Any other runtime or supporting software

The user should be able to launch BetterGameTracker as a normal native application.

The application itself is fundamentally a web application. The native local executable starts the BetterGameTracker backend and serves the web frontend to the user's normal browser.

---

# 2. Core Design Principles

## 2.1 One Backend

There should not be separate "local BetterGameTracker" and "server BetterGameTracker" implementations.

The same Java backend codebase must support both deployment models.

Differences between local and hosted installations should primarily be configuration and infrastructure differences.

Conceptually:

```text
                   BetterGameTracker
                          |
                   Spring Boot
                          |
             +------------+------------+
             |                         |
          Local                       Hosted
             |                         |
          SQLite                   PostgreSQL
             |                         |
      Native executable          JAR / Container
```

Business logic, domain models, API contracts, and application services should be shared.

---

# 3. Technology Stack

## Backend

Language:

```text
Java
```

Framework:

```text
Spring Boot
Spring MVC
```

Persistence:

```text
Spring Data JPA
Hibernate
```

Database migrations:

```text
Flyway
```

Authentication for hosted deployments:

```text
Spring Security
Google OAuth / OpenID Connect
```

Local database:

```text
SQLite
```

Hosted database:

```text
PostgreSQL
```

Local executable:

```text
GraalVM Native Image
```

Hosted deployment:

```text
Regular Spring Boot JAR
or
Container image
```

The hosted application does not need to use GraalVM Native Image.

---

# 4. Frontend Architecture

The BetterGameTracker backend must NOT depend on a specific frontend framework.

The frontend is a separate web application that consumes the BetterGameTracker REST API.

The frontend may be implemented using:

- Angular
- React
- Vue
- Another web framework
- Plain web technologies

The choice of frontend framework must not affect the backend architecture.

The contract between frontend and backend is the BetterGameTracker HTTP API.

Conceptually:

```text
Any Web Frontend
       |
       | HTTP / REST
       v
BetterGameTracker API
       |
       v
Application Services
       |
       v
Persistence
```

---

# 5. Serving the Frontend

Although the frontend is architecturally independent, the BetterGameTracker backend should be capable of serving its compiled static assets.

A frontend build is expected to eventually produce files such as:

```text
index.html
*.js
*.css
assets/
```

These files can be packaged into the BetterGameTracker backend.

Spring Boot then serves:

```text
/
```

while application API endpoints are served under:

```text
/api/v1/
```

For example:

```text
GET /
GET /index.html
GET /assets/...

GET /api/v1/games
POST /api/v1/games
```

Serving the frontend from Spring Boot is a deployment mechanism only.

Backend code must not contain assumptions about Angular, React, or another particular frontend framework.

---

# 6. Repository Structure

The frontend and backend should remain separate projects/modules.

Recommended high-level structure:

```text
BetterGameTracker/
|
+-- backend/
|   |
|   +-- Java / Spring Boot application
|
+-- frontend/
|   |
|   +-- Web application
|
+-- README.md
+-- PROJECT_SPEC.md
```

The backend build may consume the compiled frontend output when producing a release.

It should not require frontend source code at runtime.

---

# 7. Local Application

The local BetterGameTracker application is intended for ordinary users who do not want to host a server.

The expected user experience is:

```text
User launches BetterGameTracker
            |
            v
Native backend starts
            |
            v
SQLite database opened
            |
            v
Web server starts on localhost
            |
            v
Default browser opens
            |
            v
BetterGameTracker frontend loads
```

The user interacts with BetterGameTracker through their normal browser.

There is no dedicated native desktop GUI.

---

# 8. Local Distribution

The Java application should be compiled using GraalVM Native Image for local releases.

Example release targets:

```text
Windows:
BetterGameTracker.exe

macOS:
BetterGameTracker native application/binary
```

The end user must NOT need a Java installation.

The executable contains everything required to start BetterGameTracker.

Platform-specific native binaries may be built using CI, such as GitHub Actions.

---

# 9. Local Networking

The local application should bind to the loopback interface by default:

```text
127.0.0.1
```

It should NOT bind to:

```text
0.0.0.0
```

by default.

Therefore, an ordinary local installation should not automatically expose the BetterGameTracker API to other devices on the LAN.

Example:

```text
http://127.0.0.1:<port>
```

The exact port strategy has not yet been finalized.

---

# 10. Local Authentication

Local BetterGameTracker installations have:

```text
NO AUTHENTICATION
```

The local installation is considered a single-user application.

There should be no:

- Local account
- Local login
- Local password
- Artificial "default user"

The localhost-only network boundary is the initial protection for the local instance.

---

# 11. Hosted Application

The same BetterGameTracker backend can also run as a hosted service.

A hosted deployment can run as:

```text
java -jar better-game-tracker.jar
```

or as a container.

Unlike the local version, the hosted server may depend on infrastructure controlled by the server operator.

The initial planned hosted database is:

```text
PostgreSQL
```

---

# 12. Hosted Authentication

Hosted BetterGameTracker uses authentication.

The currently selected authentication provider is:

```text
Google OAuth / OpenID Connect
```

Spring Security should be used to integrate Google authentication.

BetterGameTracker should still maintain its own internal user identity.

Google authenticates the person; BetterGameTracker owns the application user and the user's data.

Conceptually:

```text
Google
   |
   | authentication
   v
BetterGameTracker User
   |
   v
User Library
```

The exact hosted user schema will be designed when hosted authentication is implemented.

Local installations must not require Google authentication.

---

# 13. Local Database

Local installations use:

```text
SQLite
```

SQLite is embedded into the application.

The user must not need to install or configure a database server.

Conceptually:

```text
BetterGameTracker.exe
        |
        v
bettergametracker.db
```

The SQLite database should live in the operating system's normal application-data location rather than beside the executable.

Example conceptual locations:

Windows:

```text
%LOCALAPPDATA%/BetterGameTracker/
```

macOS:

```text
~/Library/Application Support/BetterGameTracker/
```

The exact paths can be finalized during implementation.

---

# 14. Hosted Database

Hosted BetterGameTracker uses:

```text
PostgreSQL
```

The domain model and application services should not contain SQLite-specific or PostgreSQL-specific behavior.

Database differences belong in infrastructure/configuration where possible.

---

# 15. Database Access

BetterGameTracker uses:

```text
Spring Data JPA
Hibernate
```

JPA entities represent the persisted BetterGameTracker domain.

Repositories should use Spring Data JPA where appropriate.

Business logic must not be placed in repository implementations.

Typical flow:

```text
Controller
    |
    v
Service
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

---

# 16. Database Schema Management

BetterGameTracker uses:

```text
Flyway
```

Flyway owns database schema creation and migration.

Hibernate must NOT be responsible for automatically modifying production/user schemas.

The intended Hibernate configuration is equivalent to:

```text
ddl-auto = validate
```

Conceptually:

```text
Application startup
        |
        v
Flyway
        |
        | migrate database
        v
Current schema
        |
        v
Hibernate validates mappings
        |
        v
Application starts
```

This is especially important for local installations because users may keep the same SQLite database across many BetterGameTracker versions.

---

# 17. Migration Portability

SQLite and PostgreSQL Flyway migrations should be shared as much as reasonably possible.

Prefer portable SQL when doing so does not significantly compromise the schema.

Conceptually:

```text
             Flyway migrations
                    |
             +------+------+
             |             |
             v             v
          SQLite       PostgreSQL
```

Database-specific migrations may be introduced when genuinely required.

Do not distort the database design merely to avoid every possible database-specific migration.

Once a migration has shipped to users, it must not be modified.

For example:

```text
V1__initial_schema.sql
V2__add_play_entries.sql
V3__add_reviews.sql
V4__add_cover_metadata.sql
```

Future schema changes require new migrations.

---

# 18. Existing Go Backend

The existing Go BetterGameTracker backend is the source of truth for the initial application data requirements.

The Java rewrite should NOT arbitrarily redesign or remove existing game-tracking data.

The existing Go structures should be mapped into clearer Java domain/JPA entities.

Names and types may be improved while preserving their meaning.

For example:

```text
Go                          Java domain

GameEntry             ->    Game
GameEntryDetails      ->    PlayEntry
Review                ->    Review
cover image handling  ->    Cover handling
```

The existing fields in the Go implementation should be reviewed during implementation and carried into the Java model unless there is a specific reason to change them.

---

# 19. Core Domain Hierarchy

The currently agreed core domain is:

```text
Library
   |
   +-- Game
        |
        +-- Cover
        |
        +-- PlayEntry
             |
             +-- Review
```

Relationships:

```text
Game       1 ---- N PlayEntry

PlayEntry  1 ---- N Review

Game       1 ---- 0..1 Cover
```

A Review does NOT directly belong to a Game.

A Review belongs to a particular PlayEntry.

---

# 20. Game

`Game` represents a video game in the user's BetterGameTracker library.

The initial fields should be derived from the existing Go `GameEntry` and associated game details structures.

The exact Java field definitions should therefore be created by mapping the existing Go model rather than inventing a new model.

Every Game should have a stable unique identifier.

UUIDs are preferred for persistent domain identifiers because they make future data migration significantly easier.

---

# 21. PlayEntry

A `PlayEntry` represents one instance/playthrough of a Game.

One Game can therefore have multiple PlayEntries.

Example:

```text
Persona 5 Royal
|
+-- PlayEntry
|   Platform: PS4
|   Completed: 2020
|
+-- PlayEntry
    Platform: PC
    Completed: 2025
```

The initial PlayEntry data should come from the existing Go `GameEntryDetails` / `PlayEntry` model.

The exact field list should not be redesigned until the existing Go model is mapped.

Every PlayEntry should have its own stable identifier.

A PlayEntry belongs to exactly one Game.

---

# 22. Review

Reviews belong to PlayEntries.

A PlayEntry can have multiple Reviews.

Example:

```text
Game
|
+-- PlayEntry
    |
    +-- Review
    |
    +-- Review
```

This is an intentional change/clarification from interpreting reviews as an overall Game review.

There is no single overall Game review in the currently agreed domain model.

The initial Review fields should be based on the Review data already present in the Go backend.

Every Review should have its own stable identifier.

---

# 23. Cover Images

A Game can have a cover image.

Cover image support already exists conceptually in the Go backend and must remain supported.

The image itself does not need to be stored as a database BLOB.

Local installations are expected to use filesystem-based cover storage.

Conceptually:

```text
Application data
|
+-- bettergametracker.db
|
+-- covers/
    +-- <game-id>.<extension>
```

The database may contain metadata/reference information about the cover.

Hosted cover-storage implementation details have not yet been finalized.

The application should avoid unnecessarily coupling domain logic to a specific physical storage implementation.

---

# 24. REST API

The frontend communicates with BetterGameTracker through HTTP.

The API should be versioned from the beginning.

Initial namespace:

```text
/api/v1/
```

Example conceptual resources:

```text
/api/v1/games

/api/v1/games/{gameId}

/api/v1/games/{gameId}/plays

/api/v1/games/{gameId}/plays/{playId}

/api/v1/games/{gameId}/plays/{playId}/reviews

/api/v1/games/{gameId}/plays/{playId}/reviews/{reviewId}

/api/v1/games/{gameId}/cover
```

The exact endpoint contract should be specified during API implementation.

Prefer resource-oriented REST endpoints over action-style names such as:

```text
/getAllGames
/newGameEntry
/deleteGameEntry
```

---

# 25. API Independence

The REST API is the primary architectural contract between the BetterGameTracker backend and its clients.

The backend must not assume that its bundled web frontend is the only API consumer.

Future clients could include:

```text
BetterGameTracker web frontend
Mobile application
CLI
Third-party client
```

Therefore, API behavior should remain independent from frontend implementation details.

---

# 26. Backend Package Structure

Prefer a feature-oriented package structure rather than putting every controller, entity, repository, and service into global technical packages.

A possible structure is:

```text
bettergametracker/
|
+-- game/
|   +-- Game
|   +-- GameController
|   +-- GameService
|   +-- GameRepository
|
+-- play/
|   +-- PlayEntry
|   +-- PlayEntryController
|   +-- PlayEntryService
|   +-- PlayEntryRepository
|
+-- review/
|   +-- Review
|   +-- ReviewController
|   +-- ReviewService
|   +-- ReviewRepository
|
+-- cover/
|   +-- CoverController
|   +-- CoverService
|   +-- CoverStorage
|
+-- security/
|
+-- config/
|
+-- system/
|
+-- migration/
```

This is guidance rather than an immutable package specification.

Do not introduce unnecessary architectural layers solely for theoretical purity.

BetterGameTracker should remain understandable and maintainable.

---

# 27. Controller Responsibilities

Controllers are responsible for HTTP concerns.

Examples:

- Parse requests
- Validate request format
- Call application services
- Produce HTTP responses
- Map application errors to appropriate HTTP status codes

Controllers should NOT contain:

- Database queries
- Persistence implementation
- Large amounts of business logic
- Filesystem manipulation
- Database migration logic

---

# 28. Service Responsibilities

Application services contain application/business behavior.

Examples:

```text
GameService
PlayEntryService
ReviewService
CoverService
```

Services coordinate repositories and other application components.

Example:

```text
HTTP request
     |
     v
GameController
     |
     v
GameService
     |
     v
GameRepository
     |
     v
Database
```

---

# 29. Repository Responsibilities

Repositories handle persistence.

The initial implementation uses Spring Data JPA / Hibernate.

Repositories should not know about HTTP.

They should not accept objects such as:

```text
HttpServletRequest
ResponseEntity
```

Persistence must remain separated from the web/API layer.

---

# 30. Local and Hosted Configuration

The same application should support environment-specific configuration.

Conceptually:

```text
LOCAL

Database:
SQLite

Authentication:
None

Network:
127.0.0.1

Cover storage:
Local filesystem

Distribution:
GraalVM native executable
```

Hosted:

```text
HOSTED

Database:
PostgreSQL

Authentication:
Google OAuth/OIDC

Network:
Server configuration

Distribution:
Spring Boot JAR or container
```

Spring profiles or equivalent configuration mechanisms may be used.

For example:

```text
application.yml
application-local.yml
application-hosted.yml
```

Do not duplicate application/business logic between profiles.

---

# 31. Local Native Build

The local native executable is only a distribution target.

It must not become a separate BetterGameTracker implementation.

Conceptually:

```text
Same source code
     |
     +-- bootJar
     |      |
     |      +-- hosted BetterGameTracker
     |
     +-- nativeCompile
            |
            +-- local BetterGameTracker
```

---

# 32. Hosted JAR

The hosted version may run as a conventional Spring Boot application:

```text
java -jar better-game-tracker.jar
```

Because the server operator controls the environment, requiring Java on the hosted infrastructure is acceptable.

This requirement does not apply to local BetterGameTracker users.

---

# 33. Hosted Container

BetterGameTracker may also be packaged as a container for hosted deployment.

Container support is a hosting option.

Docker or another container runtime must NEVER be required for ordinary local BetterGameTracker users.

---

# 34. Future Remote Endpoint Support

NOT INITIAL SCOPE.

BetterGameTracker is intended to eventually allow a local installation to use another BetterGameTracker server as its active backend.

Example:

```text
Browser
   |
   v
Local BetterGameTracker process
   |
   v
Configured remote BetterGameTracker server
```

The browser should continue talking to the local process.

The local BetterGameTracker process would proxy relevant API requests to the configured remote server.

This avoids coupling the browser directly to remote endpoints and centralizes:

- Authentication
- Remote configuration
- Endpoint validation
- Compatibility checking
- Migration behavior

This capability should be considered during architecture design but should NOT be implemented unless explicitly requested.

---

# 35. Future Local-to-Hosted Migration

NOT INITIAL SCOPE.

A user should eventually be able to start with:

```text
Local BetterGameTracker
```

and later move their library to:

```text
Hosted BetterGameTracker
```

The migration should preserve:

- Games
- PlayEntries
- Reviews
- Covers
- Relationships
- Stable identifiers where practical

Using UUIDs from the beginning is recommended partly to support this future capability.

Do not implement migration yet unless explicitly requested.

---

# 36. Future Hosted-to-Local Migration

NOT INITIAL SCOPE.

Users should eventually be able to export hosted BetterGameTracker data and restore/use it locally.

This prevents unnecessary data lock-in and provides a path for backup/restore.

The exact export format has not yet been designed.

Do not invent or implement the format prematurely.

---

# 37. Future Synchronization

NOT INITIAL SCOPE.

Local and hosted BetterGameTracker instances are NOT currently intended to synchronize continuously.

Do not implement:

- Bidirectional synchronization
- Conflict resolution
- Offline synchronization
- Tombstones
- Distributed change tracking

Endpoint switching and migration will be designed before synchronization is considered.

The data model should avoid decisions that make future synchronization unnecessarily difficult, but synchronization itself is not a current requirement.

---

# 38. Current Scope Versus Future Scope

Codex and developers must distinguish agreed architecture from future ideas.

## Current architecture

Implement/design around:

- Java
- Spring Boot
- Spring MVC
- JPA/Hibernate
- Flyway
- SQLite locally
- PostgreSQL hosted
- No local authentication
- Google OAuth/OIDC for hosted authentication
- GraalVM native local distribution
- JAR/container hosted distribution
- Browser-based frontend
- Backend-served static frontend
- Frontend-framework independence
- Versioned REST API
- Game
- PlayEntry
- Review belonging to PlayEntry
- Cover support
- Existing Go model as baseline data definition

## Future functionality

Do NOT implement without an explicit task:

- Endpoint switching
- Remote proxying
- Local -> hosted migration
- Hosted -> local migration
- Portable backup format
- Local/cloud synchronization
- Multiple libraries
- LAN server mode
- Automatic updates

---

# 39. Important Development Rule

Do not invent new product requirements merely because they appear architecturally convenient.

When implementing domain entities, first inspect the existing Go BetterGameTracker implementation.

The existing Go code defines the current game-tracking data that needs to survive the rewrite.

Refactoring names, Java types, relationships, and architecture is acceptable.

Silently dropping existing data fields or changing their semantics is not.

---

# 40. Development Priorities

The initial backend rewrite should prioritize:

1. Establish Spring Boot project.
2. Configure local and hosted application profiles.
3. Map existing Go domain structures to Java entities.
4. Correct the domain relationship to:

```text
Game
  -> PlayEntry
       -> Review
```

5. Configure SQLite.
6. Configure Flyway.
7. Configure JPA/Hibernate with schema validation.
8. Implement Game persistence/service/API.
9. Implement PlayEntry persistence/service/API.
10. Implement Review persistence/service/API.
11. Implement cover storage/API.
12. Serve framework-independent compiled frontend assets.
13. Produce local GraalVM native build.
14. Verify PostgreSQL compatibility.
15. Add hosted authentication when hosted deployment work begins.

Do not begin remote proxy/migration/synchronization functionality as part of the initial rewrite.

---

# 41. Architectural Summary

BetterGameTracker should ultimately look like:

```text
                    Web Frontend
                         |
                         | REST
                         v
                BetterGameTracker API
                         |
                  Spring MVC
                         |
                 Application Services
                         |
                Spring Data JPA
                         |
                    Hibernate
                         |
              +----------+----------+
              |                     |
              v                     v
           SQLite              PostgreSQL
            Local                 Hosted
```

Deployment:

```text
LOCAL

BetterGameTracker native executable
        |
        +-- Spring Boot backend
        +-- REST API
        +-- compiled web frontend
        +-- SQLite
        +-- local cover storage
        |
        v
Default web browser
```

```text
HOSTED

Spring Boot JAR / container
        |
        +-- REST API
        +-- compiled web frontend
        +-- Google OAuth/OIDC
        +-- PostgreSQL
        +-- hosted cover storage
        |
        v
Web browser
```

The same backend codebase supports both environments.

---

# 42. Status

This document describes the architecture agreed for BetterGameTracker so far.

Many lower-level product decisions remain intentionally unspecified and should be decided as implementation progresses.

When there is a conflict between an implementation convenience and this specification, do not silently change the architecture. Document the issue and decide whether the specification should be amended.

This document should evolve alongside BetterGameTracker.