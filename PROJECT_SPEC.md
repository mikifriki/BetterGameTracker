# BetterGameTracker — Project Specification

## 1. Purpose

BetterGameTracker is a self-hostable game tracking application.

The primary goal is to allow a user to maintain a personal video game library containing:

- Games
- Individual play entries/playthroughs for games
- Play-time entries associated with individual play entries
- Reviews associated with individual play entries
- Game cover images

BetterGameTracker supports two primary deployment models:

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

## 2. Core Design Principles

### 2.1 One Backend

There must not be separate local and hosted BetterGameTracker implementations.

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
          Local                     Hosted
            |                         |
          SQLite                  PostgreSQL
            |                         |
     Native executable          JAR / Container
```

Business logic, domain models, API contracts, and application services must be shared.

### 2.2 Simple Architecture

BetterGameTracker should remain simple, understandable, and maintainable.

Do not introduce architectural layers or abstractions solely for theoretical purity or hypothetical future requirements.

### 2.3 Java Domain and Data

The Java domain model defines the application. Domain fields remain product data
unless explicitly removed. Released database migrations remain immutable; new
releases evolve the schema through Flyway.

---

## 3. Technology Stack

Backend:

```text
Java
Spring Boot
Spring MVC
```

Persistence:

```text
Spring Data JPA
Hibernate
Flyway
```

Local database:

```text
SQLite
```

Hosted database:

```text
PostgreSQL
```

Hosted authentication:

```text
Spring Security
Google OAuth / OpenID Connect
```

Local distribution:

```text
GraalVM Native Image
```

Hosted distribution:

```text
Spring Boot JAR
or
Container image
```

The hosted application does not need to use GraalVM Native Image.

---

## 4. Frontend Architecture

The BetterGameTracker backend must not depend on a specific frontend framework.

The frontend is a separate web application that consumes the BetterGameTracker HTTP API.

The frontend may be implemented using Angular, React, Vue, another web framework, or plain web technologies.

The frontend framework must not affect backend architecture.

Conceptually:

```text
Web Frontend
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

The REST API is the architectural contract between the frontend and backend.

The backend must not assume that the bundled web frontend is the only possible API consumer.

Future clients may include:

- Web frontend
- Mobile application
- CLI
- Third-party clients

Backend services and domain logic must not expose frontend-framework-specific concepts.

---

## 5. Serving the Frontend

The BetterGameTracker backend should be capable of serving compiled frontend assets.

A frontend build is expected to produce files such as:

```text
index.html
*.js
*.css
assets/
```

These compiled files may be packaged into the backend.

Spring Boot serves frontend assets under:

```text
/
```

Application API endpoints are served under:

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

Serving frontend files through Spring Boot is a deployment mechanism only.

Frontend source code is not required at runtime.

---

## 6. Repository Structure

The frontend and backend should remain separate projects or modules.

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

The backend build may consume compiled frontend output when producing a release.

---

## 7. Local Application

The local BetterGameTracker installation is intended for ordinary users who do not want to host a server.

Expected flow:

```text
User launches BetterGameTracker
           |
           v
Native backend starts
           |
           v
SQLite database opens
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

### Local Authentication

Local installations have no authentication.

There must be no:

- Local account
- Local login
- Local password
- Artificial default user

The local installation is initially a single-user application.

### Local Networking

The application must bind to the loopback interface by default:

```text
127.0.0.1
```

It must not bind to:

```text
0.0.0.0
```

by default.

An ordinary local installation therefore must not automatically expose BetterGameTracker to other devices on the LAN.

The exact local port strategy has not yet been finalized.

### Local Database

Local installations use SQLite.

The user must not need to install or configure a database server.

The SQLite database should live in the operating system's standard application-data location rather than beside the executable.

Conceptual locations:

Windows:

```text
%LOCALAPPDATA%/BetterGameTracker/
```

macOS:

```text
~/Library/Application Support/BetterGameTracker/
```

Exact paths may be finalized during implementation.

### Local Distribution

The local application should be distributed using GraalVM Native Image.

Example release targets:

```text
Windows:
BetterGameTracker.exe

macOS:
BetterGameTracker native application/binary
```

The user must not need a Java installation.

Platform-specific native binaries may be produced using CI such as GitHub Actions.

---

## 8. Hosted Application

The same BetterGameTracker backend can run as a hosted service.

The hosted application may run as:

```text
java -jar better-game-tracker.jar
```

or as a container.

The server operator may provide infrastructure dependencies that ordinary local users must not be required to install.

### Hosted Database

Hosted BetterGameTracker uses PostgreSQL.

The application domain and business logic must not contain SQLite-specific or PostgreSQL-specific behavior.

Database-specific differences belong in infrastructure or configuration where possible.

### Hosted Authentication

Hosted BetterGameTracker uses authentication.

The selected authentication provider is:

```text
Google OAuth / OpenID Connect
```

Spring Security should integrate Google authentication.

BetterGameTracker must maintain its own internal user identity.

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

## 9. Environment Configuration

Local and hosted deployments use the same application code with environment-specific configuration.

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

```text
HOSTED

Database:
PostgreSQL

Authentication:
Google OAuth/OIDC

Network:
Server configuration

Cover storage:
Hosted storage implementation

Distribution:
Spring Boot JAR or container
```

Spring profiles or equivalent mechanisms may be used.

For example:

```text
application.yml
application-local.yml
application-hosted.yml
```

Business logic must not be duplicated between deployment profiles.

---

## 10. Backend Architecture

Use the following dependency direction:

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

Application services contain application and business behavior.

Repositories handle persistence.

Database queries, filesystem persistence, and business logic must not be placed directly in controllers.

Repositories must not depend on HTTP-specific types such as:

```text
HttpServletRequest
ResponseEntity
```

Business logic must not be placed in repository implementations.

---

## 11. Backend Package Structure

Prefer a feature-oriented package structure rather than global technical packages containing every controller, entity, service, or repository.

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
|   +-- PlayTimeEntry
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
+-- config/
+-- system/
+-- migration/
```

This is guidance rather than an immutable package specification.

---

## 12. Database Schema Management

BetterGameTracker uses Flyway for schema creation and migration.

Flyway owns schema evolution.

Hibernate must validate mappings rather than automatically modifying user or production schemas.

The intended Hibernate behavior is equivalent to:

```text
ddl-auto = validate
```

Application startup should conceptually behave as follows:

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

This is especially important after the first release because users may keep the
same SQLite database across many BetterGameTracker releases.

### Migration Rules

SQLite and PostgreSQL migrations should be shared where reasonably possible.

Prefer portable SQL when doing so does not significantly compromise the schema.

Database-specific migrations are allowed when genuinely required.

Do not distort the database design solely to avoid all database-specific migrations.

Until the first release, the V1 baseline may be rewritten and development databases
may be recreated. No development-data upgrade path is required.

After the first release, shipped migrations must not be modified. Future schema
changes require new migrations.

Example:

```text
V1__initial_schema.sql
V2__add_play_entries.sql
V3__add_reviews.sql
V4__add_cover_metadata.sql
V5__add_play_time_entries.sql
```

Application upgrades after the first release must preserve existing user data.

---

## 13. Application Data

The Java backend defines application data. Games and their children use UUIDs.

---

## 14. Core Domain Model

The currently agreed domain hierarchy is:

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
           |
           +-- PlayTimeEntry
```

Relationships:

```text
Game          1 ---- N PlayEntry

PlayEntry     1 ---- N Review

PlayEntry     1 ---- N PlayTimeEntry

Game          1 ---- 0..1 Cover
```

Persistent domain objects should use stable UUID identifiers unless a specific requirement says otherwise.

---

## 15. Game

A `Game` represents a video game in the user's BetterGameTracker library.

The Java Game model defines the game metadata. A title-derived URL key is not part of the model.

Every Game should have a stable unique identifier.

A Game may have:

- Zero or more PlayEntries
- Zero or one cover image

---

## 16. PlayEntry

A `PlayEntry` represents one instance or playthrough of a Game.

One Game can have multiple PlayEntries.

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

The Java PlayEntry model defines playthrough metadata.

Every PlayEntry has its own stable identifier.

A PlayEntry belongs to exactly one Game.

A PlayEntry has an optional start date. New playthroughs default an omitted or null
start date to the current date when created. Existing playthroughs may retain a null
start date, and a full-replacement update may explicitly clear it.

A PlayEntry may have:

- Zero or more Reviews
- Zero or more PlayTimeEntries

---

## 17. PlayTimeEntry

A `PlayTimeEntry` represents an individual amount of time spent playing during a particular `PlayEntry`.

This allows play time to be recorded incrementally during a playthrough instead of relying only on one total playtime value.

Example:

```text
PlayEntry
 |
 +-- PlayTimeEntry
 |    Date: 2026-09-01
 |    Duration: 120 minutes
 |    Notes: Reached chapter 3
 |
 +-- PlayTimeEntry
      Date: 2026-09-02
      Duration: 90 minutes
      Notes: Optional
```

A PlayTimeEntry contains:

- Stable UUID identifier
- Required `LocalDate date`
- Positive integer `durationMinutes`
- Optional notes

A PlayTimeEntry belongs to exactly one PlayEntry.

A PlayEntry may have zero or more PlayTimeEntries.

Multiple PlayTimeEntries for the same PlayEntry and the same date are valid.

PlayTimeEntry is optional. Existing PlayEntries do not require PlayTimeEntries.

This is an explicitly approved BetterGameTracker requirement.

---

## 18. Review

A `Review` belongs to exactly one PlayEntry.

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

A Review does not directly belong to a Game.

There is no single overall Game review in the currently agreed domain model.

Review contains an optional date, title, text, and rating.

Every Review should have its own stable identifier.

---

## 19. Cover Images

A Game can have a cover image.

Each game has zero or one cover, stored under its UUID through the cover API.

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

The database may contain metadata or reference information about the cover.

Hosted cover-storage implementation details have not yet been finalized.

Domain logic should not be unnecessarily coupled to a particular physical storage implementation.

---

## 20. REST API

The frontend communicates with BetterGameTracker through HTTP.

The API is versioned from the beginning.

Initial namespace:

```text
/api/v1/
```

Conceptual resources include:

```text
/api/v1/games

/api/v1/games/{gameId}

/api/v1/games/{gameId}/plays

/api/v1/games/{gameId}/plays/{playId}

/api/v1/games/{gameId}/plays/{playId}/reviews

/api/v1/games/{gameId}/plays/{playId}/reviews/{reviewId}

/api/v1/games/{gameId}/cover
```

PlayTimeEntry API endpoints should be defined when PlayTimeEntry API functionality is implemented.

Prefer resource-oriented REST endpoints over action-style routes such as:

```text
/getAllGames
/newGameEntry
/deleteGameEntry
```

The exact endpoint contracts should be specified during API implementation.

---

## 21. Future Remote Endpoint Support

This is not part of the initial scope.

BetterGameTracker is intended to eventually allow a local installation to use another BetterGameTracker server as its active backend.

Conceptually:

```text
Browser
  |
  v
Local BetterGameTracker process
  |
  v
Configured remote BetterGameTracker server
```

The browser should continue communicating with the local BetterGameTracker process.

The local process may eventually proxy relevant requests to the configured remote server.

This could centralize:

- Authentication
- Remote configuration
- Endpoint validation
- Compatibility checking
- Migration behavior

Do not implement remote endpoint support unless explicitly requested.

---

## 22. Future Local-to-Hosted Migration

This is not part of the initial scope.

A user should eventually be able to start with:

```text
Local BetterGameTracker
```

and later move their library to:

```text
Hosted BetterGameTracker
```

Migration should preserve:

- Games
- PlayEntries
- PlayTimeEntries
- Reviews
- Covers
- Relationships
- Stable identifiers where practical

Using UUID identifiers from the beginning is partly intended to support this future capability.

Do not implement migration unless explicitly requested.

---

## 23. Future Hosted-to-Local Migration

This is not part of the initial scope.

Users should eventually be able to export hosted BetterGameTracker data and restore or use it locally.

This provides a path for backup, portability, and avoiding unnecessary data lock-in.

The exact export format has not yet been designed.

Do not invent or implement the format prematurely.

---

## 24. Future Synchronization

Continuous synchronization between local and hosted BetterGameTracker instances is not currently planned.

Do not implement:

- Bidirectional synchronization
- Conflict resolution
- Offline synchronization
- Tombstones
- Distributed change tracking

Endpoint switching and migration should be designed before synchronization is considered.

The data model should avoid decisions that make future synchronization unnecessarily difficult, but synchronization itself is not a current requirement.

---

## 25. Current Scope and Future Scope

### Current Architecture

Implement and design around:

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA / Hibernate
- Flyway
- SQLite locally
- PostgreSQL when hosted
- No local authentication
- Google OAuth/OIDC for hosted authentication
- GraalVM native local distribution
- JAR or container hosted distribution
- Browser-based frontend
- Backend-served compiled frontend
- Frontend-framework independence
- Versioned REST API
- Game
- PlayEntry
- PlayTimeEntry belonging to PlayEntry
- Review belonging to PlayEntry
- Cover support
- Java domain model as the data definition

### Future Functionality

Do not implement without an explicit task:

- Endpoint switching
- Remote proxying
- Local-to-hosted migration
- Hosted-to-local migration
- Portable backup format
- Local/cloud synchronization
- Multiple libraries
- LAN server mode
- Automatic updates

Architecture should avoid unnecessarily blocking likely future work, but future requirements must not create unnecessary complexity in current implementations.

---

## 26. Development Rules

Do not invent new product requirements merely because they appear architecturally convenient.

When changing domain data, inspect the Java implementation and its tests.

Explicitly approved new requirements may extend the Java model.

Refactoring names, Java types, relationships, and architecture is acceptable.

Silently dropping existing data fields or changing their semantics is not.

When a requested change conflicts with this specification, surface the conflict rather than silently changing the agreed architecture.

---

## 27. Architectural Summary

BetterGameTracker should conceptually look like:

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

Local deployment:

```text
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

Hosted deployment:

```text
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

## 28. Status

This document describes the currently agreed BetterGameTracker product architecture.

Lower-level implementation decisions that are not specified here should be made as implementation progresses.

When an implementation convenience conflicts with this specification, do not silently change the architecture. The conflict should be identified and the specification updated only when the architectural decision itself changes.

This document should evolve alongside BetterGameTracker.
