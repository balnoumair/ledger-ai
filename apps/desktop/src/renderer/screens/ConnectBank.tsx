import { useCallback, useEffect, useState } from 'react';
import { usePlaidLink } from 'react-plaid-link';
import type { PlaidLinkOnSuccessMetadata } from 'react-plaid-link';
import { createLinkToken } from '../lib/api';
import type { PlaidInstitutionMeta } from '../lib/api';

interface Props {
  onLinked: (publicToken: string, institution: PlaidInstitutionMeta | null) => void;
}

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; token: string }
  | { kind: 'error'; message: string };

export function ConnectBank({ onLinked }: Props) {
  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [nonce, setNonce] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setState({ kind: 'loading' });
    (async () => {
      try {
        const { link_token } = await createLinkToken();
        if (!cancelled) setState({ kind: 'ready', token: link_token });
      } catch (err) {
        if (!cancelled) {
          setState({
            kind: 'error',
            message:
              err instanceof Error
                ? `Couldn't reach the BFF (${err.message}).`
                : "Couldn't reach the BFF.",
          });
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [nonce]);

  const handleSuccess = useCallback(
    (publicToken: string, metadata: PlaidLinkOnSuccessMetadata) => {
      const inst = metadata.institution
        ? {
            name: metadata.institution.name ?? null,
            institution_id: metadata.institution.institution_id ?? null,
          }
        : null;
      onLinked(publicToken, inst);
    },
    [onLinked],
  );

  const token = state.kind === 'ready' ? state.token : null;
  const { open, ready } = usePlaidLink({
    token,
    onSuccess: handleSuccess,
  });

  return (
    <div className="card" role="region" aria-labelledby="connect-title">
      <span className="eyebrow">01 · Onboarding</span>
      <h1 id="connect-title" className="title">
        Connect your bank
      </h1>
      <p className="subtitle">
        We'll open Plaid's secure window so you can choose a bank. Ledger AI only sees the read-only
        account snapshot you approve — credentials stay with Plaid.
      </p>

      {state.kind === 'error' && (
        <div className="error-banner" role="alert">
          <span>{state.message}</span>
          <button type="button" onClick={() => setNonce((n) => n + 1)}>
            Retry
          </button>
        </div>
      )}

      <button
        type="button"
        className="btn-primary"
        disabled={state.kind !== 'ready' || !ready}
        onClick={() => open()}
      >
        {state.kind === 'loading'
          ? 'Preparing…'
          : state.kind === 'ready'
            ? 'Connect bank'
            : 'Try again'}
      </button>

      <p className="faint">
        Sandbox tip · use <code className="mono">user_good</code> / <code className="mono">pass_good</code>
      </p>
    </div>
  );
}
