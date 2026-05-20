## Requirements

### Requirement: Repository layout

The repository SHALL be organized as a pnpm workspace monorepo with three top-level packages:

- `apps/desktop` — Electron + React + TypeScript + Vite
- `services/bff` — Hono + TypeScript on Node.js
- `services/backend` — Spring Boot + Kotlin (Gradle wrapper checked in)

Plus the following supporting directories at the repo root:

- `infra/` — `docker-compose.yml` and any other local-infra files
- `openspec/` — change and spec documents (already present)
- `.github/` — placeholder, no workflows in this change

A root `package.json` SHALL declare the workspace and expose orchestrating scripts. A root `pnpm-workspace.yaml` SHALL enumerate the TS packages under `apps/*` and `services/*`. The Kotlin backend SHALL NOT be a pnpm workspace member; it is built via Gradle.

#### Scenario: Workspace install resolves all TS packages
- **WHEN** a developer runs `pnpm install` at the repo root on a fresh clone
- **THEN** pnpm resolves and links `apps/desktop` and `services/bff`, and no other directories are treated as workspace packages

#### Scenario: Backend builds independently of pnpm
- **WHEN** a developer runs `./gradlew build` inside `services/backend`
- **THEN** the Kotlin service compiles and tests run without requiring `pnpm install` to have been run first

### Requirement: Root developer scripts

The root `package.json` SHALL expose the following scripts, each delegating to the appropriate workspace or sub-process:

- `dev:db` — `docker compose -f infra/docker-compose.yml up -d postgres`
- `dev:backend` — runs the Kotlin backend in dev mode (via `./gradlew bootRun` in `services/backend`)
- `dev:bff` — runs the BFF in watch mode
- `dev:desktop` — runs the Electron + Vite dev server for the desktop app
- `dev` — convenience script that starts all four of the above (db, backend, bff, desktop), with the backend and BFF coming up before the desktop app

Script implementations MAY use a process orchestrator (e.g. `concurrently`) but the script names listed above SHALL exist and behave as described.

#### Scenario: A single command spins up the dev stack
- **WHEN** a developer runs `pnpm dev` at the repo root
- **THEN** Postgres is started in Docker, the backend on its port, the BFF on its port, and the Electron app launches against them

#### Scenario: Each tier can be run in isolation
- **WHEN** a developer runs `pnpm dev:bff` only
- **THEN** the BFF starts in watch mode without attempting to start the database, backend, or desktop app

### Requirement: Local Postgres via docker-compose

The file `infra/docker-compose.yml` SHALL define a `postgres` service using an official `postgres:16` image (or newer), with:

- A named volume for data persistence across `docker compose down`
- A fixed port mapping to `5432` on the host
- Default credentials suitable only for local development, sourced from `infra/.env` (committed `.env.example` SHALL document the variables)
- A `healthcheck` so dependent services can wait for readiness

No other services (no Redis, no message broker) SHALL be added in this change.

#### Scenario: Data survives a container restart
- **WHEN** the developer runs `docker compose down` and then `docker compose up -d postgres` again
- **THEN** previously written rows (e.g. the seeded `me` user) are still present

### Requirement: Aurora design tokens are shared, not duplicated

The Aurora visual system from the design handoff (deep navy `#0d1224`, card `#141a30`, Geist sans, Geist Mono, solid surfaces, no gradients) SHALL be expressed as a single source of truth — CSS custom properties declared on `:root` in a shared stylesheet imported by the desktop app's renderer entry point. Component-level styling SHALL reference these tokens rather than hard-coding hex values.

At minimum the following tokens SHALL exist:

- `--bg` (page background)
- `--surface` (card / panel background)
- `--surface-2` (one lift above `--surface`)
- `--border` (hairline border on cards / inputs)
- `--text` (primary text)
- `--text-muted` (secondary text)
- `--accent` (Aurora teal)
- `--accent-contrast` (text on accent surfaces)
- `--font-sans`, `--font-mono`

A light-mode override SHALL be applied when the `light` class is on the root element (matching the design handoff's Tweaks toggle), but no light-mode UI affordance is required to ship in this change — the desktop app may default to dark and never switch.

#### Scenario: Changing the accent in one place updates the app
- **WHEN** a developer edits the value of `--accent` in the shared tokens stylesheet
- **THEN** every component that uses the accent color picks up the change with no other edits

### Requirement: TypeScript and tooling baseline

Every TypeScript package (`apps/desktop`, `services/bff`) SHALL have:

- `strict: true` in its `tsconfig.json`
- An ESLint config and a Prettier config (these MAY be shared from the repo root)
- A `typecheck` script that runs `tsc --noEmit`

The Kotlin backend SHALL use Kotlin 2.x, JDK 21, Spring Boot 3.x, and SHALL have `ktlint` (or `spotless`) wired into `./gradlew check`.

#### Scenario: Typecheck catches a regression at the repo root
- **WHEN** a developer runs `pnpm -r typecheck` at the repo root
- **THEN** both TS packages are type-checked and the command fails if any package has a type error

### Requirement: Repo-root .gitignore

A repo-root `.gitignore` SHALL exclude at least: `node_modules/`, `dist/`, `build/`, `.env`, `.env.local`, `.gradle/`, `services/backend/build/`, `services/backend/.gradle/`, and Electron build outputs.

#### Scenario: Build outputs are not tracked
- **WHEN** a developer runs `pnpm -r build` and then `git status`
- **THEN** no build output paths appear as untracked or modified
