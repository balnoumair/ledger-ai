import { useCallback, useEffect, useState } from 'react';
import { ConnectBank } from './screens/ConnectBank';
import { PickAccounts } from './screens/PickAccounts';
import { Dashboard } from './screens/Dashboard';
import { listAccounts } from './lib/api';
import type { PlaidInstitutionMeta } from './lib/api';
import './styles/app.css';

type Route =
  | { name: 'loading' }
  | { name: 'connect' }
  | { name: 'pick'; publicToken: string; institution: PlaidInstitutionMeta | null }
  | { name: 'done' };

export function App() {
  const [route, setRoute] = useState<Route>({ name: 'loading' });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const accounts = await listAccounts();
        if (cancelled) return;
        // If anything is already linked, jump straight to the review state
        // so the user sees what they have and can toggle inclusion. Otherwise
        // start the connect flow.
        if (accounts.length === 0) {
          setRoute({ name: 'connect' });
        } else {
          setRoute({ name: 'done' });
        }
      } catch {
        if (cancelled) return;
        // BFF unreachable — start at Connect bank so the user at least sees an
        // actionable retry path there (Connect bank renders its own error).
        setRoute({ name: 'connect' });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const handleLinked = useCallback(
    (publicToken: string, institution: PlaidInstitutionMeta | null) => {
      setRoute({ name: 'pick', publicToken, institution });
    },
    [],
  );

  const handleConfirmed = useCallback(() => {
    setRoute({ name: 'done' });
  }, []);

  return (
    <div className="app-shell">
      {route.name === 'loading' && (
        <div className="centered-message">
          <p className="muted">Loading…</p>
        </div>
      )}
      {route.name === 'connect' && <ConnectBank onLinked={handleLinked} />}
      {route.name === 'pick' && (
        <PickAccounts
          publicToken={route.publicToken}
          institution={route.institution}
          onConfirmed={handleConfirmed}
        />
      )}
      {route.name === 'done' && <Dashboard onAddAnother={() => setRoute({ name: 'connect' })} />}
    </div>
  );
}
