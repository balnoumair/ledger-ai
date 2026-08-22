# ledger-ai

A personal financial dashboard with a Plaid-linked desktop app and a CLI surface for coding agents.

This change (`bootstrap-plaid-onboarding`) delivers the onboarding slice end-to-end: a desktop app where you click "Connect bank", go through Plaid Sandbox, pick accounts, and see them land in Postgres.

## Repo layout

```
apps/desktop/        Electron + React + TypeScript + Vite
services/bff/        Hono + TypeScript on Node (thin pass-through)
services/backend/    Spring Boot + Kotlin (owns Plaid + Postgres)
infra/               docker-compose.yml for local Postgres
```

## Prerequisites

- **Node.js** ≥ 20.11 (this repo is tested on Node 25)
- **pnpm** 10.x (pinned via `packageManager` in `package.json`; install with `corepack enable && corepack prepare pnpm@10.26.2 --activate`)
- **Docker** (for the Postgres dev container)
- **JDK 21** (e.g. via `sdk install java 21-tem` or `brew install --cask temurin@21`) — required to build the Kotlin backend
- **Plaid Sandbox credentials** — sign up at https://dashboard.plaid.com/signup and grab `PLAID_CLIENT_ID` and `PLAID_SECRET`

## Environment files

Copy each example file and fill in the values you need:

```bash
cp infra/.env.example infra/.env
cp services/backend/.env.example services/backend/.env
cp services/bff/.env.example services/bff/.env
cp apps/desktop/.env.example apps/desktop/.env
```

The only one that needs real values for the link flow to work is `services/backend/.env` (your Plaid Sandbox `PLAID_CLIENT_ID` and `PLAID_SECRET`).

## Dev-stack startup

Shortcut — one-time setup, then start everything:

```bash
make setup   # generates the Gradle wrapper + copies every .env.example
# fill in Plaid Sandbox credentials in services/backend/.env, then:
make dev     # starts Postgres, backend, BFF, and desktop together
```

Or run it manually:

```bash
# 1. Install JS deps
pnpm install

# 2. Start the whole stack (Postgres in Docker + backend + BFF + desktop)
pnpm dev
```

This runs:

1. `pnpm dev:db` — `docker compose up -d postgres`
2. `pnpm dev:backend` — `./gradlew bootRun` inside `services/backend` (Spring Boot, port `8080`)
3. `pnpm dev:bff` — Hono in tsx watch mode (port `8787`)
4. `pnpm dev:desktop` — Electron + Vite dev server (port `5173`)

You can also run each tier in isolation: `pnpm dev:db`, `pnpm dev:backend`, `pnpm dev:bff`, `pnpm dev:desktop`.

## Plaid Sandbox flow

Once the stack is running, the desktop app opens to the **Connect bank** screen. Click "Connect bank", pick a Sandbox institution (e.g. **First Platypus Bank** for credentials-only, or **Tartan Bank** for an OAuth round-trip), and use the standard Plaid Sandbox credentials:

- Username: `user_good`
- Password: `pass_good`
- MFA (if asked): `1234`

After approving, the **Pick accounts** screen lists the institution's accounts. Toggle any you want to skip, then click "Confirm". The included flag is persisted on `plaid_accounts.included`.

## Useful commands

```bash
pnpm typecheck                 # tsc --noEmit across both TS packages
pnpm lint                      # ESLint across both TS packages
pnpm format                    # Prettier write
cd services/backend && ./gradlew check   # Kotlin: ktlint via spotless + tests
```

## Known limitations

- **No auth.** A single seeded `me` user (UUID `00000000-0000-0000-0000-000000000001`) owns everything. Real auth is a later change.
- **`access_token` is stored unencrypted** in `plaid_items.access_token`. This is acceptable for Sandbox only; a later change adds at-rest encryption (likely pgcrypto or Spring Jasypt). **Do not point this at `PLAID_ENV=development` or `production` as-is.**
- **No transaction sync.** This change persists `Item` and `Account` rows only.
- **No CI, no production build.** Dev ergonomics only.
- **Plaid Link in Electron** relies on the default Electron `session` cookies. If you've customized `session.defaultSession` to strip cookies, the OAuth flow (Tartan Bank etc.) will loop.

## Design rationale

The Aurora visual system the desktop app uses comes from [`docs/design/personal-ai-chekcer/`](docs/design/personal-ai-chekcer/).
