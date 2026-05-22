// ─────────────────────────────────────────────────────────────
// Secondary screens (batch 2)
// Subscriptions · Recurring · People · Rules · Reports · Categories · Tags · Settings · About
// ─────────────────────────────────────────────────────────────

/* ── Subscriptions ─────────────────────────────────────────── */
const SAMPLE_SUBS = [
  { id: 's1', name: 'Spotify Family', provider: 'Spotify', amt: 179, freq: 'monthly', next: '03 Jun', status: 'active', glyph: 'play', color: 'var(--income)' },
  { id: 's2', name: 'iCloud 200 GB', provider: 'Apple', amt: 75, freq: 'monthly', next: '11 Jun', status: 'active', glyph: 'image', color: 'var(--text-2)' },
  { id: 's3', name: 'Netflix Standard', provider: 'Netflix', amt: 499, freq: 'monthly', next: '15 Jun', status: 'active', glyph: 'play', color: 'var(--expense)' },
  { id: 's4', name: 'NYT Crossword', provider: 'NYT', amt: 1400, freq: 'yearly', next: '02 Nov', status: 'active', glyph: 'edit', color: 'var(--acc-indigo)' },
  { id: 's5', name: 'Notion Plus', provider: 'Notion', amt: 850, freq: 'monthly', next: '—', status: 'paused', glyph: 'edit', color: 'var(--text-3)' },
];

function ScreenSubscriptions({ onBack, onAdd, items = SAMPLE_SUBS }) {
  const active = items.filter(i => i.status === 'active');
  const monthly = active.reduce((a, x) => a + (x.freq === 'monthly' ? x.amt : x.amt / 12), 0);
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Subscriptions" onBack={onBack}/>
      <div style={{ margin: '6px 16px 14px', padding: '16px 18px', background: 'var(--surface-2)', border: '1px solid var(--line-1)', borderRadius: 16 }}>
        <div className="eyebrow">Bleeding out monthly</div>
        <div className="num-display tnum" style={{ fontSize: 36, lineHeight: 1, marginTop: 4, color: 'var(--text-1)', fontWeight: 300, letterSpacing: '-0.02em' }}>{fmtINR(Math.round(monthly))}</div>
        <div style={{ marginTop: 6, fontSize: 12, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">
          {active.length} active · ₹{Math.round(monthly * 12).toLocaleString('en-IN')} per year
        </div>
      </div>

      <div style={{ padding: '0 16px' }}>
        {items.map(s => <SubscriptionRow key={s.id} s={s}/>)}
        <AddDashed label="Add subscription" onClick={onAdd}/>
      </div>
    </div>
  );
}

function SubscriptionRow({ s }) {
  const paused = s.status === 'paused';
  return (
    <div className="card" style={{
      padding: '12px 14px', marginBottom: 10,
      display: 'flex', alignItems: 'center', gap: 12,
      opacity: paused ? 0.6 : 1,
    }}>
      <div style={{
        width: 38, height: 38, borderRadius: 11,
        background: `color-mix(in srgb, ${s.color} 25%, var(--surface-3))`,
        border: `1px solid color-mix(in srgb, ${s.color} 40%, transparent)`,
        display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
      }}>
        <Icon name={s.glyph} size={16} color={s.color} weight={1.7}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)' }}>{s.name}</div>
          {paused && <span style={{ fontSize: 9, padding: '2px 6px', borderRadius: 4, background: 'var(--surface-4)', color: 'var(--text-3)', letterSpacing: '0.06em', fontWeight: 700 }}>PAUSED</span>}
        </div>
        <div style={{ marginTop: 2, fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">
          {paused ? 'No upcoming charges' : `Next ${s.next} · ${s.freq}`}
        </div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <div className="num-display tnum" style={{ fontSize: 17, color: 'var(--text-1)', letterSpacing: '-0.01em' }}>{fmtINR(s.amt)}</div>
        <div style={{ fontSize: 10, color: 'var(--text-3)' }}>{s.freq === 'monthly' ? '/mo' : '/yr'}</div>
      </div>
    </div>
  );
}

/* ── Recurring rules ───────────────────────────────────────── */
const SAMPLE_RECURRING = [
  { id: 'r1', name: 'Rent', template: 'Rent — Bangalore flat', freq: 'monthly', day: 1, amount: 38000, autoConfirm: true },
  { id: 'r2', name: 'Nifty 50 SIP', template: 'Zerodha — Nifty 50 SIP', freq: 'monthly', day: 5, amount: 10000, autoConfirm: true },
  { id: 'r3', name: 'Electricity', template: 'BESCOM electricity', freq: 'monthly', day: 18, amount: null, autoConfirm: false },
  { id: 'r4', name: 'PPF top-up', template: 'PPF — annual contribution', freq: 'yearly', day: 31, amount: 150000, autoConfirm: false },
];

function ScreenRecurring({ onBack, onAdd, items = SAMPLE_RECURRING }) {
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Recurring" onBack={onBack}/>
      <div style={{ padding: '0 16px 16px' }}>
        <div style={{
          margin: '0 0 14px', padding: '12px 14px',
          background: 'color-mix(in srgb, var(--ochre) 10%, transparent)',
          border: '1px solid color-mix(in srgb, var(--ochre) 30%, transparent)',
          borderRadius: 12, display: 'flex', gap: 10, alignItems: 'flex-start',
        }}>
          <Icon name="info" size={16} color="var(--ochre)" weight={1.6}/>
          <div style={{ flex: 1, fontSize: 12, color: 'var(--text-2)', lineHeight: 1.5 }}>
            Rules stored locally. Auto-firing on schedule turns on with WorkManager in Phase 5 — you'll still see entries here as reminders.
          </div>
        </div>

        {items.map(r => (
          <div key={r.id} className="card" style={{ padding: 14, marginBottom: 10 }}>
            <div className="row between" style={{ marginBottom: 6 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Icon name="refresh" size={16} color="var(--teal-300)" weight={1.7}/>
                <div style={{ fontSize: 14.5, fontWeight: 600, color: 'var(--text-1)' }}>{r.name}</div>
              </div>
              {r.autoConfirm && (
                <span style={{ fontSize: 9, padding: '2px 8px', borderRadius: 4, background: 'var(--teal-900)', color: 'var(--teal-300)', letterSpacing: '0.06em', fontWeight: 700 }}>AUTO</span>
              )}
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-2)' }}>{r.template}</div>
            <div className="row between" style={{ marginTop: 8 }}>
              <div className="num-mono tnum" style={{ fontSize: 11, color: 'var(--text-3)' }}>
                {r.freq} · day {r.day}
              </div>
              {r.amount && (
                <div className="tnum" style={{ fontSize: 13, color: 'var(--text-1)', fontFamily: 'var(--font-ui)', fontWeight: 500 }}>{fmtINR(r.amount)}</div>
              )}
            </div>
          </div>
        ))}

        <AddDashed label="New recurring rule" onClick={onAdd}/>
      </div>
    </div>
  );
}

/* ── People ─────────────────────────────────────────────── */
const SAMPLE_PEOPLE = [
  { id: 'p1', name: 'Rahul', relation: 'Friend', net: -1240, last: 'Tibetan Spoon, 21 May' },
  { id: 'p2', name: 'Priya', relation: 'Sibling', net: 3500, last: 'Mumbai trip share, 14 May' },
  { id: 'p3', name: 'Mom', relation: 'Parent', net: 0, last: 'Diwali gifts, 02 Nov 2025' },
  { id: 'p4', name: 'Kiran', relation: 'Colleague', net: -680, last: 'Lunch, 18 May' },
];

function ScreenPeople({ onBack, onAdd, items = SAMPLE_PEOPLE }) {
  const owedToMe = items.filter(p => p.net > 0).reduce((a, p) => a + p.net, 0);
  const iOwe = items.filter(p => p.net < 0).reduce((a, p) => a + Math.abs(p.net), 0);
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="People" onBack={onBack}/>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, padding: '6px 16px 14px' }}>
        <div className="card" style={{ padding: 14 }}>
          <div className="eyebrow">Owed to you</div>
          <div className="num-display tnum" style={{ fontSize: 24, marginTop: 4, color: 'var(--income)', letterSpacing: '-0.01em' }}>{fmtINR(owedToMe)}</div>
        </div>
        <div className="card" style={{ padding: 14 }}>
          <div className="eyebrow">You owe</div>
          <div className="num-display tnum" style={{ fontSize: 24, marginTop: 4, color: 'var(--expense)', letterSpacing: '-0.01em' }}>{fmtINR(iOwe)}</div>
        </div>
      </div>

      <div style={{ padding: '0 16px' }}>
        {items.map(p => {
          const owesMe = p.net > 0;
          const settled = p.net === 0;
          return (
            <div key={p.id} className="card" style={{ padding: '12px 14px', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{
                width: 40, height: 40, borderRadius: 20,
                background: `linear-gradient(135deg, var(--acc-${['teal','indigo','saffron','violet','magenta'][p.id.charCodeAt(1) % 5]}), color-mix(in srgb, var(--surface-3) 80%, #000))`,
                color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 16, fontWeight: 600, flexShrink: 0, fontFamily: 'var(--font-ui)',
              }}>{p.name[0]}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)' }}>{p.name}</div>
                  <span style={{ fontSize: 10, padding: '2px 6px', borderRadius: 4, background: 'var(--surface-4)', color: 'var(--text-3)', letterSpacing: '0.04em', fontWeight: 600 }}>
                    {p.relation}
                  </span>
                </div>
                <div style={{ marginTop: 2, fontSize: 11, color: 'var(--text-3)' }}>Last · {p.last}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div className="num-display tnum" style={{
                  fontSize: 18,
                  color: settled ? 'var(--text-3)' : owesMe ? 'var(--income)' : 'var(--expense)',
                  letterSpacing: '-0.01em',
                }}>{settled ? '—' : fmtINR(Math.abs(p.net))}</div>
                <div style={{ fontSize: 10, color: 'var(--text-3)' }}>
                  {settled ? 'settled' : owesMe ? 'owes you' : 'you owe'}
                </div>
              </div>
            </div>
          );
        })}
        <AddDashed label="Add person" onClick={onAdd}/>
      </div>
    </div>
  );
}

/* ── Rules engine ──────────────────────────────────────── */
const SAMPLE_RULES = [
  { id: 'r1', name: 'Swiggy → Food & Drink', priority: 20, when: 'description contains "swiggy"', then: 'set category Food & Drink, tag #dinner', enabled: true, seeded: true },
  { id: 'r2', name: 'BESCOM → Bills', priority: 30, when: 'description contains "bescom"', then: 'set category Bills & Utilities', enabled: true, seeded: true },
  { id: 'r3', name: 'Zerodha → Investment', priority: 40, when: 'description contains "zerodha"', then: 'set type Investment, tag #sip', enabled: true, seeded: true },
  { id: 'r4', name: 'My custom — Tokai', priority: 100, when: 'description contains "tokai"', then: 'set category Food & Drink, tag #coffee', enabled: true, seeded: false },
  { id: 'r5', name: 'Auto-confirm small UPI', priority: 110, when: 'amount < ₹200 and app = GPay', then: 'auto-confirm', enabled: false, seeded: false },
];

function ScreenRules({ onBack, onAdd, items = SAMPLE_RULES }) {
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Rules" onBack={onBack}/>

      <div style={{ padding: '4px 20px 12px' }}>
        <div style={{ fontSize: 13, color: 'var(--text-2)', lineHeight: 1.5 }}>
          Run in priority order on every new transaction. Lower number runs first. Seeded rules use 10–90.
        </div>
      </div>

      <div style={{ padding: '0 16px' }}>
        {items.map(r => (
          <div key={r.id} className="card" style={{ padding: 14, marginBottom: 10, opacity: r.enabled ? 1 : 0.55 }}>
            <div className="row between" style={{ marginBottom: 4 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{
                  width: 24, height: 24, borderRadius: 6,
                  background: r.seeded ? 'var(--surface-4)' : 'var(--teal-900)',
                  color: r.seeded ? 'var(--text-2)' : 'var(--teal-300)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 10, fontFamily: 'var(--font-mono)', fontWeight: 600,
                }}>{r.priority}</div>
                <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)' }}>{r.name}</div>
              </div>
              <Toggle checked={r.enabled}/>
            </div>
            <div style={{ marginLeft: 32, marginTop: 6 }}>
              <RuleClause kind="when" text={r.when}/>
              <RuleClause kind="then" text={r.then}/>
            </div>
          </div>
        ))}
        <AddDashed label="New rule" onClick={onAdd}/>
      </div>
    </div>
  );
}

function RuleClause({ kind, text }) {
  return (
    <div style={{ display: 'flex', gap: 8, marginTop: 4, fontSize: 12 }}>
      <span style={{
        flexShrink: 0,
        width: 38, padding: '2px 0', textAlign: 'center',
        borderRadius: 4,
        background: kind === 'when' ? 'var(--surface-3)' : 'var(--teal-900)',
        color: kind === 'when' ? 'var(--text-3)' : 'var(--teal-300)',
        fontSize: 10, fontFamily: 'var(--font-mono)', fontWeight: 600, letterSpacing: '0.08em',
        textTransform: 'uppercase',
      }}>{kind}</span>
      <span style={{ color: 'var(--text-2)', lineHeight: 1.4 }}>{text}</span>
    </div>
  );
}

function Toggle({ checked, onChange }) {
  return (
    <button onClick={onChange} className="touch" style={{
      width: 40, height: 22, borderRadius: 11,
      background: checked ? 'var(--teal-700)' : 'var(--surface-4)',
      border: 'none', position: 'relative', cursor: 'pointer',
      transition: 'background .15s',
    }}>
      <span style={{
        position: 'absolute', top: 2, left: checked ? 20 : 2,
        width: 18, height: 18, borderRadius: 9,
        background: '#fff', transition: 'left .15s',
      }}/>
    </button>
  );
}

/* ── Reports ─────────────────────────────────────────────── */
function ScreenReports({ onBack }) {
  const [period, setPeriod] = useState('month');
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Reports" onBack={onBack}/>

      <div style={{ padding: '0 16px 14px' }}>
        <ChipGroup
          value={period}
          onChange={setPeriod}
          options={[
            { value: 'month', label: 'This month' },
            { value: 'last', label: 'Last month' },
            { value: 'fy', label: 'FY 25–26' },
          ]}
        />
      </div>

      {/* Net worth hero */}
      <div style={{
        margin: '0 16px 14px', padding: '18px 22px',
        background: 'var(--surface-2)', border: '1px solid var(--line-1)',
        borderRadius: 20, position: 'relative', overflow: 'hidden',
      }}>
        <div style={{ position: 'absolute', inset: 0, color: 'var(--teal-500)', opacity: 0.05 }}>
          <svg width="100%" height="100%"><rect width="100%" height="100%" fill="url(#p-blockprint)"/></svg>
        </div>
        <div style={{ position: 'relative' }}>
          <div className="eyebrow">Net worth · liquid + invest − card o/s</div>
          <div className="num-display tnum" style={{ fontSize: 44, marginTop: 6, color: 'var(--text-1)', fontWeight: 300, letterSpacing: '-0.02em' }}>{fmtINR(482610)}</div>
          <Sparkline data={[420, 432, 448, 455, 462, 470, 475, 478, 482]} height={32} color="var(--teal-500)" style={{ marginTop: 12 }}/>
        </div>
      </div>

      {/* In/Out twin */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, margin: '0 16px 14px' }}>
        <div className="card" style={{ padding: 14 }}>
          <div className="eyebrow"><span style={{ color: 'var(--income)' }}>● </span>Income</div>
          <div className="num-display tnum" style={{ fontSize: 22, marginTop: 4, color: 'var(--text-1)' }}>{fmtINR(93899)}</div>
        </div>
        <div className="card" style={{ padding: 14 }}>
          <div className="eyebrow"><span style={{ color: 'var(--expense)' }}>● </span>Expense</div>
          <div className="num-display tnum" style={{ fontSize: 22, marginTop: 4, color: 'var(--text-1)' }}>{fmtINR(60879)}</div>
        </div>
      </div>

      {/* Spending by category bar */}
      <ReportSection title="Spending by category">
        <CategoryBars/>
      </ReportSection>

      <ReportSection title="Spending by payment app">
        <AppBars/>
      </ReportSection>

      <ReportSection title="Top merchants">
        <TopMerchants/>
      </ReportSection>

      <ReportSection title="Tax — Section usage">
        <TaxSections/>
      </ReportSection>
    </div>
  );
}

function ReportSection({ title, children }) {
  return (
    <div style={{ margin: '0 16px 18px' }}>
      <div className="eyebrow" style={{ marginBottom: 10, padding: '0 4px' }}>{title}</div>
      <div className="card" style={{ padding: '14px 16px' }}>{children}</div>
    </div>
  );
}

function CategoryBars() {
  const rows = [
    { name: 'Food & Drink', spent: 18400, color: 'var(--acc-saffron)' },
    { name: 'Home & Rent', spent: 38000, color: 'var(--acc-teal)' },
    { name: 'Bills', spent: 5840, color: 'var(--acc-magenta)' },
    { name: 'Transport', spent: 1840, color: 'var(--acc-indigo)' },
    { name: 'Shopping', spent: 6200, color: 'var(--acc-violet)' },
  ];
  const max = Math.max(...rows.map(r => r.spent));
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {rows.map(r => (
        <div key={r.name}>
          <div className="row between" style={{ marginBottom: 4 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--text-1)' }}>
              <span style={{ width: 8, height: 8, borderRadius: 4, background: r.color }}/>
              {r.name}
            </div>
            <div className="num-mono tnum" style={{ fontSize: 12, color: 'var(--text-2)' }}>{fmtINR(r.spent)}</div>
          </div>
          <div style={{ height: 4, borderRadius: 2, background: 'var(--surface-4)', overflow: 'hidden' }}>
            <div style={{ width: `${r.spent / max * 100}%`, height: '100%', background: r.color }}/>
          </div>
        </div>
      ))}
    </div>
  );
}

function AppBars() {
  const apps = [
    { name: 'GPay', amt: 21400, pct: 45 },
    { name: 'CRED', amt: 14200, pct: 30 },
    { name: 'Bank app', amt: 8400, pct: 18 },
    { name: 'BHIM', amt: 3240, pct: 7 },
  ];
  return (
    <div>
      <div style={{ display: 'flex', height: 10, borderRadius: 5, overflow: 'hidden', marginBottom: 12 }}>
        {apps.map((a, i) => (
          <div key={a.name} style={{ flex: a.pct, background: ['var(--teal-500)','var(--ochre-soft)','var(--acc-indigo)','var(--acc-magenta)'][i] }}/>
        ))}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {apps.map((a, i) => (
          <div key={a.name} className="row between" style={{ fontSize: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--text-2)' }}>
              <span style={{ width: 6, height: 6, borderRadius: 3, background: ['var(--teal-500)','var(--ochre-soft)','var(--acc-indigo)','var(--acc-magenta)'][i] }}/>
              {a.name}
            </div>
            <div className="num-mono tnum" style={{ color: 'var(--text-1)' }}>{fmtINR(a.amt)} <span style={{ color: 'var(--text-3)' }}>· {a.pct}%</span></div>
          </div>
        ))}
      </div>
    </div>
  );
}

function TopMerchants() {
  const merch = [
    { name: 'Swiggy', txns: 8, amt: 4860 },
    { name: 'Blue Tokai', txns: 6, amt: 2400 },
    { name: 'BESCOM', txns: 1, amt: 2840 },
    { name: 'Bigbasket', txns: 2, amt: 5240 },
  ];
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {merch.map(m => (
        <div key={m.name} className="row between">
          <div>
            <div style={{ fontSize: 13, color: 'var(--text-1)', fontWeight: 500 }}>{m.name}</div>
            <div className="num-mono tnum" style={{ fontSize: 11, color: 'var(--text-3)' }}>{m.txns} txns</div>
          </div>
          <div className="num-display tnum" style={{ fontSize: 16, color: 'var(--text-1)' }}>{fmtINR(m.amt)}</div>
        </div>
      ))}
    </div>
  );
}

function TaxSections() {
  const sec = [
    { name: '80C', cap: 150000, used: 120000 },
    { name: '80D', cap: 25000, used: 24800 },
    { name: '80CCD(1B)', cap: 50000, used: 0 },
  ];
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {sec.map(s => {
        const pct = Math.round(s.used / s.cap * 100);
        return (
          <div key={s.name}>
            <div className="row between" style={{ marginBottom: 4 }}>
              <div style={{ fontSize: 13, color: 'var(--text-1)', fontFamily: 'var(--font-mono)', fontWeight: 600 }} className="tnum">Section {s.name}</div>
              <div className="num-mono tnum" style={{ fontSize: 12, color: 'var(--text-2)' }}>{fmtINR(s.used)} / {fmtINR(s.cap, { compact: true })}</div>
            </div>
            <div style={{ height: 5, borderRadius: 3, background: 'var(--surface-4)', overflow: 'hidden' }}>
              <div style={{ width: `${pct}%`, height: '100%', background: pct > 90 ? 'var(--income)' : 'var(--ochre)' }}/>
            </div>
            <div style={{ fontSize: 10, color: 'var(--text-3)', marginTop: 3, fontFamily: 'var(--font-mono)' }} className="tnum">
              {pct}% utilised · ₹{(s.cap - s.used).toLocaleString('en-IN')} headroom
            </div>
          </div>
        );
      })}
    </div>
  );
}

/* ── Categories ────────────────────────────────────────────── */
const SYSTEM_EXPENSE_CATS = [
  { name: 'Food & Drink', icon: 'receipt', color: 'var(--acc-saffron)', sub: ['Groceries', 'Eating out', 'Coffee'] },
  { name: 'Transport', icon: 'arrowSwap', color: 'var(--acc-indigo)', sub: ['Auto/Cab', 'Fuel', 'Flights'] },
  { name: 'Bills & Utilities', icon: 'fire', color: 'var(--acc-magenta)', sub: ['Electricity', 'Internet', 'Mobile'] },
  { name: 'Shopping', icon: 'tag', color: 'var(--acc-violet)' },
  { name: 'Health', icon: 'shield', color: 'var(--acc-emerald)' },
  { name: 'Entertainment', icon: 'play', color: 'var(--acc-magenta)' },
  { name: 'Travel', icon: 'flag', color: 'var(--acc-saffron)' },
  { name: 'Home', icon: 'home', color: 'var(--acc-teal)' },
  { name: 'Family', icon: 'people', color: 'var(--acc-indigo)' },
  { name: 'Friends', icon: 'people', color: 'var(--ochre)' },
  { name: 'Religious & Spiritual', icon: 'flag', color: 'var(--acc-saffron)' },
  { name: 'Festivals', icon: 'sparkles', color: 'var(--terracotta)' },
  { name: 'Education', icon: 'edit', color: 'var(--acc-emerald)' },
  { name: 'Personal Care', icon: 'shield', color: 'var(--acc-violet)' },
  { name: 'Charity & Donations', icon: 'shield', color: 'var(--income)' },
  { name: 'Fees & Charges', icon: 'card', color: 'var(--expense)' },
  { name: 'Taxes', icon: 'receipt', color: 'var(--text-2)' },
  { name: 'Loan EMI', icon: 'card', color: 'var(--acc-magenta)' },
  { name: 'Insurance Premium', icon: 'shield', color: 'var(--acc-indigo)' },
  { name: 'Pets', icon: 'home', color: 'var(--ochre)' },
];

function ScreenCategories({ onBack, onAdd }) {
  const [type, setType] = useState('expense');
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Categories" onBack={onBack}/>
      <div style={{ padding: '0 16px 12px' }}>
        <ChipGroup
          value={type}
          onChange={setType}
          options={[
            { value: 'expense', label: 'Expense' },
            { value: 'income', label: 'Income' },
            { value: 'transfer', label: 'Transfer' },
            { value: 'investment', label: 'Investment' },
          ]}
        />
      </div>
      <div style={{ padding: '0 16px' }}>
        {SYSTEM_EXPENSE_CATS.map(c => (
          <div key={c.name} className="card" style={{ padding: 12, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              width: 36, height: 36, borderRadius: 10,
              background: `color-mix(in srgb, ${c.color} 25%, var(--surface-3))`,
              border: `1px solid color-mix(in srgb, ${c.color} 40%, transparent)`,
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <Icon name={c.icon} size={16} color={c.color} weight={1.7}/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)' }}>{c.name}</div>
              {c.sub ? (
                <div style={{ fontSize: 11, color: 'var(--text-3)' }}>{c.sub.join(' · ')}</div>
              ) : (
                <div style={{ fontSize: 11, color: 'var(--text-3)' }}>System</div>
              )}
            </div>
            <Icon name="chevronRight" size={16} color="var(--text-3)"/>
          </div>
        ))}
        <AddDashed label="Custom category" onClick={onAdd}/>
      </div>
    </div>
  );
}

/* ── Tags ──────────────────────────────────────────────── */
const SAMPLE_TAGS = [
  { name: 'coffee', count: 14, color: 'var(--acc-saffron)' },
  { name: 'dinner', count: 22, color: 'var(--terracotta)' },
  { name: 'sip', count: 5, color: 'var(--acc-indigo)' },
  { name: 'reimburse', count: 3, color: 'var(--acc-emerald)' },
  { name: 'mumbai-trip', count: 18, color: 'var(--acc-violet)' },
  { name: 'fixed', count: 6, color: 'var(--text-2)' },
];

function ScreenTags({ onBack, onAdd, items = SAMPLE_TAGS }) {
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Tags" onBack={onBack}/>
      <div style={{ padding: '0 16px' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, padding: '0 0 14px' }}>
          {items.map(t => (
            <div key={t.name} style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              padding: '8px 14px',
              borderRadius: 18,
              border: `1px solid color-mix(in srgb, ${t.color} 40%, transparent)`,
              background: `color-mix(in srgb, ${t.color} 12%, transparent)`,
              color: 'var(--text-1)',
              fontSize: 13,
            }}>
              <span style={{ width: 6, height: 6, borderRadius: 3, background: t.color }}/>
              #{t.name}
              <span className="num-mono tnum" style={{ fontSize: 11, color: 'var(--text-3)' }}>{t.count}</span>
            </div>
          ))}
        </div>
        <AddDashed label="New tag" onClick={onAdd}/>
      </div>
    </div>
  );
}

/* ── Settings ──────────────────────────────────────────── */
function ScreenSettings({ onBack }) {
  const [dyncolor, setDyncolor] = useState(true);
  const [smsImport, setSmsImport] = useState(false);
  const [biometric, setBiometric] = useState(false);
  const [theme, setTheme] = useState('dark');

  const sections = [
    {
      title: 'Profile',
      rows: [
        { kind: 'input', label: 'Your name', value: 'Subramanya' },
        { kind: 'static', label: 'Currency', value: '₹ INR (locked for v1)' },
      ],
    },
    {
      title: 'Dashboard',
      rows: [
        { kind: 'static', label: 'Net Position', value: 'Always pinned to top', dim: true },
        { kind: 'toggle', label: 'Income / Expense strip', value: true },
        { kind: 'toggle', label: 'Accounts row', value: true },
        { kind: 'toggle', label: 'Cards row', value: false },
        { kind: 'toggle', label: 'Recent transactions', value: true },
        { kind: 'toggle', label: 'AI Quick Entry card', value: true },
      ],
    },
    {
      title: 'Appearance',
      rows: [
        { kind: 'segment', label: 'Theme', value: theme, options: ['System', 'Light', 'Dark'], onChange: setTheme },
        { kind: 'toggle', label: 'Material You dynamic color', value: dyncolor, onChange: setDyncolor, hint: 'Tint the UI with your wallpaper palette' },
      ],
    },
    {
      title: 'Behavior',
      rows: [
        { kind: 'segment', label: 'Spouse transaction default', value: 'Ask each time', options: ['Ask each time', 'Transfer', 'Expense'] },
        { kind: 'link', label: 'Reset spouse prompt', danger: false },
      ],
    },
    {
      title: 'Security',
      rows: [
        { kind: 'toggle', label: 'Biometric lock', value: biometric, onChange: setBiometric, hint: 'Fingerprint / face / device PIN on app open' },
        { kind: 'toggle', label: 'SMS auto-import', value: smsImport, onChange: setSmsImport, hint: 'Pre-fill from bank SMS for your review' },
      ],
    },
    {
      title: 'Data',
      rows: [
        { kind: 'link', label: 'Export all data', sub: 'JSON snapshot — everything', icon: 'download' },
        { kind: 'link', label: 'Export encrypted backup', sub: 'AES-GCM, password-protected', icon: 'shield' },
        { kind: 'link', label: 'Reset all data', sub: 'Wipe permanently', icon: 'trash', danger: true },
      ],
    },
  ];

  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Settings" onBack={onBack}/>

      <div style={{ padding: '0 16px' }}>
        {sections.map(sec => (
          <div key={sec.title} style={{ marginBottom: 18 }}>
            <div className="eyebrow" style={{ padding: '4px 4px 8px' }}>{sec.title}</div>
            <div className="card-flush" style={{ overflow: 'hidden' }}>
              {sec.rows.map((r, i) => (
                <div key={r.label}>
                  <SettingsRow row={r}/>
                  {i < sec.rows.length - 1 && <div style={{ height: 1, background: 'var(--line-1)', marginLeft: 16 }}/>}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function SettingsRow({ row }) {
  if (row.kind === 'input') {
    return (
      <div style={{ padding: '12px 16px' }}>
        <div className="eyebrow" style={{ marginBottom: 6 }}>{row.label}</div>
        <input className="input" style={{ height: 44, fontSize: 14 }} defaultValue={row.value}/>
      </div>
    );
  }
  if (row.kind === 'static') {
    return (
      <div style={{ padding: '14px 16px' }}>
        <div style={{ fontSize: 14, color: row.dim ? 'var(--text-2)' : 'var(--text-1)' }}>{row.label}</div>
        {row.value && <div style={{ marginTop: 2, fontSize: 12, color: 'var(--text-3)' }}>{row.value}</div>}
      </div>
    );
  }
  if (row.kind === 'toggle') {
    return (
      <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, color: 'var(--text-1)' }}>{row.label}</div>
          {row.hint && <div style={{ marginTop: 2, fontSize: 11, color: 'var(--text-3)' }}>{row.hint}</div>}
        </div>
        <Toggle checked={row.value} onChange={row.onChange}/>
      </div>
    );
  }
  if (row.kind === 'segment') {
    return (
      <div style={{ padding: '12px 16px' }}>
        <div className="eyebrow" style={{ marginBottom: 8 }}>{row.label}</div>
        <ChipGroup
          value={row.value}
          onChange={row.onChange || (() => {})}
          options={row.options.map(o => ({ value: o, label: o }))}
        />
      </div>
    );
  }
  if (row.kind === 'link') {
    return (
      <button className="touch" style={{
        width: '100%', textAlign: 'left',
        padding: '14px 16px', background: 'transparent', border: 'none', cursor: 'pointer',
        display: 'flex', alignItems: 'center', gap: 12, fontFamily: 'var(--font-ui)',
      }}>
        {row.icon && (
          <div style={{
            width: 32, height: 32, borderRadius: 8,
            background: row.danger ? 'var(--expense-soft)' : 'var(--surface-4)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: row.danger ? 'var(--expense)' : 'var(--teal-300)',
          }}>
            <Icon name={row.icon} size={15} color="currentColor"/>
          </div>
        )}
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, color: row.danger ? 'var(--expense)' : 'var(--text-1)' }}>{row.label}</div>
          {row.sub && <div style={{ marginTop: 2, fontSize: 11, color: 'var(--text-3)' }}>{row.sub}</div>}
        </div>
        <Icon name="chevronRight" size={16} color="var(--text-3)"/>
      </button>
    );
  }
  return null;
}

/* ── About ────────────────────────────────────────────────── */
function ScreenAbout({ onBack }) {
  return (
    <div style={{ paddingBottom: 100, display: 'flex', flexDirection: 'column' }}>
      <TopBar title="About" onBack={onBack}/>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '40px 24px' }}>
        <BrandMark size={88} bg="var(--teal-700)" color="#fff" radius={22}/>
        <div style={{ marginTop: 18, fontSize: 32, color: 'var(--text-1)', fontFamily: 'var(--font-display)', letterSpacing: '-0.01em' }}>Artha</div>
        <div style={{ marginTop: 4, fontSize: 14, color: 'var(--text-2)', fontFamily: 'var(--font-display)', fontStyle: 'italic' }}>Your money. Your rules.</div>
        <div className="num-mono tnum" style={{ marginTop: 16, fontSize: 11, color: 'var(--text-3)' }}>v0.2.0 · build 188</div>

        <div style={{ marginTop: 36, width: '100%', maxWidth: 320, padding: 18, background: 'var(--surface-2)', border: '1px solid var(--line-1)', borderRadius: 16 }}>
          <div className="eyebrow" style={{ marginBottom: 8 }}>What it means</div>
          <div style={{ fontSize: 13, color: 'var(--text-2)', lineHeight: 1.6 }}>
            <span className="deva" style={{ color: 'var(--teal-300)', fontSize: 18 }}>अर्थ</span> · artha · is one of the four puruṣārthas — the pursuit of material prosperity. Treated as a discipline, not a hustle.
          </div>
        </div>

        <div style={{ marginTop: 22, width: '100%', maxWidth: 320 }} className="card-flush">
          {[
            { label: 'Privacy', sub: 'Everything stays on your device' },
            { label: 'Source code', sub: 'Apache 2.0 on GitHub', icon: 'link' },
            { label: 'Acknowledgements', icon: 'chevronRight' },
          ].map((r, i) => (
            <div key={r.label} style={{ display: 'flex', alignItems: 'center', padding: '14px 16px', borderTop: i ? '1px solid var(--line-1)' : 'none' }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, color: 'var(--text-1)' }}>{r.label}</div>
                {r.sub && <div style={{ marginTop: 2, fontSize: 11, color: 'var(--text-3)' }}>{r.sub}</div>}
              </div>
              <Icon name="chevronRight" size={16} color="var(--text-3)"/>
            </div>
          ))}
        </div>

        <div style={{ marginTop: 28, fontSize: 11, color: 'var(--text-3)', textAlign: 'center', fontFamily: 'var(--font-mono)' }} className="tnum">
          Built with Claude Code · Bengaluru, 2026
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  ScreenSubscriptions, SAMPLE_SUBS, SubscriptionRow,
  ScreenRecurring, SAMPLE_RECURRING, RuleClause, Toggle,
  ScreenPeople, SAMPLE_PEOPLE,
  ScreenRules, SAMPLE_RULES,
  ScreenReports, ReportSection, CategoryBars, AppBars, TopMerchants, TaxSections,
  ScreenCategories, SYSTEM_EXPENSE_CATS,
  ScreenTags, SAMPLE_TAGS,
  ScreenSettings, SettingsRow,
  ScreenAbout,
});
