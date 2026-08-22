import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  getSummary,
  listAccounts,
  listTransactions,
  refreshBalances,
  syncTransactions,
  updateAccountIncluded,
} from '../lib/api';
import type { Account, Summary, Transaction } from '../lib/api';

interface Props {
  onAddAnother: () => void;
}

function fmtMoney(value: number, currency: string | null): string {
  try {
    return value.toLocaleString(undefined, {
      style: 'currency',
      currency: currency ?? 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  } catch {
    return value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}

function prettyCategory(raw: string | null): string {
  if (!raw) return 'Other';
  const lower = raw.replaceAll('_', ' ').toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}

function fmtDate(iso: string): string {
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

export function Dashboard({ onAddAnother }: Props) {
  const [summary, setSummary] = useState<Summary | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadAll = useCallback(async () => {
    const [s, a, t] = await Promise.all([getSummary(), listAccounts(), listTransactions(30)]);
    setSummary(s);
    setAccounts(a);
    setTransactions(t);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        await loadAll();
      } catch {
        if (!cancelled) setError('Could not load your dashboard. Is the backend running?');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [loadAll]);

  async function handleRefresh() {
    setError(null);
    setNotice(null);
    setRefreshing(true);
    try {
      await refreshBalances();
      await loadAll();
      setNotice('Balances refreshed from your bank.');
    } catch {
      setError('Balance refresh failed. Try again in a moment.');
    } finally {
      setRefreshing(false);
    }
  }

  async function handleSync() {
    setError(null);
    setNotice(null);
    setSyncing(true);
    try {
      const result = await syncTransactions();
      await loadAll();
      setNotice(
        result.added + result.modified + result.removed === 0
          ? 'Transactions are already up to date.'
          : `Synced: ${result.added} new, ${result.modified} updated, ${result.removed} removed.`,
      );
    } catch {
      setError('Transaction sync failed. Try again in a moment.');
    } finally {
      setSyncing(false);
    }
  }

  async function handleToggleIncluded(account: Account) {
    try {
      await updateAccountIncluded(account.id, !account.included);
      await loadAll();
    } catch {
      setError('Could not update the account.');
    }
  }

  const currency = accounts.find((a) => a.iso_currency_code)?.iso_currency_code ?? 'USD';
  const maxCategory = useMemo(
    () => Math.max(...(summary?.spending_by_category.map((c) => c.amount) ?? [0]), 0),
    [summary],
  );

  if (loading) {
    return (
      <div className="centered-message">
        <p className="muted">Loading your dashboard…</p>
      </div>
    );
  }

  return (
    <div className="card card-dashboard" role="region" aria-labelledby="dash-title">
      <div className="dash-header">
        <div>
          <span className="eyebrow">Dashboard</span>
          <h1 id="dash-title" className="title">
            {summary ? fmtMoney(summary.net_worth, currency) : '—'}
          </h1>
          <p className="subtitle">
            Net worth across {summary?.account_count ?? 0} tracked account
            {(summary?.account_count ?? 0) === 1 ? '' : 's'}
          </p>
        </div>
        <div className="dash-actions">
          <button type="button" className="btn-ghost" onClick={onAddAnother}>
            Connect another bank
          </button>
          <button
            type="button"
            className="btn-ghost"
            disabled={refreshing}
            onClick={handleRefresh}
          >
            {refreshing ? 'Refreshing…' : 'Refresh balances'}
          </button>
          <button type="button" className="btn-primary" disabled={syncing} onClick={handleSync}>
            {syncing ? 'Syncing…' : 'Sync transactions'}
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner" role="alert">
          <span>{error}</span>
        </div>
      )}
      {notice && <p className="muted">{notice}</p>}

      {summary && (
        <div className="stat-grid">
          <div className="stat">
            <span className="stat-label">Assets</span>
            <span className="stat-value">{fmtMoney(summary.total_assets, currency)}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Liabilities</span>
            <span className="stat-value">{fmtMoney(summary.total_liabilities, currency)}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Spent this month</span>
            <span className="stat-value stat-out">{fmtMoney(summary.month_spending, currency)}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Income this month</span>
            <span className="stat-value stat-in">{fmtMoney(summary.month_income, currency)}</span>
          </div>
        </div>
      )}

      {summary && summary.spending_by_category.length > 0 && (
        <section className="dash-section">
          <h2 className="section-title">Spending by category · {summary.month}</h2>
          <div className="category-list">
            {summary.spending_by_category.map((c) => (
              <div key={c.category} className="category-row">
                <span className="category-name">{prettyCategory(c.category)}</span>
                <div className="category-bar-track">
                  <div
                    className="category-bar"
                    style={{ width: `${maxCategory > 0 ? (c.amount / maxCategory) * 100 : 0}%` }}
                  />
                </div>
                <span className="category-amount">{fmtMoney(c.amount, currency)}</span>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="dash-section">
        <h2 className="section-title">Accounts</h2>
        <div className="row-list">
          {accounts.map((a) => (
            <div key={a.id} className={`account-row${a.included ? '' : ' excluded'}`}>
              <div className="account-meta">
                <div className="name">
                  {a.institution_name} · {a.name}
                </div>
                <div className="sub">
                  {a.type}
                  {a.subtype ? ` · ${a.subtype}` : ''}
                  {a.mask ? ` · ••${a.mask}` : ''}
                </div>
              </div>
              <div className="balance">
                {a.current_balance == null ? '—' : fmtMoney(a.current_balance, a.iso_currency_code)}
              </div>
              <button
                type="button"
                role="switch"
                aria-checked={a.included}
                aria-label={`${a.included ? 'Including' : 'Skipping'} ${a.name}`}
                className={`toggle${a.included ? ' on' : ''}`}
                onClick={() => handleToggleIncluded(a)}
              />
            </div>
          ))}
        </div>
      </section>

      <section className="dash-section">
        <h2 className="section-title">Recent transactions</h2>
        {transactions.length === 0 ? (
          <p className="muted">
            No transactions yet. Hit “Sync transactions” to pull them from your bank.
          </p>
        ) : (
          <div className="row-list">
            {transactions.map((t) => (
              <div key={t.id} className="tx-row">
                <div className="tx-meta">
                  <div className="name">
                    {t.merchant_name ?? t.name}
                    {t.pending ? <span className="tx-pending"> · pending</span> : null}
                  </div>
                  <div className="sub">
                    {fmtDate(t.date)} · {t.account_name} · {prettyCategory(t.category)}
                  </div>
                </div>
                <div className={`balance ${t.amount > 0 ? 'tx-out' : 'tx-in'}`}>
                  {t.amount > 0 ? '−' : '+'}
                  {fmtMoney(Math.abs(t.amount), t.iso_currency_code)}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
