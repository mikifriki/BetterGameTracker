# BetterGameTracker backend scaffold

This module is a Java 21 Spring Boot foundation. It has local (SQLite) and hosted
(PostgreSQL) profiles, with JPA mappings and an initial Flyway schema for games,
play entries, and reviews. API routes are intentionally not implemented yet.

The local profile is the default and binds only to `127.0.0.1`. It stores SQLite
data persistently in the operating system's application-data location:
`%LOCALAPPDATA%/BetterGameTracker` on Windows, `~/Library/Application Support/BetterGameTracker`
on macOS, and `$XDG_DATA_HOME/BetterGameTracker` (or `~/.local/share/BetterGameTracker`)
on other systems. Set `BETTER_GAME_TRACKER_DATABASE_PATH` to choose a different
database file. The application creates the database's parent directory at startup.

Flyway is enabled in both profiles and owns schema changes. `V1__create_initial_domain_schema.sql`
creates `games`, `play_entries`, and `reviews`; relationships use restrictive
foreign keys, so direct parent deletion or invalid relationship updates cannot
silently remove or detach related data. Hibernate uses `ddl-auto=validate` and
will not mutate the schema. The shared migration uses Flyway's `uuidType`
placeholder because Hibernate maps UUID values to `BLOB` for SQLite and PostgreSQL
has a native `UUID` type.

The hosted profile requires `BETTER_GAME_TRACKER_DATABASE_URL`,
`BETTER_GAME_TRACKER_DATABASE_USERNAME`, and
`BETTER_GAME_TRACKER_DATABASE_PASSWORD`. Google OAuth/OIDC and native-image
packaging are intentionally deferred until their respective implementations.

Build and test with Java 21:

```sh
cd backend
./gradlew test
```

Run the default local scaffold with:

```sh
cd backend
./gradlew bootRun
```

To start the hosted profile after configuring PostgreSQL:

```sh
cd backend
SPRING_PROFILES_ACTIVE=hosted ./gradlew bootRun
```

The hosted profile is configuration-only at this stage. It has not been smoke
tested because no PostgreSQL service is part of this repository.
