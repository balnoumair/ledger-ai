## Requirements

### Requirement: Plaid credentials are isolated to the backend

The Plaid `client_id` and `secret` SHALL only ever be read by the Kotlin backend. The BFF and the desktop app SHALL NOT have access to these credentials, and the desktop app SHALL NOT make any direct network call to Plaid's API.

Plaid credentials SHALL be loaded by the backend from environment variables (`PLAID_CLIENT_ID`, `PLAID_SECRET`, `PLAID_ENV`), with `PLAID_ENV` defaulting to `sandbox` in this change. A committed `.env.example` SHALL document the required variables; no `.env` file SHALL be committed.

#### Scenario: BFF source tree contains no Plaid credentials reference
- **WHEN** searching the `services/bff/` source for `PLAID_CLIENT_ID` or `PLAID_SECRET`
- **THEN** no matches are found

#### Scenario: Desktop source tree contains no Plaid credentials reference
- **WHEN** searching the `apps/desktop/` source for `PLAID_CLIENT_ID` or `PLAID_SECRET`
- **THEN** no matches are found

### Requirement: BFF is a thin pass-through to the backend

The BFF SHALL expose the following HTTP endpoints, each of which delegates to a corresponding backend endpoint and SHALL NOT contain Plaid-specific business logic:

- `POST /api/plaid/link-token` → backend creates a Plaid Link token and returns it
- `POST /api/plaid/exchange` with body `{ public_token, institution }` → backend exchanges the public token, stores the resulting `Item`, fetches the item's accounts, persists them, and returns the persisted accounts
- `GET /api/accounts` → backend returns all accounts currently linked for the local user

The BFF MAY add request logging, request validation, and CORS handling, but SHALL NOT call the Plaid SDK directly and SHALL NOT persist data of its own.

#### Scenario: BFF forwards link-token request
- **WHEN** the desktop app calls `POST /api/plaid/link-token` on the BFF
- **THEN** the BFF calls the backend's corresponding internal endpoint and returns the backend's response body unchanged (apart from envelope shape, if any)

#### Scenario: BFF rejects malformed exchange payload
- **WHEN** the desktop app calls `POST /api/plaid/exchange` with a body missing `public_token`
- **THEN** the BFF responds `400 Bad Request` without calling the backend

### Requirement: Backend creates a Plaid Link token

The backend SHALL expose an endpoint that creates a Plaid Link token configured for the Sandbox environment, with at minimum:

- `products: ["transactions"]` (we don't fetch transactions in this change, but the token is provisioned so future changes can extend the flow without re-linking)
- `country_codes: ["US"]`
- `language: "en"`
- A stable `client_user_id` derived from the seeded local `me` user

The endpoint SHALL return the `link_token` and its `expiration` to the caller.

#### Scenario: Link token can be created in sandbox
- **WHEN** the desktop app's Connect Bank screen mounts and requests a link token via the BFF
- **THEN** within a few seconds the desktop app receives a non-empty `link_token` string usable to launch Plaid Link in Sandbox mode

#### Scenario: Sandbox env is the default
- **GIVEN** no `PLAID_ENV` is set
- **WHEN** the backend starts and creates a link token
- **THEN** the token is created against Plaid Sandbox, not Development or Production

### Requirement: Backend exchanges public token and persists Item + Accounts

The backend SHALL expose an endpoint that accepts a `public_token` (plus optional Plaid `institution` metadata returned by Link) and SHALL:

1. Exchange the `public_token` for an `access_token` and `item_id` via the Plaid `/item/public_token/exchange` call.
2. Persist a row in the `plaid_items` table with at least: `item_id`, `access_token`, `institution_name`, `institution_id` (nullable), `user_id` (the local `me` user), `created_at`.
3. Call Plaid `/accounts/get` for the new item, persist each returned account in the `plaid_accounts` table with at least: `account_id`, `item_id` (FK), `name`, `official_name` (nullable), `mask`, `type`, `subtype`, `current_balance`, `available_balance` (nullable), `iso_currency_code`, `created_at`.
4. Return the persisted accounts in the response so the desktop app can render the Pick-accounts screen without an additional round-trip.

The `access_token` value SHALL NEVER appear in any response body returned by the backend or BFF.

#### Scenario: Sandbox public token completes a full link
- **GIVEN** the user has just completed Plaid Link in Sandbox mode and the desktop app holds a `public_token`
- **WHEN** the desktop app posts `{ public_token, institution }` to `POST /api/plaid/exchange`
- **THEN** the response contains a list of accounts for that item, and a row exists in `plaid_items` plus one row per returned account in `plaid_accounts`

#### Scenario: Access token never leaves the backend
- **WHEN** inspecting any HTTP response sent by the BFF or backend's external endpoints
- **THEN** no field named or containing `access_token` is present

### Requirement: Backend serves the user's linked accounts

The backend SHALL expose an endpoint that returns all accounts currently linked for the local `me` user, joined with their owning institution name. The response SHALL include for each account: `id` (internal UUID), `plaid_account_id`, `name`, `mask`, `type`, `subtype`, `current_balance`, `iso_currency_code`, and `institution_name`. The `access_token` and `item_id` from Plaid SHALL NOT be included.

#### Scenario: Listing accounts after a successful link
- **GIVEN** the user has linked one Sandbox institution with three accounts
- **WHEN** the desktop app calls `GET /api/accounts` via the BFF
- **THEN** the response contains exactly those three accounts, each with the fields listed above

#### Scenario: Empty list before any link
- **GIVEN** no Plaid items have been linked yet
- **WHEN** the desktop app calls `GET /api/accounts`
- **THEN** the response is an empty array, not an error

### Requirement: Desktop app — Connect bank screen

The desktop app SHALL render a "Connect bank" screen that matches the Aurora visual system and:

1. On mount, requests a Plaid Link token from the BFF.
2. Renders a primary action that, when clicked, opens Plaid Link in the embedded browser context using the `react-plaid-link` library (or equivalent).
3. On successful link, collects the `public_token` and Plaid-returned institution metadata and navigates the user to the Pick-accounts screen, passing the data through.
4. Displays an inline error state (within the same screen, no modals) if the link token request fails or Plaid Link returns an error.

This screen SHALL NOT call Plaid directly; all Plaid interaction goes through the BFF → backend.

#### Scenario: Happy path from Connect to Pick accounts
- **GIVEN** the BFF and backend are running and Plaid Sandbox credentials are configured
- **WHEN** the user clicks the primary action on Connect bank, completes the Plaid Sandbox flow with a test institution, and approves the link
- **THEN** the desktop app transitions to the Pick accounts screen with the institution's accounts visible

#### Scenario: Link-token failure surfaces a retryable error
- **GIVEN** the BFF is unreachable
- **WHEN** the Connect bank screen mounts
- **THEN** the screen displays an error message and a "Retry" affordance, and does not crash

### Requirement: Desktop app — Pick accounts screen

After a successful exchange, the desktop app SHALL render a "Pick accounts" screen that:

1. Shows each account returned by the backend with its name, mask, type/subtype, and current balance, formatted using Geist Mono for numeric values.
2. Allows the user to toggle each account between "include" and "skip". By default all accounts are included.
3. Shows a primary "Confirm" action that closes the onboarding flow once at least one account is included.
4. Persists the user's include/skip selection locally for now (e.g. setting an `included` flag on each `plaid_account` row via a `PATCH /api/accounts/:id` endpoint added in this change). Excluded accounts SHALL remain in the database but be flagged.

#### Scenario: Toggling an account persists across reload
- **GIVEN** the Pick accounts screen is showing three accounts, all included
- **WHEN** the user toggles one to "skip" and clicks Confirm, then reopens the app
- **THEN** the skipped account is still flagged as excluded in `GET /api/accounts`

#### Scenario: Confirm is disabled when nothing is included
- **WHEN** the user toggles all accounts to "skip"
- **THEN** the Confirm action is disabled and a hint explains why
