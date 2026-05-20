import { useEffect, useState } from 'react';
import { listAccounts } from '../lib/api';
import type { Account } from '../lib/api';

interface Props {
  onAddAnother: () => void;
}

export function AllSet({ onAddAnother }: Props) {
  const [accounts, setAccounts] = useState<Account[] | null>(null);

  useEffect(() => {
    let cancelled = false;
    listAccounts()
      .then((a) => {
        if (!cancelled) setAccounts(a);
      })
      .catch(() => {
        if (!cancelled) setAccounts([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const included = accounts?.filter((a) => a.included) ?? [];

  return (
    <div className="card" role="region" aria-labelledby="done-title">
      <span className="eyebrow">All set</span>
      <h1 id="done-title" className="title">
        You're connected
      </h1>
      <p className="subtitle">
        {accounts === null
          ? 'Loading your accounts…'
          : included.length === 0
            ? "You've linked accounts but they're all marked as skipped. You can re-include any of them from settings (coming soon)."
            : `Ledger AI is tracking ${included.length} account${
                included.length === 1 ? '' : 's'
              }. The dashboard is a later change — for now, the data is in Postgres.`}
      </p>

      {accounts && accounts.length > 0 && (
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
                {a.current_balance == null
                  ? '—'
                  : a.current_balance.toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                <span className="balance-currency">{a.iso_currency_code ?? ''}</span>
              </div>
              <span className="sub">{a.included ? 'included' : 'skipped'}</span>
            </div>
          ))}
        </div>
      )}

      <div className="actions-row">
        <span className="hint">Want to add another institution?</span>
        <button type="button" className="btn-ghost" onClick={onAddAnother}>
          Connect another bank
        </button>
      </div>
    </div>
  );
}
