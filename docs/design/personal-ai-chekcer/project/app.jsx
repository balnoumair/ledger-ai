// Main app — design canvas with onboarding / dashboard / AI bridge / settings.
// Single visual system (Aurora). Light/dark via Tweaks.

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "light": false
}/*EDITMODE-END*/;

function applyMode(light) {
  const root = document.documentElement;
  if (light) root.classList.add('light');
  else root.classList.remove('light');
}

function App() {
  const [t, setTweak] = useTweaks(TWEAK_DEFAULTS);
  React.useEffect(() => { applyMode(t.light); }, [t.light]);

  const W = 1440, H = 900;

  return (
    <>
      <DesignCanvas
        title="Personal AI Finance Checker"
        subtitle="Onboarding · dashboard · AI bridge · settings. Toggle light/dark from the Tweaks panel."
      >
        <DCSection id="onboarding" title="01 · Onboarding"
          subtitle="Plaid linking flow — read-only by default, AI-aware.">
          <DCArtboard id="plaid-1" label="Connect bank" width={W} height={H}>
            <WinChrome url="app.ledger.ai/connect"><div className="art"><PlaidConnect /></div></WinChrome>
          </DCArtboard>
          <DCArtboard id="plaid-2" label="Pick accounts" width={W} height={H}>
            <WinChrome url="app.ledger.ai/connect/accounts"><div className="art"><AccountSelect /></div></WinChrome>
          </DCArtboard>
        </DCSection>

        <DCSection id="dashboard" title="02 · Dashboard"
          subtitle="Post-setup home.">
          <DCArtboard id="dash-main" label="Overview" width={W} height={H}>
            <WinChrome><div className="art"><Dashboard /></div></WinChrome>
          </DCArtboard>
        </DCSection>

        <DCSection id="ai" title="03 · AI bridge"
          subtitle="Install the CLI, then set up a skill in Claude Code, Codex, or Cursor.">
          <DCArtboard id="bridge" label="Connect an agent" width={W} height={H}>
            <WinChrome url="app.ledger.ai/ai"><div className="art"><AiBridge /></div></WinChrome>
          </DCArtboard>
        </DCSection>

        <DCSection id="settings" title="04 · Settings"
          subtitle="Account, banks, connected agents, and your data.">
          <DCArtboard id="settings-1" label="Settings" width={W} height={H}>
            <WinChrome url="app.ledger.ai/settings"><div className="art"><Settings /></div></WinChrome>
          </DCArtboard>
        </DCSection>
      </DesignCanvas>

      <TweaksPanel title="Tweaks">
        <TweakSection label="Appearance" />
        <TweakToggle label="Light mode" value={t.light} onChange={v => setTweak('light', v)} />
      </TweaksPanel>
    </>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
