## Context

This is the first change in a brand-new repository. The product idea is a personal financial dashboard that links bank accounts via Plaid and lets coding agents (Claude Code, Codex, Cursor) interact with the user's finances via a CLI. The full product design has four sections (Onboarding, Dashboard, AI bridge, Settings — see the bundled design at [`docs/design/personal-ai-chekcer/`](../../../docs/design/personal-ai-chekcer/), in particular [`chats/chat1.md`](../../../docs/design/personal-ai-chekcer/chats/chat1.md) and [`chats/chat2.md`](../../../docs/design/personal-ai-chekcer/chats/chat2.md) where the user converged on the Aurora visual system), but this change only delivers the Onboarding section so we can converge fast on a working slice.

The three-service shape (Electron desktop, TS BFF, Kotlin backend) was decided up front with the user. The BFF exists primarily so the desktop app talks to one local origin and so the CLI (added in a later change) can authenticate against the same surface — it is not where business logic lives. The backend owns persistence, third-party integration, and the financial-data domain.

There is no existing code, no existing schema, no existing API. Everything in this design is greenfield.

## Goals / Non-Goals

**Goals:**

- A developer with Docker installed can run `pnpm install && pnpm dev` and reach a working Plaid Sandbox link flow.
- The Plaid `access_token` is owned by one and only one service (the Kotlin backend), and no other service can read it.
- The Aurora visual system is centralised in shared design tokens so the rest of the screens (added in later changes) inherit them for free.
- The repo structure scales: adding the dashboard screen later doesn't require restructuring; adding the CLI later doesn't require restructuring; adding a second BFF route later doesn't require restructuring.
- Demonstrate end-to-end: launch app → click Connect → Plaid Sandbox flow → see accounts in app → confirm → accounts in Postgres.

**Non-Goals:**

- Real authentication. There is no user model beyond a single seeded `me` row. JWT, sessions, OAuth, and multi-user are all out.
- Transaction sync. We persist `Item` and `Account` rows only. `/transactions/sync` integration is a later change.
- Dashboard, AI bridge, settings, CLI — all explicitly deferred.
- Production deployment, CI, observability, error monitoring. Dev-only ergonomics for this change.
- Pixel-perfect reproduction of the design canvas. The Aurora visual system (colors, type, surface treatment) is the contract; component-level layout we reproduce from the chat transcript intent rather than from the truncated component source we received in the handoff bundle.
- A polished React Router setup or state library. The two-screen onboarding flow is small enough to drive with local component state.

## Decisions

### Decision 1: pnpm workspaces, not Turborepo or Nx

**What:** Root `pnpm-workspace.yaml` lists `apps/*` and `services/*`. No Turborepo, no Nx.

**Why:** We have two TS packages and one Kotlin package (Kotlin isn't a pnpm workspace member at all — it's built via Gradle). pnpm workspaces alone are enough at this scale; Turborepo's task graph and remote cache become valuable around 5+ TS packages or in CI, and we have neither yet. We can layer Turborepo in later without restructuring.

**Alternative considered:** Turborepo. Rejected as premature for two packages.

### Decision 2: Kotlin backend talks to Plaid; BFF is a pass-through

**What:** The backend depends on `plaid-java` (Plaid's official Java SDK) and is the only service that constructs Plaid API calls or reads `PLAID_CLIENT_ID` / `PLAID_SECRET`. The BFF's Plaid routes just proxy: receive the request, forward to the backend's internal API, return the response.

**Why:**
- Single owner of the financial-data domain. The Kotlin backend is going to grow into the place where account types, balances, transactions, categorization, and Plaid webhooks live. Keeping the SDK there means there's exactly one place to look for Plaid behavior.
- `access_token` is a high-value secret. Constraining it to one service shrinks the audit surface — we don't have to think about whether it's leaking from the BFF's logs, its cache, or a stray response field.
- Plaid webhooks (later change) need to land on a stable backend endpoint; setting that ownership now avoids a refactor.

**Alternative considered:** BFF holds Plaid SDK; backend stores normalised data only. Rejected because it splits the financial-data domain and puts a secret in a second service for no real benefit.

### Decision 3: BFF in Hono on Node, not Fastify or Nest

**What:** Hono 4.x running on Node.js (not Bun for now — Electron's Node version sets our baseline, and we keep the runtimes aligned).

**Why:** Hono has the smallest API surface of the three options the user considered, which fits a pass-through BFF: most routes are 5-10 lines (parse, forward, return). Fastify is more featureful than we need; Nest is far more structure than fits a pass-through. If the BFF grows real business logic later we can revisit.

### Decision 4: Single local user, seeded at backend startup

**What:** Backend has a `users` table with one seeded row (`id = '00000000-0000-0000-0000-000000000001'`, `email = 'me@localhost'`). Every request is treated as that user. No header, no cookie, no token.

**Why:** Anything else introduces auth plumbing that the product doesn't need yet — the user expressly scoped this change to "the basics". When the CLI lands (a later change), we'll add a real auth mechanism appropriate to that surface (likely a long-lived install token, since it's per-machine).

**Trade-off:** The shape of the API will change when auth lands — endpoints currently keyed implicitly off `me` will need to read a user identity from the request. We accept the migration cost because doing it now adds complexity without value.

### Decision 5: Postgres only; no Redis, no queue

**What:** `docker-compose.yml` brings up exactly one service: `postgres:16`. No Redis, no NATS, no RabbitMQ.

**Why:** We don't have any caching, background job, or fan-out concern in scope. Adding infrastructure preemptively is the kind of half-finished implementation we want to avoid. Plaid's `/transactions/sync` (later change) can drive off a Spring `@Scheduled` poller backed by Postgres for state — no broker required.

### Decision 6: Flyway for schema migrations

**What:** The backend uses Flyway with SQL migrations under `services/backend/src/main/resources/db/migration/`. The initial migration (`V1__init.sql`) creates `users`, `plaid_items`, `plaid_accounts`.

**Why:** Flyway is the de facto standard with Spring Boot, has automatic detection, and keeps migrations as plain SQL (easy to review and grep). Liquibase considered and rejected as more ceremony than we need.

### Decision 7: Electron + Vite, renderer is a plain React 18 SPA

**What:** `electron-vite` template at the package level. The main process is a thin shim that creates one window pointing at the Vite dev server (in dev) or the built `index.html` (in prod). The preload script exposes nothing beyond a typed `window.api` — and for this change, even that is empty, because the app talks to the BFF over HTTP at `http://localhost:<bff-port>`, not via IPC.

**Why:** Keeps the renderer indistinguishable from a regular React app. We can lift the same renderer into a web build later if needed. We avoid Electron-specific complications (context isolation gotchas, preload bridges) until something genuinely requires them.

### Decision 8: `react-plaid-link` runs in the renderer

**What:** The desktop app uses `react-plaid-link` to mount Plaid's web-based Link experience inside the Electron window. The link token comes from the backend (via the BFF); the `public_token` returned by Link is posted back to the BFF for exchange.

**Why:** Plaid's mobile SDKs target iOS / Android, not Electron. The web-Link flow is what's officially supported in desktop browser-like contexts and is what Plaid documents for Electron apps. Sandbox mode works identically.

**Risk:** Plaid Link wants a real browser environment with cookies enabled. Electron defaults are fine, but we should make sure we don't strip cookies via session config — flagged as a verification step in tasks.

### Decision 9: API shape

The BFF and backend mirror each other's surface, but the backend's lives under `/internal/...` to make the boundary obvious:

| Desktop → BFF                        | BFF → Backend                                |
|--------------------------------------|----------------------------------------------|
| `POST /api/plaid/link-token`         | `POST /internal/plaid/link-token`            |
| `POST /api/plaid/exchange`           | `POST /internal/plaid/exchange`              |
| `GET  /api/accounts`                 | `GET  /internal/accounts`                    |
| `PATCH /api/accounts/:id`            | `PATCH /internal/accounts/:id`               |

Bodies and response shapes are identical at both hops in this change — the BFF doesn't reshape data, only adds CORS and request validation.

### Decision 10: Aurora design tokens as CSS custom properties

**What:** A single `tokens.css` file declares all Aurora tokens on `:root` and on `:root.light`. The desktop app imports it once in its renderer entry. Components use `var(--...)` references everywhere — no inline hex values, no JS theme object.

**Why:** CSS custom properties travel through every component, including third-party React components (like Plaid Link's container, if we wrap it), without prop drilling. They also let the light-mode toggle (when added later) be a single class swap on `<html>`. A JS theme object would tightly couple us to a CSS-in-JS library, which we don't want.

## Risks / Trade-offs

- **Truncated design source** → The tar bundle from the design handoff was truncated before we could read the `PlaidConnect` / `AccountSelect` component bodies. We reproduce them from the Aurora visual system described in the chat transcripts plus standard patterns for a two-screen Plaid Link flow. **Mitigation:** Treat the design as a visual spec (colors, type, surface treatment) and accept that the layout we ship is a best-effort recreation. The user can iterate on layout once they see it in the app.

- **No auth → eventual rewrite of all endpoints** → Every endpoint added in this change will need to learn how to read a user identity when auth lands. **Mitigation:** We pick endpoint shapes that don't bake `me`-ness into the URL (e.g. `/api/accounts` not `/api/me/accounts`), so adding auth is "read user id from middleware" rather than a path change.

- **`access_token` storage at rest** → For this change we store `access_token` in plain text in Postgres. This is fine for Sandbox but unacceptable for production. **Mitigation:** Document this in the README; a later change adds at-rest encryption (likely Spring's `Jasypt` or pgcrypto). Flag in tasks so it's not forgotten.

- **Electron + Plaid cookie quirks** → If the Electron `session` is configured oddly, Plaid Link may fail silently or loop on the OAuth handoff step. **Mitigation:** Verify with at least one Sandbox institution that uses OAuth (e.g. Tartan Bank) during implementation, not just a credentials-only one (First Platypus Bank).

- **pnpm-version drift** → Mixing pnpm versions across machines causes lockfile churn. **Mitigation:** Pin the pnpm version with `packageManager` in the root `package.json` and check in `.npmrc` with `engine-strict=true`.

- **Sandbox-only Plaid env** → `PLAID_ENV` defaults to `sandbox`. If anyone sets it to `development` or `production` accidentally, they'll burn real Plaid budget. **Mitigation:** Backend startup logs the resolved env explicitly, and the README warns to keep it on sandbox until a later change opts in.

## Open Questions

- Should the Pick-accounts screen's "skip" state delete the row or flag it? This spec says flag (`included = false`). Keeping the row preserves history if the user changes their mind, but means future code has to remember to filter. Worth revisiting once the dashboard is in.
- Do we want `plaid-link` requirements to include re-link / item-error handling now, or strictly the happy path? This change targets happy path only; re-link is a later change.
- Do we want a smoke test (Playwright + a stubbed Plaid Link) in this change, or defer all tests to a follow-up? Currently deferred — flag in tasks.
