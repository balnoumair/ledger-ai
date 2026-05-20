## 1. Monorepo skeleton

- [x] 1.1 Create root `package.json` with `"private": true`, `packageManager` pinned to a specific pnpm version, and the orchestrating scripts (`dev`, `dev:db`, `dev:backend`, `dev:bff`, `dev:desktop`, `typecheck`) defined per the spec
- [x] 1.2 Create root `pnpm-workspace.yaml` listing `apps/*` and `services/*`
- [x] 1.3 Create root `.gitignore` covering `node_modules`, build outputs, `.env*`, Gradle caches, and Electron artifacts
- [x] 1.4 Create root `.editorconfig`, `.prettierrc`, `.eslintrc.cjs` (or flat config), shared by both TS packages
- [x] 1.5 Create root `README.md` documenting prerequisites (Node, pnpm, Docker, JDK 21), the dev-stack startup sequence, and the Plaid Sandbox env vars
- [x] 1.6 Create `infra/docker-compose.yml` with a `postgres:16` service, named volume, healthcheck, and credentials sourced from `infra/.env`
- [x] 1.7 Create `infra/.env.example` documenting `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`

## 2. Aurora shared design tokens

- [x] 2.1 Create `apps/desktop/src/renderer/styles/tokens.css` declaring all Aurora tokens on `:root` and the light-mode overrides on `:root.light`, matching the spec
- [x] 2.2 Add Geist and Geist Mono via `@fontsource/geist-sans` and `@fontsource/geist-mono` (or self-host); import in the renderer entry
- [x] 2.3 Create `apps/desktop/src/renderer/styles/base.css` for body reset, dark default, font-family wiring off the tokens
- [x] 2.4 Import `tokens.css` and `base.css` from the renderer entry exactly once

## 3. Kotlin backend — scaffold

- [x] 3.1 Initialize `services/backend` with Gradle Kotlin DSL, Spring Boot 3.x, JDK 21, Kotlin 2.x
- [x] 3.2 Add dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `org.postgresql:postgresql`, `org.flywaydb:flyway-core`, `com.plaid:plaid-java`, Kotlin test starter
- [x] 3.3 Wire Spotless (or ktlint Gradle plugin) into `./gradlew check`
- [x] 3.4 Create `application.yml` reading `PLAID_CLIENT_ID`, `PLAID_SECRET`, `PLAID_ENV` (default `sandbox`), and Postgres connection from env
- [x] 3.5 On startup, log the resolved `PLAID_ENV` explicitly (per design risk mitigation)
- [x] 3.6 Add a `.env.example` for the backend documenting required Plaid variables

## 4. Kotlin backend — schema and domain

- [x] 4.1 Write `V1__init.sql` Flyway migration: `users (id uuid PK, email text unique, created_at timestamptz)`, `plaid_items (id uuid PK, item_id text unique, access_token text, institution_name text, institution_id text null, user_id uuid FK, created_at timestamptz)`, `plaid_accounts (id uuid PK, item_id uuid FK, plaid_account_id text unique, name text, official_name text null, mask text null, type text, subtype text null, current_balance numeric null, available_balance numeric null, iso_currency_code text null, included boolean default true, created_at timestamptz)`
- [x] 4.2 Add a startup seeder that inserts the single `me` user row with the fixed UUID `00000000-0000-0000-0000-000000000001` if not present
- [x] 4.3 Define JPA entities for `User`, `PlaidItem`, `PlaidAccount` and Spring Data repositories for each

## 5. Kotlin backend — Plaid integration

- [x] 5.1 Configure a Plaid `ApiClient` bean wired to the configured env (Sandbox / Development / Production)
- [x] 5.2 Implement `PlaidService.createLinkToken(userId)` calling `/link/token/create` with `products=[transactions]`, `country_codes=[US]`, `language=en`, and a stable `client_user_id` derived from the user UUID
- [x] 5.3 Implement `PlaidService.exchangePublicToken(publicToken, institution)` that exchanges the token, persists a `PlaidItem`, calls `/accounts/get`, persists each account, and returns the persisted accounts
- [x] 5.4 Verify (manually in a unit or integration test) that no response DTO contains `access_token`

## 6. Kotlin backend — HTTP surface

- [x] 6.1 Implement `POST /internal/plaid/link-token` returning `{ link_token, expiration }`
- [x] 6.2 Implement `POST /internal/plaid/exchange` accepting `{ public_token, institution }`, returning the list of persisted accounts (no `access_token`, no internal `item_id`)
- [x] 6.3 Implement `GET /internal/accounts` returning all accounts for `me`, joined with institution name
- [x] 6.4 Implement `PATCH /internal/accounts/:id` accepting `{ included: boolean }` and updating the flag
- [x] 6.5 Add an `@RestControllerAdvice` that maps validation errors to 400 and unexpected errors to 500 with a sanitized body
- [x] 6.6 Add an integration test that boots the context, hits each endpoint with a stubbed Plaid client, and asserts the happy path

## 7. BFF (Hono) — scaffold

- [x] 7.1 Initialize `services/bff` with `package.json`, `tsconfig.json` (strict), Hono, `@hono/node-server`, `zod`, `dotenv`, `pino` (or built-in console)
- [x] 7.2 Add `dev` script (tsx watch) and `start` script (built JS), plus `typecheck`
- [x] 7.3 Read `BACKEND_URL` and `BFF_PORT` from env with sensible local defaults
- [x] 7.4 Add CORS middleware permitting the desktop dev origin

## 8. BFF (Hono) — routes

- [x] 8.1 Implement `POST /api/plaid/link-token` proxying to backend
- [x] 8.2 Implement `POST /api/plaid/exchange` with zod validation on `{ public_token: string, institution: object | null }`; forward to backend
- [x] 8.3 Implement `GET /api/accounts` proxying to backend
- [x] 8.4 Implement `PATCH /api/accounts/:id` with zod validation on `{ included: boolean }`; forward to backend
- [x] 8.5 Add request logging that omits request bodies for the `exchange` route (avoid logging `public_token`)
- [x] 8.6 Add a smoke test (vitest or node:test) that boots the BFF with a stubbed backend and asserts each route forwards correctly

## 9. Desktop app — Electron + Vite scaffold

- [x] 9.1 Initialize `apps/desktop` with an `electron-vite` setup (main + preload + renderer), `electron-builder` config, React 18, TypeScript, Vite
- [x] 9.2 Configure the main process to open one window pointing at the dev server (dev) or `dist/index.html` (prod)
- [x] 9.3 Keep the preload script minimal (no `window.api` surface needed in this change)
- [x] 9.4 Read `BFF_URL` from a renderer-side env (`VITE_BFF_URL`) with a localhost default
- [x] 9.5 Verify the Electron `session` does not strip cookies — needed for any Plaid OAuth institution to round-trip

## 10. Desktop app — Connect bank screen

- [x] 10.1 Build the screen layout per the Aurora visual system (background `var(--bg)`, primary card on `var(--surface)`, headings in Geist, accent button)
- [x] 10.2 On mount, `fetch(BFF_URL + '/api/plaid/link-token')` and store the resulting `link_token`
- [x] 10.3 Use `react-plaid-link`'s `usePlaidLink({ token, onSuccess })` to drive the flow
- [x] 10.4 On `onSuccess(publicToken, metadata)`, navigate to the Pick accounts screen with `{ publicToken, institution: metadata.institution }`
- [x] 10.5 Render an inline error + "Retry" affordance when the link-token fetch fails

## 11. Desktop app — Pick accounts screen

- [x] 11.1 On mount with `{ publicToken, institution }` from navigation state, `POST /api/plaid/exchange` and render returned accounts
- [x] 11.2 Render each account as a row: name (Geist), mask, type/subtype, current balance (Geist Mono), include/skip toggle
- [x] 11.3 Toggle state lives in local component state until Confirm; Confirm issues one `PATCH /api/accounts/:id` per account whose state diverges from the default
- [x] 11.4 Disable Confirm when zero accounts are included; show an explanatory hint when disabled
- [x] 11.5 On confirm success, close the onboarding flow (show a simple "All set." placeholder screen — the real dashboard lands in a later change)

## 12. Navigation and state plumbing

- [x] 12.1 Pick a tiny router for two screens — either two component states behind a `useState`, or `wouter`. Avoid `react-router` for two screens.
- [x] 12.2 Ensure the app reopens to the Pick accounts review state if at least one item is already linked, otherwise to Connect bank — drives off `GET /api/accounts`

## 13. Verification (unit/integration; no manual e2e)

- [x] 13.1 Run `pnpm typecheck` across TypeScript packages
- [x] 13.2 Run `pnpm lint` across TypeScript packages
- [x] 13.3 Run the BFF smoke/integration tests with a stubbed backend
- [x] 13.4 Build both TypeScript packages (`services/bff` and `apps/desktop`)

## 14. Documentation gaps to flag for follow-ups

- [x] 14.1 README: add a "Known limitations" section noting that `access_token` is stored unencrypted and is acceptable for Sandbox only
- [x] 14.2 README: link to this OpenSpec change so a new contributor can find the design rationale
