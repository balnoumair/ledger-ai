import { useEffect, useMemo, useState } from 'react';
import { exchangePublicToken, updateAccountIncluded } from '../lib/api';
import type { Account, PlaidInstitutionMeta } from '../lib/api';

interface Props {
  publicToken: string;
  institution: PlaidInstitutionMeta | null;
  onConfirmed: () => void;
}

type LoadState =
  | { kind: 'exchanging' }
  | { kind: 'ready'; accounts: Account[] }
  | { kind: 'error'; message: string };

export function PickAccounts({ publicToken, institution, onConfirmed }: Props) {
  const [state, setState] = useState<LoadState>({ kind: 'exchanging' });
  // included state is keyed by account id; default = true (matches DB).
  const [included, setIncluded] = useState<Record<string, boolean>>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setState({ kind: 'exchanging' });
    (async () => {
      try {
        const accounts = await exchangePublicToken(publicToken, institution);
        if (cancelled) return;
        setState({ kind: 'ready', accounts });
        setIncluded(Object.fromEntries(accounts.map((a) => [a.id, a.included])));
      } catch (err) {
        if (cancelled) return;
        setState({
          kind: 'error',
          message:
            err instanceof Error ? err.message : 'Could not exchange the public token.',
        });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [publicToken, institution]);

  const includedCount = useMemo(
    () => Object.values(included).filter(Boolean).length,
    [included],
  );
  const canConfirm = includedCount > 0 && state.kind === 'ready' && !submitting;

  async function confirm() {
    if (state.kind !== 'ready') return;
    setSubmitError(null);
    setSubmitting(true);
    try {
      const toPatch = state.accounts.filter((a) => (included[a.id] ?? true) !== a.included);
      await Promise.all(
        toPatch.map((a) => updateAccountIncluded(a.id, included[a.id] ?? true)),
      );
      onConfirmed();
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : 'Failed to save your selection.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="card card-wide" role="region" aria-labelledby="pick-title">
      <span className="eyebrow">01 · Onboarding</span>
      <h1 id="pick-title" className="title">
        Pick accounts
      </h1>
      <p className="subtitle">
        {institution?.name
          ? `These are the accounts ${institution.name} returned. Toggle off anything you don't want Ledger AI to track.`
          : 'These are the accounts your institution returned. Toggle off anything you don\'t want Ledger AI to track.'}
      </p>

      {state.kind === 'exchanging' && <p className="muted">Fetching your accounts…</p>}

      {state.kind === 'error' && (
        <div className="error-banner" role="alert">
          <span>{state.message}</span>
        </div>
      )}

      {state.kind === 'ready' && (
        <div className="row-list">
          {state.accounts.map((a) => {
            const isOn = included[a.id] ?? true;
            return (
              <div key={a.id} className={`account-row${isOn ? '' : ' excluded'}`}>
                <div className="account-meta">
                  <div className="name">
                    {a.name}
                    {a.mask ? <span className="sub"> · ••{a.mask}</span> : null}
                  </div>
                  <div className="sub">
                    {a.type}
                    {a.subtype ? ` · ${a.subtype}` : ''}
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
                <button
                  type="button"
                  role="switch"
                  aria-checked={isOn}
                  aria-label={`${isOn ? 'Including' : 'Skipping'} ${a.name}`}
                  className={`toggle${isOn ? ' on' : ''}`}
                  onClick={() => setIncluded((prev) => ({ ...prev, [a.id]: !isOn }))}
                />
              </div>
            );
          })}
        </div>
      )}

      {submitError && (
        <div className="error-banner" role="alert">
          <span>{submitError}</span>
        </div>
      )}

      <div className="actions-row">
        <span className={`hint${includedCount === 0 ? ' hint-warning' : ''}`}>
          {state.kind === 'ready'
            ? includedCount === 0
              ? 'Include at least one account to continue.'
              : `${includedCount} of ${state.accounts.length} account${state.accounts.length === 1 ? '' : 's'} included`
            : ''}
        </span>
        <button type="button" className="btn-primary" disabled={!canConfirm} onClick={confirm}>
          {submitting ? 'Saving…' : 'Confirm'}
        </button>
      </div>
    </div>
  );
}
