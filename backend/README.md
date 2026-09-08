# BetterGameTracker

Java 21 / Spring Boot application with a bundled browser frontend.

## Architecture

Feature packages contain controllers, transactional services, JPA entities,
repositories and record DTOs. Controllers map HTTP requests and responses; services
own mutations and access checks; repositories query the database. There is no
mapping framework or generic service hierarchy.

- `Game` → many `PlayEntry` → many `Review` and `PlayTimeEntry`.
- Each game has zero or one filesystem cover, addressed by the game's UUID.
- Hosted games belong to an internal UUID user associated with Google's stable
  subject identifier. Game access is scoped to that owner; nested operations reuse
  that check. User IDs and JPA relationships are not exposed in response DTOs.
- Local mode has no accounts. Its games have no owner, and queries select only
  those games.
- Flyway owns schema creation; Hibernate validates it. The application is still in
  development, so one shared V1 migration defines the complete current schema.
- `frontend/` is plain HTML, CSS and JavaScript copied into the application's
  static resources at build time. Neither Node nor a frontend framework is needed.

## API

All routes start with `/api/v1`. UUID paths determine ownership; request fields
cannot reassign resources.

| Resource | Collection operations | Individual operations |
| --- | --- | --- |
| `/games` | GET, POST | GET, PUT, DELETE |
| `/games/{gameId}/plays` | GET, POST | GET, PUT, DELETE |
| `/games/{gameId}/plays/{playId}/reviews` | GET, POST | GET, PUT, DELETE |
| `/games/{gameId}/plays/{playId}/time-entries` | GET, POST | PUT, DELETE |
| `/games/{gameId}/cover` | — | GET, PUT multipart `file`, DELETE |
| `/session` | — | GET |

Create returns 201 and Location; delete returns 204. Missing or inaccessible
resources return 404. Invalid requests return 400 with Problem Details.

Game fields: only `gameTitle` is required and must be nonblank. `description`,
`releasePlatform`, `releaseDate`, `developer`, `metaRating`, `userRating`, and
`physicalCopy` are optional.
Play fields: required nonblank `playthroughRating`, `completionDate`,
`platformPlayedOn`, `completionRate`, `location`; optional `timeToBeat` and `coop`.
Review fields (`reviewDate`, `reviewTitle`, `review`, `rating`) are optional.
These dates and ratings remain strings and are stored as `VARCHAR(255)`. Time
entries use a required ISO date, positive integer `durationMinutes`, and optional
`notes`. Same-date entries are allowed; fractional durations are rejected. PUT
replaces editable scalar fields.

Game deletion cascades to its plays, reviews and time entries. Cover files are
removed after the database commit. Uploads accept PNG/JPEG signatures, up to 5 MB,
ignore client filenames and replace files atomically. Cover access checks game
ownership; images are served without caching and with `nosniff`.

## Local use

```sh
./gradlew bootRun
```

The default `local` profile binds to `127.0.0.1:8080` and opens the browser after
startup. No login is required. Requests with foreign Origin headers or non-loopback
hostnames are rejected. SQLite uses one pooled connection to serialize database
transactions and avoid competing connection writes within this process.

Data locations:

- Windows: `%LOCALAPPDATA%/BetterGameTracker`
- macOS: `~/Library/Application Support/BetterGameTracker`
- Linux: `$XDG_DATA_HOME/BetterGameTracker` or `~/.local/share/BetterGameTracker`

Optional environment variables:

- `BETTER_GAME_TRACKER_DATABASE_PATH`: SQLite file path.
- `BETTER_GAME_TRACKER_COVER_DIRECTORY`: cover directory; defaults to the database
  path plus `.covers`.
- `BETTER_GAME_TRACKER_OPEN_BROWSER=false`: suppress browser launch.
- `SERVER_PORT`: choose another port if 8080 is occupied.

Use exactly one deployment profile. Do not expose the unauthenticated local mode
as a hosted service.

## Hosted use

Build with `./gradlew bootJar`, then configure:

```text
SPRING_PROFILES_ACTIVE=hosted
BETTER_GAME_TRACKER_DATABASE_URL=jdbc:postgresql://database:5432/better_game_tracker
BETTER_GAME_TRACKER_DATABASE_USERNAME=...
BETTER_GAME_TRACKER_DATABASE_PASSWORD=...
BETTER_GAME_TRACKER_GOOGLE_CLIENT_ID=...
BETTER_GAME_TRACKER_GOOGLE_CLIENT_SECRET=...
BETTER_GAME_TRACKER_COVER_DIRECTORY=/persistent/covers
```

Run `java -jar build/libs/better-game-tracker-backend-0.0.1-SNAPSHOT.jar`.
Provide HTTPS, PostgreSQL and a writable persistent cover directory. In Google's
OAuth configuration, register `https://YOUR_HOST/login/oauth2/code/google` as the
redirect URI. Sign-in starts at `/oauth2/authorization/google`.

Hosted sessions use Secure, HttpOnly, SameSite=Lax cookies. CSRF remains enabled;
clients fetch `/api/v1/session` and send its token using the returned header name
on writes, including multipart uploads and POST `/logout`. The bundled frontend
handles this. API reads without authentication return 401.

If TLS terminates at a reverse proxy, configure `SERVER_FORWARD_HEADERS_STRATEGY=native`
and the trusted proxy settings for that deployment, and prevent direct public
access to the backend. Do not blindly trust client-supplied forwarded headers.
Sessions are in memory; restart requires signing in again. This release targets
one application instance with persistent filesystem storage.

## Builds and verification

```sh
./gradlew build
python3 scripts/smoke.py java -jar build/libs/better-game-tracker-backend-0.0.1-SNAPSHOT.jar
```

Native local builds require GraalVM JDK 21 and platform build tools on the build
machine, not on end-user machines:

```sh
./gradlew -Pnative nativeCompile
python3 scripts/smoke.py build/native/nativeCompile/BetterGameTracker
```

Windows produces `BetterGameTracker.exe`. Native builds use the local profile;
hosted deployments use the JAR. The repository CI workflow builds/test-checks the
JAR against PostgreSQL and builds/smoke-checks native binaries on Windows and
macOS. It does not publish unsigned binaries as a release.

The default tests use isolated SQLite databases, including the hosted security
integration suite. Set `BGT_TEST_POSTGRES_URL`, `BGT_TEST_POSTGRES_USERNAME` and
`BGT_TEST_POSTGRES_PASSWORD` to run that suite against a dedicated empty PostgreSQL
test database. It applies migrations and writes test data; never point it at a
production database.

## Release verification still required

The JVM suite and packaged-JAR smoke test pass, and hosted ownership/security tests
have also passed against PostgreSQL 14.22. A passing JVM build alone does not certify a production release. Confirm the CI
PostgreSQL/native jobs, the real Google login/logout flow over HTTPS, and frontend
interaction testing before release. Native signing/notarization is an operator
release step. Keep database and cover backups together. Filesystem failures after
a committed game deletion are logged and can leave an unreachable cover file for
operator cleanup; the database and filesystem are not a distributed transaction.
Collections are currently unpaginated, suitable for a personal library.
