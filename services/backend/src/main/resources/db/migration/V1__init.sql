create table if not exists users (
    id          uuid primary key,
    email       text        not null unique,
    created_at  timestamptz not null default now()
);

create table if not exists plaid_items (
    id                uuid        primary key,
    item_id           text        not null unique,
    access_token      text        not null,
    institution_name  text        not null,
    institution_id    text        null,
    user_id           uuid        not null references users (id) on delete cascade,
    created_at        timestamptz not null default now()
);

create index if not exists plaid_items_user_idx on plaid_items (user_id);

create table if not exists plaid_accounts (
    id                  uuid           primary key,
    item_id             uuid           not null references plaid_items (id) on delete cascade,
    plaid_account_id    text           not null unique,
    name                text           not null,
    official_name       text           null,
    mask                text           null,
    type                text           not null,
    subtype             text           null,
    current_balance     numeric(19, 4) null,
    available_balance   numeric(19, 4) null,
    iso_currency_code   text           null,
    included            boolean        not null default true,
    created_at          timestamptz    not null default now()
);

create index if not exists plaid_accounts_item_idx on plaid_accounts (item_id);
