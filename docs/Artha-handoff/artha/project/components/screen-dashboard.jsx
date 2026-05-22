// ─────────────────────────────────────────────────────────────
// Screen — Dashboard (home tab)
// Hero #2 treatment: dark + hairline teal border, light-weight
// editorial numerals, अ corner mark, block-print texture.
// ─────────────────────────────────────────────────────────────

/* Sample data — kept here so the prototype feels real */
const SAMPLE = {
  user: 'Subramanya',
  netPosition: 482610,
  changeAmount: 12400,
  changePct: 2.6,
  income: 84200,
  expense: 31580,
  budget: 60000,
  spark30: [430, 442, 438, 455, 449, 462, 460, 471, 468, 475, 470, 478, 482],
  accounts: [
    { id: 'a1', name: 'HDFC Savings', tag: '7421', bal: 142850, type: 'Bank Savings', color: 'var(--acc-teal)', glyph: 'bank' },
    { id: 'a2', name: 'ICICI Current', tag: '0902', bal: 62300, type: 'Bank Current', color: 'var(--acc-indigo)', glyph: 'bank' },
    { id: 'a3', name: 'Cash Wallet', tag: null, bal: 4200, type: 'Cash', color: 'var(--acc-saffron)', glyph: 'wallet' },
    { id: 'a4', name: 'Niyo Forex', tag: '2210', bal: 18650, type: 'Wallet', color: 'var(--acc-emerald)', glyph: 'wallet' },
  ],
  cards: [
    { id: 'c1', name: 'HDFC Regalia', last4: '8842', network: 'Visa', limit: 300000, used: 47600, due: 24, color: 'var(--acc-indigo)' },
    { id: 'c2', name: 'Axis Magnus', last4: '3309', network: 'Mastercard', limit: 500000, used: 21800, due: 3, color: 'var(--acc-magenta)' },
    { id: 'c3', name: 'ICICI Amazon Pay', last4: '7104', network: 'Visa', limit: 200000, used: 8420, due: 11, color: 'var(--acc-saffron)' },
  ],
  recent: [
    { id: 't1', desc: 'Blue Tokai — Indiranagar', cat: 'Food & Drink', icon: 'receipt', amt: -420, when: 'Today, 9:12 AM', acc: 'HDFC •8842', tag: ['coffee'] },
    { id: 't2', desc: 'Salary — Acme Corp', cat: 'Income', icon: 'arrowDown', amt: 92000, when: 'Yesterday', acc: 'HDFC Savings', tag: [] },
    { id: 't3', desc: 'Swiggy — Tibetan Spoon', cat: 'Food & Drink', icon: 'receipt', amt: -680, when: 'Yesterday, 8:40 PM', acc: 'GPay → HDFC', tag: ['dinner'] },
    { id: 't4', desc: 'Auto to MG Road', cat: 'Transport', icon: 'arrowSwap', amt: -180, when: 'Wed, 21 May', acc: 'Cash', tag: [] },
    { id: 't5', desc: 'Zerodha — SIP, Nifty 50', cat: 'Investment', icon: 'chart', amt: -10000, when: 'Tue, 20 May', acc: 'HDFC → Zerodha', tag: ['sip'] },
    { id: 't6', desc: 'BESCOM Electricity', cat: 'Bills & Utilities', icon: 'fire', amt: -2840, when: 'Mon, 19 May', acc: 'GPay → ICICI', tag: [] },
  ],
};

/* ───── Hero — Net Position ───── */
function NetPositionHero({ value, change, changePct, spark, onTap }) {
  return (
    <div style={{
      margin: '8px 16px 0',
      padding: '20px 22px 18px',
      background: 'var(--surface-2)',
      border: '1px solid var(--line-teal)',
      borderRadius: 20,
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Block-print texture — extremely subtle */}
      <div style={{
        position: 'absolute', inset: 0, color: 'var(--teal-500)',
        opacity: 0.06, pointerEvents: 'none',
      }}>
        <svg width="100%" height="100%">
          <rect width="100%" height="100%" fill="url(#p-jaali)"/>
        </svg>
      </div>

      {/* Corner glyph */}
      <div style={{
        position: 'absolute', top: 16, right: 16,
        width: 32, height: 32, borderRadius: 8,
        border: '1px solid var(--line-teal)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: 'var(--teal-300)',
        fontFamily: 'var(--font-deva)', fontSize: 18, lineHeight: 1, paddingBottom: 2,
      }}>अ</div>

      <div style={{ position: 'relative' }}>
        <div className="eyebrow" style={{ color: 'var(--text-3)', marginBottom: 8 }}>Net Position</div>

        <div className="num-display tnum" style={{
          fontSize: 60, lineHeight: 1, color: 'var(--text-1)',
          letterSpacing: '-0.02em',
          fontWeight: 300,
        }}>{fmtINR(value)}</div>

        <div style={{
          marginTop: 10, display: 'flex', alignItems: 'center', gap: 8,
          fontSize: 13, fontFamily: 'var(--font-mono)',
          color: change >= 0 ? 'var(--income)' : 'var(--expense)',
        }} className="tnum">
          <Icon name={change >= 0 ? 'arrowUp' : 'arrowDown'} size={14} color="currentColor" weight={2}/>
          <span style={{ fontWeight: 500 }}>{fmtINR(Math.abs(change), { sign: false })}</span>
          <span style={{ color: 'var(--text-3)' }}>•</span>
          <span>{changePct > 0 ? '+' : ''}{changePct}%</span>
          <span style={{ color: 'var(--text-3)', fontFamily: 'var(--font-ui)' }}>this month</span>
        </div>

        {/* Sparkline */}
        <Sparkline data={spark} height={36} color="var(--teal-500)" style={{ marginTop: 14 }}/>

        {/* Breakdown */}
        <div style={{
          marginTop: 18, paddingTop: 14,
          borderTop: '1px solid var(--line-1)',
          display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8,
        }}>
          <Breakdown label="Liquid" value={210800} color="var(--teal-300)"/>
          <Breakdown label="Invested" value={344600} color="var(--ochre-soft)"/>
          <Breakdown label="Card o/s" value={-72790} color="var(--expense)" negative/>
        </div>
      </div>
    </div>
  );
}

function Breakdown({ label, value, color, negative }) {
  return (
    <div>
      <div style={{ fontSize: 10, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.12em', fontWeight: 600 }}>{label}</div>
      <div className="tnum" style={{
        marginTop: 4, fontSize: 16, fontWeight: 500,
        color: color || 'var(--text-1)',
        fontFamily: 'var(--font-ui)',
        letterSpacing: '-0.01em',
      }}>{fmtINR(Math.abs(value), { compact: true })}</div>
    </div>
  );
}

/* Sparkline — minimal SVG line + gradient */
function Sparkline({ data, height = 32, width = '100%', color = 'var(--teal-500)', style }) {
  const min = Math.min(...data), max = Math.max(...data), range = max - min || 1;
  const W = 100, H = height;
  const pts = data.map((v, i) => `${(i / (data.length - 1)) * W},${H - ((v - min) / range) * (H - 4) - 2}`).join(' ');
  const area = `M0,${H} L${pts.split(' ').join(' L')} L${W},${H} Z`;
  return (
    <svg width={width} height={H} viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={style}>
      <defs>
        <linearGradient id="sparkfill" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.3"/>
          <stop offset="100%" stopColor={color} stopOpacity="0"/>
        </linearGradient>
      </defs>
      <path d={area} fill="url(#sparkfill)" />
      <polyline points={pts} fill="none" stroke={color} strokeWidth="1.2" strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke"/>
      {/* end dot */}
      <circle cx={W} cy={H - ((data[data.length-1] - min) / range) * (H - 4) - 2} r="1.8" fill={color}/>
    </svg>
  );
}

/* Income/Expense paired tiles */
function FlowStrip({ income, expense, budget }) {
  const pct = Math.min(100, Math.round(expense / budget * 100));
  return (
    <div style={{ margin: '14px 16px 0', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
      <FlowTile dir="in" label="Income" value={income} when="May"/>
      <FlowTile dir="out" label="Spending" value={expense} when={`${pct}% of cap`} barPct={pct}/>
    </div>
  );
}

function FlowTile({ dir, label, value, when, barPct }) {
  const isIncome = dir === 'in';
  const color = isIncome ? 'var(--income)' : 'var(--expense)';
  return (
    <div className="card" style={{ padding: '14px 14px 12px', position: 'relative' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{
          width: 22, height: 22, borderRadius: 11,
          background: isIncome ? 'var(--income-soft)' : 'var(--expense-soft)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon name={isIncome ? 'arrowDown' : 'arrowUp'} size={13} color={color} weight={2.2}/>
        </div>
        <div className="eyebrow">{label}</div>
      </div>
      <div className="num-display tnum" style={{
        marginTop: 6, fontSize: 28, color: 'var(--text-1)', fontWeight: 400,
        letterSpacing: '-0.01em', lineHeight: 1.1,
      }}>{fmtINR(value)}</div>
      <div style={{ marginTop: 6, fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">{when}</div>
      {barPct !== undefined && (
        <div style={{ position: 'absolute', left: 14, right: 14, bottom: 8, height: 3, borderRadius: 2, background: 'var(--surface-4)', overflow: 'hidden' }}>
          <div style={{ width: `${barPct}%`, height: '100%', background: barPct > 90 ? 'var(--ochre)' : 'var(--expense)' }}/>
        </div>
      )}
    </div>
  );
}

/* ───── Accounts row ───── */
function AccountsRow({ accounts, onAdd }) {
  return (
    <div>
      <SectionHeader title="Accounts" action="View all"/>
      <div style={{
        display: 'flex', gap: 10, padding: '0 16px 4px',
        overflowX: 'auto',
      }} className="phone-content">
        {accounts.map(a => <AccountChip key={a.id} acc={a}/>)}
        <button onClick={onAdd} className="touch" style={{
          flexShrink: 0, width: 96, height: 116,
          borderRadius: 16, border: '1px dashed var(--line-2)',
          background: 'transparent', color: 'var(--text-3)',
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 6,
          cursor: 'pointer', fontFamily: 'var(--font-ui)', fontSize: 12,
        }}>
          <Icon name="plus" size={18} color="var(--text-2)"/>
          Add account
        </button>
      </div>
    </div>
  );
}

function AccountChip({ acc }) {
  return (
    <div style={{
      flexShrink: 0, width: 158, height: 116,
      padding: '14px 14px',
      borderRadius: 16,
      background: `linear-gradient(155deg, ${acc.color} 0%, color-mix(in srgb, ${acc.color} 65%, #000) 100%)`,
      border: '1px solid rgba(255,255,255,0.06)',
      color: '#fff', display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
      position: 'relative', overflow: 'hidden',
    }}>
      {/* Texture overlay */}
      <svg width="100%" height="100%" style={{ position: 'absolute', inset: 0, opacity: 0.18 }}>
        <rect width="100%" height="100%" fill="url(#p-bandhani)"/>
      </svg>
      <div style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 6 }}>
        <Icon name={acc.glyph} size={14} color="rgba(255,255,255,0.85)"/>
        <div style={{ fontSize: 10, opacity: 0.85, letterSpacing: '0.08em', textTransform: 'uppercase', fontWeight: 600 }}>{acc.type}</div>
      </div>
      <div style={{ position: 'relative' }}>
        <div style={{ fontSize: 13, fontWeight: 600, lineHeight: 1.2 }}>{acc.name}</div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 6 }}>
          <div className="tnum" style={{ fontSize: 17, fontWeight: 500, letterSpacing: '-0.01em' }}>{fmtINR(acc.bal)}</div>
          {acc.tag && <div className="num-mono" style={{ fontSize: 10, opacity: 0.7 }}>•{acc.tag}</div>}
        </div>
      </div>
    </div>
  );
}

/* ───── Recent transactions ───── */
function RecentList({ items, onViewAll }) {
  // group by date
  const grouped = items.reduce((acc, t) => {
    (acc[t.when.split(',')[0]] ??= []).push(t);
    return acc;
  }, {});
  return (
    <div>
      <SectionHeader title="Recent activity" action="View all" actionOnClick={onViewAll}/>
      <div style={{ padding: '0 16px' }}>
        {Object.entries(grouped).map(([day, list]) => (
          <div key={day}>
            <div style={{ padding: '12px 4px 8px', fontSize: 11, color: 'var(--text-3)', letterSpacing: '0.08em', textTransform: 'uppercase', fontWeight: 600 }}>{day}</div>
            <div className="card-flush" style={{ overflow: 'hidden' }}>
              {list.map((t, i) => (
                <div key={t.id}>
                  <TransactionRow t={t}/>
                  {i < list.length - 1 && <div style={{ height: 1, background: 'var(--line-1)', marginLeft: 56 }}/>}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function TransactionRow({ t, onClick }) {
  const isIncome = t.amt > 0;
  const time = t.when.includes(',') ? t.when.split(',').slice(1).join(',').trim() : t.when;
  return (
    <div onClick={onClick} className="touch" style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '12px 14px',
    }}>
      <div style={{
        width: 36, height: 36, borderRadius: 11,
        background: isIncome ? 'var(--income-soft)' : 'var(--surface-4)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
        color: isIncome ? 'var(--income)' : 'var(--text-2)',
      }}>
        <Icon name={t.icon} size={17} color="currentColor" weight={1.7}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 14, color: 'var(--text-1)', fontWeight: 500,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{t.desc}</div>
        <div style={{ marginTop: 2, display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }}>
          <span>{t.acc}</span>
          {time && <><span>•</span><span>{time}</span></>}
        </div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <div className="tnum" style={{
          fontSize: 15, fontWeight: 500,
          color: isIncome ? 'var(--income)' : 'var(--text-1)',
          fontFamily: 'var(--font-ui)', letterSpacing: '-0.01em',
        }}>{fmtINR(t.amt, { sign: true })}</div>
        <div style={{ marginTop: 2, fontSize: 10, color: 'var(--text-3)', letterSpacing: '0.04em' }}>{t.cat}</div>
      </div>
    </div>
  );
}

/* ───── Dashboard greeting header ───── */
function DashHeader({ user, date }) {
  return (
    <div style={{
      padding: '12px 20px 6px',
      display: 'flex', alignItems: 'center', gap: 12,
    }}>
      <BrandMark size={40} bg="var(--teal-900)" color="var(--teal-300)" radius={12}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 11, color: 'var(--text-3)', letterSpacing: '0.08em', textTransform: 'uppercase', fontWeight: 600 }}>{date}</div>
        <div style={{ fontSize: 18, color: 'var(--text-1)', fontWeight: 600, letterSpacing: '-0.01em' }}>
          Namaste, {user}
        </div>
      </div>
      <button className="touch" style={{
        width: 40, height: 40, borderRadius: 12,
        background: 'var(--surface-2)', border: '1px solid var(--line-1)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
      }}>
        <Icon name="search" size={18} color="var(--text-1)"/>
      </button>
    </div>
  );
}

/* ───── AI Quick Entry — inline card on dashboard ───── */
function AIEntryCard({ onOpen }) {
  return (
    <div style={{ margin: '14px 16px 0' }}>
      <button onClick={onOpen} className="touch" style={{
        width: '100%', textAlign: 'left',
        padding: '14px 16px',
        borderRadius: 16,
        background: 'linear-gradient(135deg, var(--surface-2) 0%, var(--teal-950) 100%)',
        border: '1px solid var(--line-teal)',
        color: 'var(--text-1)',
        cursor: 'pointer',
        display: 'flex', alignItems: 'center', gap: 12,
        fontFamily: 'var(--font-ui)',
      }}>
        <div style={{
          width: 40, height: 40, borderRadius: 12,
          background: 'var(--teal-700)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          boxShadow: '0 0 0 1px rgba(94,234,212,0.2)',
        }}>
          <Icon name="sparkles" size={20} color="#fff"/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13, fontWeight: 600 }}>Quick add with Gemini</div>
          <div style={{ fontSize: 11.5, color: 'var(--text-2)', marginTop: 2 }}>"Auto to MG Road ₹180" — type, dictate, snap a receipt</div>
        </div>
        <Icon name="arrowRight" size={18} color="var(--teal-300)"/>
      </button>
    </div>
  );
}

/* ───── Dashboard composition ───── */
function ScreenDashboard({ onTab, onAdd, onAi, onTransactions, onAccounts }) {
  return (
    <div style={{ paddingBottom: 100 }}>
      <DashHeader user={SAMPLE.user} date="Fri, 22 May"/>
      <NetPositionHero
        value={SAMPLE.netPosition}
        change={SAMPLE.changeAmount}
        changePct={SAMPLE.changePct}
        spark={SAMPLE.spark30}
      />
      <FlowStrip income={SAMPLE.income} expense={SAMPLE.expense} budget={SAMPLE.budget}/>
      <AIEntryCard onOpen={onAi}/>
      <div style={{ height: 12 }}/>
      <AccountsRow accounts={SAMPLE.accounts} onAdd={onAccounts}/>
      <RecentList items={SAMPLE.recent} onViewAll={onTransactions}/>
    </div>
  );
}

Object.assign(window, {
  SAMPLE, NetPositionHero, FlowStrip, AccountsRow, AccountChip,
  RecentList, TransactionRow, DashHeader, AIEntryCard, ScreenDashboard, Sparkline, Breakdown,
});
