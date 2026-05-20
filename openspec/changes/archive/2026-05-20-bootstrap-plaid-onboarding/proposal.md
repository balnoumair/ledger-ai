## Why

We have nothing yet — no repo layout, no app, no services. The goal is a working slice we can demo: a desktop app where the user clicks "Connect bank", goes through Plaid Sandbox, picks accounts, and the chosen accounts land in our own database. Everything downstream (dashboards, the CLI agent surface, AI bridge) depends on actually having linked account data to work with, so onboarding is the right first cut.

This change also locks in the three-service shape — Electron + React desktop, TypeScript BFF, Kotlin (Spring Boot) backend — so subsequent changes have a stable place to land code instead of relitigating structure.

## What Changes

- Stand up a pnpm monorepo at the repo root with three packages: `apps/desktop` (Electron + React + TypeScript + Vite), `services/bff` (Hono + TypeScript), `services/backend` (Spring Boot + Kotlin + Gradle).
- Add `infra/docker-compose.yml` with a Postgres service for local dev.
- Implement the Plaid Sandbox link flow end-to-end:
  - Backend (Kotlin) owns the Plaid SDK and is the only service that holds Plaid credentials or `access_token` values. Persists `Item` and `Account` rows in Postgres.
  - BFF (Hono) is a thin pass-through that exposes `/plaid/link-token`, `/plaid/exchange`, and `/accounts` to the desktop app and proxies to the backend.
  - Desktop app renders the two onboarding artboards from the design handoff: **Connect bank** (kicks off Plaid Link) and **Pick accounts** (lets the user toggle which sub-accounts to keep, then confirms).
- Apply the Aurora visual system from the design bundle (deep navy `#0d1224`, card `#141a30`, Geist + Geist Mono, solid surfaces — no gradients, no glassmorphism).
- No auth in this change: backend assumes a single local user row (`me`) seeded at startup. Anything that would need a real user identity (sessions, JWTs, multi-tenant) is explicitly out of scope.
- No dashboard, no AI bridge, no settings screens — those artboards exist in the design but are deferred to later changes.

## Capabilities

### New Capabilities
- `monorepo-scaffold`: Repo layout, pnpm workspace config, Gradle wrapper for the backend, root scripts, docker-compose for Postgres, and the Aurora design tokens shared by the desktop app.
- `plaid-link`: End-to-end Plaid Sandbox linking — backend Plaid integration, BFF pass-through endpoints, desktop Connect-bank and Pick-accounts screens, and the persistence model for linked Items and Accounts.

### Modified Capabilities
<!-- None — this is the first change in the project. -->

## Impact

- **Code**: Creates the entire repo. No existing code is touched (there isn't any).
- **APIs**: Establishes the initial BFF surface (`POST /plaid/link-token`, `POST /plaid/exchange`, `GET /accounts`) and the initial backend surface (same paths under an internal namespace, e.g. `/internal/plaid/...`). These shapes will be the contract subsequent changes extend.
- **Dependencies**: Adds Plaid Node-side SDK on the BFF only for type sharing if needed (the live SDK call happens in Kotlin via `plaid-java`); adds `plaid-java`, Spring Boot starters, Flyway, and PostgreSQL driver to the backend; adds React 18, Vite, Electron, `react-plaid-link`, and Geist fonts to the desktop app.
- **Systems**: Introduces Postgres as the backing store. Requires Docker for local development. Requires Plaid Sandbox credentials in a `.env` file at the backend.
- **Out of scope (called out so the proposal isn't read more broadly than intended)**: real auth, dashboard UI, transaction sync (we only fetch and store accounts here, not transactions), the AI bridge / CLI, settings screen, production deployment, CI.
