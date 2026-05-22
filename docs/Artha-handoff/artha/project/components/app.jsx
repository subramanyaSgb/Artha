// ─────────────────────────────────────────────────────────────
// App — router, state, side notes panel, tweaks
// ─────────────────────────────────────────────────────────────

const { useState, useEffect, useRef, useMemo } = React;

const DEFAULTS = /*EDITMODE-BEGIN*/{
  "accent": "teal",
  "showNotes": true
}/*EDITMODE-END*/;

function App() {
  const [tab, setTab] = useState('home');
  const [screen, setScreen] = useState(null);   // sub-screen pushed from More
  const [sheet, setSheet] = useState(null);     // bottom-sheet id
  const [notesTab, setNotesTab] = useState('overview');

  const tweaks = window.useTweaks ? window.useTweaks(DEFAULTS) : { state: DEFAULTS, setTweak: () => {} };

  const goBack = () => { setScreen(null); };
  const openSheet = (id) => setSheet(id);
  const closeSheet = () => setSheet(null);

  // ── Sub-screen renderer ─────────────────────────────────
  let content;
  if (screen) {
    const m = {
      investments: <ScreenInvestments onBack={goBack} onAdd={() => openSheet('add-invest')}/>,
      insurance:   <ScreenInsurance   onBack={goBack} onAdd={() => openSheet('add-insurance')}/>,
      goals:       <ScreenGoals       onBack={goBack} onAdd={() => openSheet('add-goal')}/>,
      budgets:     <ScreenBudgets     onBack={goBack} onAdd={() => openSheet('add-budget')}/>,
      subscriptions: <ScreenSubscriptions onBack={goBack} onAdd={() => openSheet('add-sub')}/>,
      recurring:   <ScreenRecurring   onBack={goBack} onAdd={() => openSheet('add-recurring')}/>,
      people:      <ScreenPeople      onBack={goBack} onAdd={() => openSheet('add-person')}/>,
      rules:       <ScreenRules       onBack={goBack} onAdd={() => openSheet('add-rule')}/>,
      reports:     <ScreenReports     onBack={goBack}/>,
      categories:  <ScreenCategories  onBack={goBack} onAdd={() => openSheet('add-category')}/>,
      tags:        <ScreenTags        onBack={goBack} onAdd={() => openSheet('add-tag')}/>,
      settings:    <ScreenSettings    onBack={goBack}/>,
      about:       <ScreenAbout       onBack={goBack}/>,
    };
    content = m[screen] || <div style={{ padding: 40 }}>{screen}</div>;
  } else if (tab === 'home') {
    content = <ScreenDashboard
      onTab={setTab}
      onAdd={() => openSheet('add-tx')}
      onAi={() => openSheet('ai-quick')}
      onTransactions={() => setTab('transactions')}
      onAccounts={() => setTab('accounts')}
    />;
  } else if (tab === 'transactions') {
    content = <ScreenTransactions user={SAMPLE.user} onAdd={() => openSheet('add-tx')} onAi={() => openSheet('ai-quick')}/>;
  } else if (tab === 'accounts') {
    content = <ScreenAccounts user={SAMPLE.user} onAdd={() => openSheet('add-account')}/>;
  } else if (tab === 'cards') {
    content = <ScreenCards user={SAMPLE.user} onAdd={() => openSheet('add-card')}/>;
  } else if (tab === 'more') {
    content = <ScreenMore user={SAMPLE.user} onNav={setScreen}/>;
  }

  return (
    <div id="stage">
      <PatternDefs/>
      <SideNotes tab={notesTab} onTab={setNotesTab}/>
      <PhoneShell overlays={<>
        <BottomTabs active={tab} onChange={(t) => { setScreen(null); setTab(t); }}/>
        {tab === 'home' && !sheet && !screen && (
          <FAB onClick={() => openSheet('add-tx')} icon="plus" label="Add" extended/>
        )}

        {/* Sheets */}
        <Sheet open={sheet === 'add-tx'} onClose={closeSheet} title="New transaction" tall>
          <AddTransactionSheet onClose={closeSheet}/>
        </Sheet>
        <Sheet open={sheet === 'ai-quick'} onClose={closeSheet}>
          <AIQuickEntry onClose={closeSheet}/>
        </Sheet>
        <Sheet open={sheet === 'add-account'} onClose={closeSheet} tall><AddAccountSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-card'} onClose={closeSheet} tall><AddCardSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-invest'} onClose={closeSheet} tall><AddInvestmentSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-insurance'} onClose={closeSheet} tall><AddInsuranceSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-goal'} onClose={closeSheet}><AddGoalSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-budget'} onClose={closeSheet}><AddBudgetSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-sub'} onClose={closeSheet}><AddSubscriptionSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-recurring'} onClose={closeSheet}><AddRecurringSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-person'} onClose={closeSheet}><AddPersonSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-rule'} onClose={closeSheet} tall><AddRuleSheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-category'} onClose={closeSheet} tall><AddCategorySheet onClose={closeSheet}/></Sheet>
        <Sheet open={sheet === 'add-tag'} onClose={closeSheet}><AddTagSheet onClose={closeSheet}/></Sheet>
      </>}>
        {content}
      </PhoneShell>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Side notes — design rationale + broken call-outs + try-these
   ─────────────────────────────────────────────────────────── */
function SideNotes({ tab, onTab }) {
  return (
    <aside className="meta-side">
      <h1><span className="deva-mark">अ</span> Artha</h1>
      <p style={{ color: 'var(--text-3)', fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 17, marginTop: -4 }}>Your money. Your rules.</p>
      <p style={{ marginTop: 14, color: 'var(--text-1)', fontWeight: 500 }}>Visual revamp — prototype</p>

      <div style={{ display: 'flex', gap: 4, marginTop: 14, padding: 4, background: 'var(--surface-2)', borderRadius: 12, border: '1px solid var(--line-1)' }}>
        {[
          ['overview', 'Overview'],
          ['system', 'System'],
          ['callouts', 'Call-outs'],
        ].map(([id, label]) => (
          <button key={id} onClick={() => onTab(id)} style={{
            flex: 1, height: 32, borderRadius: 9,
            background: tab === id ? 'var(--surface-4)' : 'transparent',
            color: tab === id ? 'var(--text-1)' : 'var(--text-3)',
            border: 'none', cursor: 'pointer', fontSize: 12, fontWeight: 500, fontFamily: 'var(--font-ui)',
          }}>{label}</button>
        ))}
      </div>

      {tab === 'overview' && <NotesOverview/>}
      {tab === 'system' && <NotesSystem/>}
      {tab === 'callouts' && <NotesCallouts/>}
    </aside>
  );
}

function NotesOverview() {
  return (
    <>
      <p style={{ marginTop: 14 }}>Dark-first system inspired by Indian rupee-note color cues, temple jaali geometry, and block-print micro-pattern. Numerals are editorial serif (display) and tabular mono (small) — both lining + tabular so columns of money align.</p>

      <div className="stack-buttons">
        <div className="group">Try these (in order)</div>
        <button>1 · Net Position hero with अ corner mark</button>
        <button>2 · Tap <strong style={{color:'var(--teal-300)'}}>Quick add with Gemini</strong></button>
        <button>3 · FAB → New transaction sheet</button>
        <button>4 · Tabs: Home · Ledger · Accounts · Cards · More</button>
        <button>5 · More → Investments / Goals / Reports / Rules</button>
        <button>6 · Cards tab → see the rupee-note credit cards</button>
      </div>

      <p className="note">
        The bottom tab labelled <strong style={{color:'var(--text-1)'}}>Ledger</strong> (instead of <em>Transactions</em>) so the label survives on small phones and reads instantly to a finance user. Easily reverted.
      </p>
    </>
  );
}

function NotesSystem() {
  return (
    <>
      <div className="stack-buttons" style={{ marginTop: 14 }}>
        <div className="group">Palette · rupee-note cues</div>
        <NoteSwatch c="var(--teal-700)" name="Brand teal — #0F766E" />
        <NoteSwatch c="var(--ochre)" name="Ochre — saffron-adjacent" />
        <NoteSwatch c="var(--indigo)" name="Rupee blue — softened" />
        <NoteSwatch c="var(--income)" name="Sage — income (never green-red)" />
        <NoteSwatch c="var(--expense)" name="Coral — expense (warm, not aggressive)" />

        <div className="group" style={{ marginTop: 14 }}>Type stack</div>
        <button style={{ fontFamily: 'var(--font-display)', fontSize: 22, padding: '12px 14px' }}>Instrument Serif · ₹4,82,610</button>
        <button style={{ fontFamily: 'var(--font-mono)', fontSize: 13, padding: '12px 14px' }}>IBM Plex Mono · ₹84,200 +2.6%</button>
        <button style={{ fontFamily: 'var(--font-deva)', fontSize: 18, padding: '12px 14px' }}>Tiro Devanagari · अर्थ · २,२,३ grouping</button>
        <button style={{ padding: '12px 14px' }}>Plus Jakarta Sans · UI</button>

        <div className="group" style={{ marginTop: 14 }}>Compose mapping</div>
        <div style={{ background: 'var(--surface-2)', border: '1px solid var(--line-1)', borderRadius: 12, padding: '12px 14px', fontSize: 12, color: 'var(--text-2)', lineHeight: 1.6 }}>
          <strong style={{color:'var(--text-1)'}}>colorScheme</strong> · build dark via <code>darkColorScheme()</code> + Material You dynamic seed (teal #0F766E).<br/>
          <strong style={{color:'var(--text-1)'}}>typography</strong> · <code>headlineLarge</code> = Instrument Serif (display num), <code>labelSmall</code> = caps eyebrow, <code>bodyMedium</code> = Plus Jakarta. Add a custom <code>numericLarge</code> token using <code>FontFeatureSetting("tnum","lnum")</code>.<br/>
          <strong style={{color:'var(--text-1)'}}>Components</strong> · <code>Card</code> (tonalElevation = 1) for surface-2, <code>FilledTonalChip</code> (M3) for the rupee-pill chips, <code>NavigationBar</code> + <code>NavigationBarItem</code> for the tabs, <code>ModalBottomSheet</code> for sheets, <code>OutlinedTextField</code> for inputs, <code>LinearProgressIndicator</code> for budget bars.
        </div>
      </div>
    </>
  );
}

function NoteSwatch({ c, name }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', background: 'var(--surface-2)', border: '1px solid var(--line-1)', borderRadius: 10 }}>
      <span style={{ width: 20, height: 20, borderRadius: 6, background: c, flexShrink: 0 }}/>
      <span style={{ fontSize: 12, color: 'var(--text-1)' }}>{name}</span>
    </div>
  );
}

function NotesCallouts() {
  const items = [
    { sev: 'visual', screen: 'Add Account / Add Card', text: 'Color & icon pickers had no preview and the swatches were raw HSL chips. New: outlined-when-selected swatches + a live AccountChip preview at the bottom of the Add sheet.' },
    { sev: 'visual', screen: 'Dashboard', text: 'The “across N accounts, 0 cards” line had no information density. New hero shows Liquid / Invested / Card-out as a 3-up under the headline number — same vertical real estate, far more useful.' },
    { sev: 'copy', screen: 'Subscriptions empty', text: '"No subscriptions yet. Add Netflix, Spotify, iCloud, etc." reads like ad copy. Suggest tightening to "Add what bills you monthly."' },
    { sev: 'a11y', screen: 'Hero (current build)', text: 'Solid #2A6FDB blue background + thin white text against a dark page is loud and gives a “demo card” feel. Replaced with surface-2 + a hairline teal border. Tonal hierarchy now comes from type, not block-fill colour.' },
    { sev: 'broken', screen: 'Settings · About', text: 'In screenshot 2026-05-22-09-49-25 the "About" header reappears twice (looks like a duplicated block from your Export-encrypted card layout). Easy to spot — likely a stray LazyColumn item.' },
    { sev: 'visual', screen: 'Reports (current)', text: '“No spending in this period” is hugged by an opaque grey pill that looks active. In dark theme it reads as a chip the user can tap. Lighten with a dotted border + lower alpha.' },
    { sev: 'copy', screen: 'Recurring · Add rule', text: 'The Phase 5 disclaimer is good but currently presented as body text; demote to an inline info-banner (yellow ochre border) so it isn’t mistaken for a description field.' },
    { sev: 'visual', screen: 'Categories list', text: 'Every system category has the same red-orange circle icon — visually they collapse into one stripe. Variation by category-color from the rupee palette restores scannability.' },
    { sev: 'copy', screen: 'Bottom nav', text: '"Transactions" label gets clipped on most phones (your screenshots show it as just the icon). Either rename to "Ledger" / "Activity" or drop the labels under 360dp width.' },
    { sev: 'visual', screen: 'New transaction', text: 'Amount entered as a generic OutlinedTextField gets lost. The new sheet shows it as a giant centered display-serif number so the user always knows what they’re saving.' },
  ];
  const sevMap = {
    visual: ['var(--ochre)', 'Visual'],
    copy: ['var(--acc-indigo)', 'Copy'],
    a11y: ['var(--acc-magenta)', 'A11y'],
    broken: ['var(--expense)', 'Broken'],
  };
  return (
    <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 10 }}>
      <p style={{ color: 'var(--text-3)' }}>Things I noticed in your screenshots — separate from the redesign, but worth fixing:</p>
      {items.map((it, i) => {
        const [c, label] = sevMap[it.sev];
        return (
          <div key={i} style={{ background: 'var(--surface-2)', border: '1px solid var(--line-1)', borderRadius: 12, padding: '12px 14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <span style={{ width: 8, height: 8, borderRadius: 4, background: c, flexShrink: 0 }}/>
              <span style={{ fontSize: 10, color: c, fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase' }}>{label}</span>
              <span style={{ fontSize: 10, color: 'var(--text-3)' }}>· {it.screen}</span>
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-2)', lineHeight: 1.5 }}>{it.text}</div>
          </div>
        );
      })}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
