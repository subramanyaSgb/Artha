// ─────────────────────────────────────────────────────────────
// More menu + secondary screens
// ─────────────────────────────────────────────────────────────

const MORE_GROUPS = [
  {
    title: 'Money',
    items: [
      { id: 'investments', label: 'Investments', icon: 'chart', sub: 'SIPs, FDs, stocks, gold' },
      { id: 'insurance', label: 'Insurance', icon: 'shield', sub: 'Policies & premiums' },
      { id: 'goals', label: 'Goals', icon: 'flag', sub: 'Emergency fund, travel…' },
      { id: 'budgets', label: 'Budgets', icon: 'wallet', sub: 'Monthly caps & alerts' },
    ],
  },
  {
    title: 'Recurring',
    items: [
      { id: 'subscriptions', label: 'Subscriptions', icon: 'play', sub: 'Spotify, Netflix, iCloud' },
      { id: 'recurring', label: 'Recurring rules', icon: 'refresh', sub: 'Rent, EMIs, SIPs' },
    ],
  },
  {
    title: 'People & rules',
    items: [
      { id: 'people', label: 'People', icon: 'people', sub: 'Who owes whom' },
      { id: 'rules', label: 'Auto-categorize', icon: 'rules', sub: 'Rules engine' },
    ],
  },
  {
    title: 'Look-ups',
    items: [
      { id: 'reports', label: 'Reports', icon: 'chart', sub: 'Spending, taxes, trends' },
      { id: 'categories', label: 'Categories', icon: 'category', sub: '24 system + custom' },
      { id: 'tags', label: 'Tags', icon: 'tag', sub: 'Loose labels' },
    ],
  },
  {
    title: 'App',
    items: [
      { id: 'settings', label: 'Settings', icon: 'settings', sub: 'Theme, security, data' },
      { id: 'about', label: 'About Artha', icon: 'info', sub: 'v0.2 · open source' },
    ],
  },
];

function ScreenMore({ user, onNav }) {
  return (
    <div style={{ paddingBottom: 100 }}>
      <DashHeader user={user} date="Fri, 22 May"/>

      <div style={{ padding: '4px 20px 12px' }}>
        <div className="eyebrow" style={{ marginBottom: 4 }}>Everything else</div>
        <div style={{ fontSize: 26, fontFamily: 'var(--font-display)', color: 'var(--text-1)', letterSpacing: '-0.01em' }}>More</div>
      </div>

      <div style={{ padding: '0 16px' }}>
        {MORE_GROUPS.map(g => (
          <div key={g.title} style={{ marginBottom: 18 }}>
            <div className="eyebrow" style={{ padding: '4px 4px 8px' }}>{g.title}</div>
            <div className="card-flush" style={{ overflow: 'hidden' }}>
              {g.items.map((it, i) => (
                <div key={it.id}>
                  <button onClick={() => onNav(it.id)} className="touch" style={{
                    width: '100%', textAlign: 'left',
                    display: 'flex', alignItems: 'center', gap: 14,
                    padding: '12px 16px', background: 'transparent', border: 'none', cursor: 'pointer',
                    fontFamily: 'var(--font-ui)',
                  }}>
                    <div style={{
                      width: 36, height: 36, borderRadius: 11,
                      background: 'var(--surface-4)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                    }}>
                      <Icon name={it.icon} size={17} color="var(--teal-300)" weight={1.7}/>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 14.5, color: 'var(--text-1)', fontWeight: 500 }}>{it.label}</div>
                      <div style={{ fontSize: 11.5, color: 'var(--text-3)', marginTop: 2 }}>{it.sub}</div>
                    </div>
                    <Icon name="chevronRight" size={16} color="var(--text-3)"/>
                  </button>
                  {i < g.items.length - 1 && <div style={{ height: 1, background: 'var(--line-1)', marginLeft: 64 }}/>}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Investments screen
   ─────────────────────────────────────────────────────────── */
const SAMPLE_INVEST = [
  { id: 'i1', name: 'Nifty 50 Index Fund', kind: 'Mutual Fund', amc: 'UTI', current: 124300, invested: 96000, color: 'var(--acc-indigo)' },
  { id: 'i2', name: 'Parag Parikh Flexi Cap', kind: 'Mutual Fund', amc: 'PPFAS', current: 86200, invested: 65000, color: 'var(--acc-teal)' },
  { id: 'i3', name: 'HDFC Bank FD — 7.1%', kind: 'FD', amc: 'HDFC', current: 50500, invested: 50000, color: 'var(--acc-saffron)' },
  { id: 'i4', name: 'Sovereign Gold Bond \'31', kind: 'Bonds', amc: 'RBI', current: 38400, invested: 32000, color: 'var(--ochre)' },
  { id: 'i5', name: 'PPF — SBI', kind: 'PPF', amc: 'SBI', current: 45200, invested: 42000, color: 'var(--acc-emerald)' },
];

function ScreenInvestments({ onBack, onAdd, items = SAMPLE_INVEST }) {
  const [view, setView] = useState('all');
  const totalCur = items.reduce((a, x) => a + x.current, 0);
  const totalInv = items.reduce((a, x) => a + x.invested, 0);
  const gain = totalCur - totalInv;
  const gainPct = (gain / totalInv * 100).toFixed(1);

  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Investments" onBack={onBack}/>

      {/* Hero: current value with gain badge */}
      <div style={{
        margin: '6px 16px 12px', padding: '20px 22px',
        background: 'var(--surface-2)', border: '1px solid var(--line-1)',
        borderRadius: 20, position: 'relative', overflow: 'hidden',
      }}>
        <div style={{ position: 'absolute', inset: 0, color: 'var(--ochre)', opacity: 0.05 }}>
          <svg width="100%" height="100%"><rect width="100%" height="100%" fill="url(#p-jaali)"/></svg>
        </div>
        <div style={{ position: 'relative' }}>
          <div className="eyebrow">Portfolio value</div>
          <div className="num-display tnum" style={{ fontSize: 48, lineHeight: 1, marginTop: 8, color: 'var(--text-1)', fontWeight: 300, letterSpacing: '-0.02em' }}>{fmtINR(totalCur)}</div>
          <div style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: 4,
              padding: '4px 10px', borderRadius: 12,
              background: 'var(--income-soft)', color: 'var(--income)',
              fontSize: 12, fontWeight: 600,
            }} className="tnum">
              <Icon name="arrowUp" size={12} color="currentColor" weight={2.2}/>
              {fmtINR(gain)} · {gainPct}%
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">
              Invested {fmtINR(totalInv)}
            </div>
          </div>
        </div>
      </div>

      {/* Allocation bar (mini donut alternative) */}
      <div style={{ margin: '0 16px 14px' }}>
        <div className="eyebrow" style={{ padding: '0 4px 8px' }}>Allocation</div>
        <div style={{ display: 'flex', height: 8, borderRadius: 4, overflow: 'hidden' }}>
          {items.map(it => (
            <div key={it.id} style={{ flex: it.current, background: it.color, borderRight: '1px solid var(--surface-1)' }}/>
          ))}
        </div>
        <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {Array.from(new Set(items.map(i => i.kind))).map(k => {
            const it = items.find(i => i.kind === k);
            return (
              <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 11, color: 'var(--text-2)' }}>
                <span style={{ width: 7, height: 7, borderRadius: 4, background: it.color }}/>
                {k}
              </div>
            );
          })}
        </div>
      </div>

      <ChipGroup
        scroll
        value={view}
        onChange={setView}
        options={[
          { value: 'all', label: 'All' },
          { value: 'mf', label: 'Mutual Funds' },
          { value: 'fd', label: 'Deposits' },
          { value: 'gold', label: 'Gold' },
          { value: 'tax', label: 'Tax-saving' },
        ]}
      />

      <div style={{ padding: '12px 16px 0' }}>
        {items.map(it => <InvestRow key={it.id} item={it}/>)}
        <button onClick={onAdd} className="touch" style={{
          width: '100%', marginTop: 8,
          padding: '14px', background: 'transparent',
          border: '1px dashed var(--line-2)', borderRadius: 14,
          color: 'var(--text-2)', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          fontFamily: 'var(--font-ui)', fontSize: 13,
        }}>
          <Icon name="plus" size={16} color="currentColor"/>
          Add investment
        </button>
      </div>
    </div>
  );
}

function InvestRow({ item }) {
  const pct = ((item.current - item.invested) / item.invested * 100).toFixed(1);
  const up = item.current >= item.invested;
  return (
    <div className="card" style={{ padding: '14px 14px', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 12 }}>
      <div style={{
        width: 4, alignSelf: 'stretch', borderRadius: 2, background: item.color,
      }}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.name}</div>
        <div style={{ marginTop: 2, fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">{item.kind} · {item.amc}</div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <div className="num-display tnum" style={{ fontSize: 18, color: 'var(--text-1)', letterSpacing: '-0.01em', fontWeight: 400 }}>{fmtINR(item.current)}</div>
        <div style={{ fontSize: 11, color: up ? 'var(--income)' : 'var(--expense)', fontFamily: 'var(--font-mono)', marginTop: 1 }} className="tnum">
          {up ? '+' : ''}{pct}%
        </div>
      </div>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Insurance screen
   ─────────────────────────────────────────────────────────── */
const SAMPLE_INSURANCE = [
  { id: 'p1', name: 'HDFC Life Click 2 Protect', type: 'Life (Term)', sum: 15000000, premium: 18400, freq: 'yearly', next: '12 Jun 2026', color: 'var(--acc-indigo)' },
  { id: 'p2', name: 'Star Comprehensive', type: 'Health', sum: 1000000, premium: 24800, freq: 'yearly', next: '03 Aug 2026', color: 'var(--acc-emerald)' },
  { id: 'p3', name: 'Bajaj Allianz — Car', type: 'Vehicle', sum: 850000, premium: 12600, freq: 'yearly', next: '21 Sep 2026', color: 'var(--acc-saffron)' },
];

function ScreenInsurance({ onBack, onAdd, items = SAMPLE_INSURANCE }) {
  const totalSum = items.reduce((a, x) => a + x.sum, 0);
  const totalPremium = items.reduce((a, x) => a + x.premium, 0);
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Insurance" onBack={onBack}/>

      <div style={{ margin: '6px 16px 14px', padding: '18px 20px', background: 'var(--surface-2)', border: '1px solid var(--line-1)', borderRadius: 18 }}>
        <div className="eyebrow">Total cover</div>
        <div className="num-display tnum" style={{ fontSize: 40, lineHeight: 1, marginTop: 6, color: 'var(--text-1)', fontWeight: 300, letterSpacing: '-0.02em' }}>{fmtINR(totalSum, { compact: true })}</div>
        <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">
          Annual premium {fmtINR(totalPremium)} · {items.length} active policies
        </div>
      </div>

      <div style={{ padding: '0 16px' }}>
        {items.map(p => (
          <div key={p.id} className="card" style={{ padding: 16, marginBottom: 10, display: 'flex', gap: 12 }}>
            <div style={{
              width: 40, height: 40, borderRadius: 12,
              background: `color-mix(in srgb, ${p.color} 25%, var(--surface-3))`,
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
              border: `1px solid color-mix(in srgb, ${p.color} 40%, transparent)`,
            }}>
              <Icon name="shield" size={18} color={p.color} weight={1.7}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)' }}>{p.name}</div>
                <div className="tnum" style={{ fontSize: 13, color: 'var(--text-1)', fontWeight: 500 }}>{fmtINR(p.sum, { compact: true })}</div>
              </div>
              <div style={{ marginTop: 2, fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">
                {p.type}
              </div>
              <div style={{ marginTop: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11.5 }}>
                <span style={{ color: 'var(--text-2)' }}>Next due {p.next}</span>
                <span className="tnum" style={{ color: 'var(--text-1)', fontFamily: 'var(--font-mono)' }}>{fmtINR(p.premium)}/yr</span>
              </div>
            </div>
          </div>
        ))}
        <AddDashed label="Add policy" onClick={onAdd}/>
      </div>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Budgets screen
   ─────────────────────────────────────────────────────────── */
const SAMPLE_BUDGETS = [
  { id: 'b1', name: 'Food & Drink', spent: 5840, cap: 8000, color: 'var(--acc-saffron)' },
  { id: 'b2', name: 'Transport', spent: 1240, cap: 2500, color: 'var(--acc-indigo)' },
  { id: 'b3', name: 'Subscriptions', spent: 1879, cap: 1500, color: 'var(--acc-violet)' },
  { id: 'b4', name: 'Shopping', spent: 6200, cap: 5000, color: 'var(--acc-magenta)' },
  { id: 'b5', name: 'Overall (May)', spent: 31580, cap: 60000, color: 'var(--teal-500)', overall: true },
];

function ScreenBudgets({ onBack, onAdd, items = SAMPLE_BUDGETS }) {
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Budgets" onBack={onBack}/>
      <div style={{ padding: '6px 16px 0' }}>
        {items.map(b => <BudgetRow key={b.id} b={b}/>)}
        <AddDashed label="Set new cap" onClick={onAdd}/>
      </div>
    </div>
  );
}

function BudgetRow({ b }) {
  const pct = Math.min(140, Math.round(b.spent / b.cap * 100));
  const isOver = pct > 100;
  const isWarn = pct > 80 && !isOver;
  return (
    <div className="card" style={{ padding: 16, marginBottom: 10 }}>
      <div className="row between" style={{ marginBottom: 6 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ width: 8, height: 8, borderRadius: 4, background: b.color }}/>
          <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)' }}>{b.name}</div>
        </div>
        <div className="num-mono tnum" style={{
          fontSize: 11, fontWeight: 600,
          color: isOver ? 'var(--expense)' : isWarn ? 'var(--ochre)' : 'var(--text-2)',
        }}>{pct}%</div>
      </div>
      <div className="row between" style={{ marginBottom: 8 }}>
        <div className="tnum" style={{ fontSize: 13, color: 'var(--text-1)', fontFamily: 'var(--font-mono)' }}>
          {fmtINR(b.spent)} <span style={{ color: 'var(--text-3)' }}>of {fmtINR(b.cap)}</span>
        </div>
        <div style={{ fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">
          {isOver ? `Over by ${fmtINR(b.spent - b.cap)}` : `${fmtINR(b.cap - b.spent)} left`}
        </div>
      </div>
      <div style={{ height: 6, background: 'var(--surface-4)', borderRadius: 3, overflow: 'hidden', position: 'relative' }}>
        <div style={{
          width: `${Math.min(100, pct)}%`, height: '100%',
          background: isOver ? 'var(--expense)' : isWarn ? 'var(--ochre)' : b.color,
          transition: 'width .3s',
        }}/>
        {isOver && (
          <div style={{
            position: 'absolute', left: '100%', top: -2, width: `${pct - 100}%`, height: 10,
            background: 'repeating-linear-gradient(45deg, var(--expense) 0 4px, transparent 4px 8px)',
          }}/>
        )}
      </div>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Goals screen
   ─────────────────────────────────────────────────────────── */
const SAMPLE_GOALS = [
  { id: 'g1', name: 'Emergency fund', target: 600000, current: 180000, eta: 'Mar 2027', icon: 'shield', color: 'var(--acc-teal)' },
  { id: 'g2', name: 'Japan trip — Spring 2027', target: 350000, current: 84200, eta: 'Apr 2027', icon: 'flag', color: 'var(--acc-saffron)' },
  { id: 'g3', name: 'New laptop', target: 180000, current: 142000, eta: 'Jul 2026', icon: 'play', color: 'var(--acc-indigo)' },
];

function ScreenGoals({ onBack, onAdd, items = SAMPLE_GOALS }) {
  return (
    <div style={{ paddingBottom: 100 }}>
      <TopBar title="Goals" onBack={onBack}/>
      <div style={{ padding: '6px 16px 0' }}>
        {items.map(g => <GoalCard key={g.id} g={g}/>)}
        <AddDashed label="Add goal" onClick={onAdd}/>
      </div>
    </div>
  );
}

function GoalCard({ g }) {
  const pct = Math.min(100, Math.round(g.current / g.target * 100));
  return (
    <div className="card" style={{ padding: 16, marginBottom: 12, position: 'relative', overflow: 'hidden' }}>
      {/* corner chhatri silhouette */}
      <div style={{ position: 'absolute', right: -16, bottom: -16, opacity: 0.06, color: g.color }}>
        <Chhatri size={110} color="currentColor"/>
      </div>
      <div style={{ position: 'relative' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
          <div style={{
            width: 32, height: 32, borderRadius: 10,
            background: `color-mix(in srgb, ${g.color} 25%, var(--surface-3))`,
            border: `1px solid color-mix(in srgb, ${g.color} 40%, transparent)`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name={g.icon} size={15} color={g.color} weight={1.8}/>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14.5, fontWeight: 600, color: 'var(--text-1)' }}>{g.name}</div>
            <div style={{ fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">ETA {g.eta}</div>
          </div>
        </div>
        <div className="row between" style={{ alignItems: 'baseline', marginBottom: 8 }}>
          <div className="num-display tnum" style={{ fontSize: 22, color: 'var(--text-1)', letterSpacing: '-0.01em' }}>{fmtINR(g.current)}</div>
          <div className="tnum" style={{ fontSize: 12, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }}>of {fmtINR(g.target)}</div>
        </div>
        <div style={{ height: 8, background: 'var(--surface-4)', borderRadius: 4, overflow: 'hidden' }}>
          <div style={{ width: `${pct}%`, height: '100%', background: g.color }}/>
        </div>
        <div className="row between" style={{ marginTop: 6 }}>
          <span style={{ fontSize: 11, color: 'var(--text-3)' }}>{pct}% of target</span>
          <span className="tnum" style={{ fontSize: 11, color: 'var(--text-2)', fontFamily: 'var(--font-mono)' }}>+₹{Math.round((g.target - g.current) / 12).toLocaleString('en-IN')}/mo to hit ETA</span>
        </div>
      </div>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Helper — dashed Add button
   ─────────────────────────────────────────────────────────── */
function AddDashed({ label, onClick }) {
  return (
    <button onClick={onClick} className="touch" style={{
      width: '100%', marginTop: 6,
      padding: '14px',
      background: 'transparent', border: '1px dashed var(--line-2)',
      borderRadius: 14,
      color: 'var(--text-2)', cursor: 'pointer',
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
      fontFamily: 'var(--font-ui)', fontSize: 13, fontWeight: 500,
    }}>
      <Icon name="plus" size={16} color="currentColor"/>
      {label}
    </button>
  );
}

Object.assign(window, {
  MORE_GROUPS, ScreenMore,
  ScreenInvestments, InvestRow, SAMPLE_INVEST,
  ScreenInsurance, SAMPLE_INSURANCE,
  ScreenBudgets, BudgetRow, SAMPLE_BUDGETS,
  ScreenGoals, GoalCard, SAMPLE_GOALS,
  AddDashed,
});
