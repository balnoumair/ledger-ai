# services/backend

Spring Boot 3 + Kotlin 2 + JDK 21. Owns Plaid integration and Postgres.

## One-time setup

This repo doesn't check in the `gradle-wrapper.jar` binary. Generate it once:

```bash
# Option A: if you already have Gradle installed locally
gradle wrapper --gradle-version 8.11

# Option B: via Docker (no local Gradle needed)
docker run --rm -v "$PWD":/work -w /work gradle:8.11-jdk21 gradle wrapper
```

After that, `./gradlew bootRun` works.

## Environment

Copy `.env.example` to `.env` and fill in your Plaid Sandbox credentials. Spring Boot does not autoload `.env`; the `pnpm dev:backend` script wraps `./gradlew bootRun` so the values can be exported manually or via direnv. For local development you can also pass them inline:

```bash
PLAID_CLIENT_ID=... PLAID_SECRET=... ./gradlew bootRun
```

Postgres must be running first (`pnpm dev:db` from the repo root, or `docker compose -f ../../infra/docker-compose.yml up -d postgres`).

## Tasks

- `./gradlew bootRun` — run the server (port `8080`)
- `./gradlew check` — runs Spotless ktlint + tests
- `./gradlew test` — tests only
- `./gradlew spotlessApply` — auto-format Kotlin
