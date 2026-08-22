alter table plaid_items
    add column if not exists transactions_cursor text null,
    add column if not exists last_synced_at      timestamptz null;

create table if not exists plaid_transactions (
    id                    uuid           primary key,
    account_id            uuid           not null references plaid_accounts (id) on delete cascade,
    plaid_transaction_id  text           not null unique,
    name                  text           not null,
    merchant_name         text           null,
    amount                numeric(19, 4) not null,
    iso_currency_code     text           null,
    date                  date           not null,
    pending               boolean        not null default false,
    category_primary      text           null,
    category_detailed     text           null,
    payment_channel       text           null,
    created_at            timestamptz    not null default now()
);

create index if not exists plaid_transactions_account_idx on plaid_transactions (account_id);
create index if not exists plaid_transactions_date_idx on plaid_transactions (date desc);
