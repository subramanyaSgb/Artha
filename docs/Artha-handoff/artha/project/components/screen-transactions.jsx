// ─────────────────────────────────────────────────────────────
// Screen — Transactions (ledger tab)
// Smart filters, day-grouped list, balance running totals option
// ─────────────────────────────────────────────────────────────

const FULL_TXNS = [
  // Today
  { id: 't01', desc: 'Blue Tokai — Indiranagar', cat: 'Food & Drink', icon: 'receipt', amt: -420, date: '2026-05-22', time: '9:12 AM', acc: 'HDFC •8842', app: 'GPay', tag: ['coffee'], type: 'expense' },
  { id: 't02', desc: 'Uber — Domlur to MG Rd', cat: 'Transport', icon: 'arrowSwap', amt: -340, date: '2026-05-22', time: '8:30 AM', acc: 'GPay → HDFC', app: 'GPay', tag: [], type: 'expense' },
  // Yesterday
  { id: 't03', desc: 'Salary — Acme Corp', cat: 'Income', icon: 'arrowDown', amt: 92000, date: '2026-05-21', time: '11:00 AM', acc: 'HDFC Savings', app: 'NEFT', tag: [], type: 'income' },
  { id: 't04', desc: 'Swiggy — Tibetan Spoon', cat: 'Food & Drink', icon: 'receipt', amt: -680, date: '2026-05-21', time: '8:40 PM', acc: 'GPay → HDFC', app: 'GPay', tag: ['dinner'], type: 'expense' },
  { id: 't05', desc: 'Transfer to Niyo', cat: 'Transfer', icon: 'arrowSwap', amt: -5000, date: '2026-05-21', time: '6:15 PM', acc: 'HDFC → Niyo', app: 'IMPS', tag: [], type: 'transfer' },
  // 21 May
  { id: 't06', desc: 'Auto to MG Road', cat: 'Transport', icon: 'arrowSwap', amt: -180, date: '2026-05-20', time: '7:20 AM', acc: 'Cash', app: 'Cash', tag: [], type: 'expense' },
  { id: 't07', desc: 'Zerodha — Nifty 50 SIP', cat: 'Investment', icon: 'chart', amt: -10000, date: '2026-05-20', time: '10:00 AM', acc: 'HDFC → Zerodha', app: 'NEFT', tag: ['sip'], type: 'investment' },
  { id: 't08', desc: 'BESCOM Electricity', cat: 'Bills & Utilities', icon: 'fire', amt: -2840, date: '2026-05-19', time: '4:30 PM', acc: 'GPay → ICICI', app: 'GPay', tag: [], type: 'expense' },
  { id: 't09', desc: 'Spotify — Family', cat: 'Subscriptions', icon: 'play', amt: -179, date: '2026-05-19', time: '12:00 PM', acc: 'Axis Magnus •3309', app: 'CRED', tag: [], type: 'expense' },
  { id: 't10', desc: 'Rent — May', cat: 'Home', icon: 'home', amt: -38000, date: '2026-05-18', time: '10:00 AM', acc: 'HDFC → Landlord', app: 'NEFT', tag: ['fixed'], type: 'expense' },
  { id: 't11', desc: 'Bigbasket groceries', cat: 'Food & Drink', icon: 'receipt', amt: -3240, date: '2026-05-17', time: '7:45 PM', acc: 'HDFC Regalia •8842', app: 'BHIM', tag: [], type: 'expense' },
  { id: 't12', desc: 'Refund — Myntra return', cat: 'Income', icon: 'arrowDown', amt: 1899, date: '2026-05-17', time: '3:20 PM', acc: 'HDFC Regalia •8842', app: 'CRED', tag: [], type: 'income' },
];

function fmtDay(date, today = '2026-05-22') {
  const d = new Date(date), now = new Date(today);
  const diff = Math.round((now - d) / (1000 * 60 * 60 * 24));
  if (diff === 0) return 'Today';
  if (diff === 1) return 'Yesterday';
  const opts = { weekday: 'short', day: 'numeric', month: 'short' };
  return d.toLocaleDateString('en-IN', opts);
}

function ScreenTransactions({ user, onSearch, onAdd, onAi }) {
  const [type, setType] = useState('all');
  const [period, setPeriod] = useState('this-month');
  const [query, setQuery] = useState('');

  const filtered = useMemo(() => {
    return FULL_TXNS.filter(t => {
      if (type !== 'all' && t.type !== type) return false;
      if (query && !t.desc.toLowerCase().includes(query.toLowerCase())) return false;
      return true;
    });
  }, [type, query]);

  // group by date
  const grouped = filtered.reduce((acc, t) => {
    (acc[t.date] ??= []).push(t); return acc;
  }, {});

  // totals
  const inSum = filtered.filter(t => t.amt > 0).reduce((a, t) => a + t.amt, 0);
  const outSum = filtered.filter(t => t.amt < 0).reduce((a, t) => a + Math.abs(t.amt), 0);

  return (
    <div style={{ paddingBottom: 100 }}>
      <DashHeader user={user} date="Fri, 22 May"/>

      {/* Page title + sort */}
      <div className="row between" style={{ padding: '4px 20px 12px' }}>
        <div>
          <div className="eyebrow" style={{ marginBottom: 4 }}>The Ledger</div>
          <div style={{ fontSize: 26, fontFamily: 'var(--font-display)', color: 'var(--text-1)', letterSpacing: '-0.01em', whiteSpace: 'nowrap' }}>This month</div>
        </div>
        <button className="touch" style={{
          width: 40, height: 40, borderRadius: 12,
          background: 'var(--surface-2)', border: '1px solid var(--line-1)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }}>
          <Icon name="filter" size={18} color="var(--text-1)"/>
        </button>
      </div>

      {/* In/Out/Net totals strip */}
      <div style={{ margin: '0 16px 14px', padding: '12px 14px', background: 'var(--surface-2)', border: '1px solid var(--line-1)', borderRadius: 16, display: 'grid', gridTemplateColumns: '1fr 1fr 1.1fr', gap: 12 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 6, height: 6, borderRadius: 3, background: 'var(--income)' }}/>
            <div className="eyebrow">In</div>
          </div>
          <div className="num-display tnum" style={{ fontSize: 20, color: 'var(--income)', marginTop: 4, letterSpacing: '-0.01em', whiteSpace: 'nowrap' }}>{fmtINR(inSum)}</div>
        </div>
        <div style={{ borderLeft: '1px solid var(--line-1)', paddingLeft: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 6, height: 6, borderRadius: 3, background: 'var(--expense)' }}/>
            <div className="eyebrow">Out</div>
          </div>
          <div className="num-display tnum" style={{ fontSize: 20, color: 'var(--text-1)', marginTop: 4, letterSpacing: '-0.01em', whiteSpace: 'nowrap' }}>{fmtINR(outSum)}</div>
        </div>
        <div style={{ borderLeft: '1px solid var(--line-1)', paddingLeft: 12 }}>
          <div className="eyebrow">Net</div>
          <div className="num-display tnum" style={{ fontSize: 20, color: 'var(--text-1)', marginTop: 4, letterSpacing: '-0.01em', whiteSpace: 'nowrap' }}>{fmtINR(inSum - outSum, { sign: true })}</div>
        </div>
      </div>

      {/* Search */}
      <div style={{ margin: '0 16px 12px', position: 'relative' }}>
        <div style={{ position: 'absolute', left: 14, top: 14, color: 'var(--text-3)' }}>
          <Icon name="search" size={18} color="var(--text-3)"/>
        </div>
        <input
          value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Find merchant, note, amount…"
          className="input" style={{ paddingLeft: 42, height: 44, fontSize: 14 }}
        />
      </div>

      {/* Type chips */}
      <div style={{ paddingBottom: 6 }}>
        <ChipGroup
          scroll
          value={type}
          onChange={setType}
          options={[
            { value: 'all', label: 'All' },
            { value: 'expense', label: 'Expense' },
            { value: 'income', label: 'Income' },
            { value: 'transfer', label: 'Transfer' },
            { value: 'investment', label: 'Investment' },
          ]}
        />
      </div>

      {/* Active filter chips row */}
      <div style={{ display: 'flex', gap: 8, padding: '8px 20px 4px', overflowX: 'auto' }} className="phone-content">
        <FilterChip icon="calendar" label="This month" active/>
        <FilterChip icon="bank" label="All accounts"/>
        <FilterChip icon="category" label="All categories"/>
        <FilterChip icon="tag" label="Tags"/>
      </div>

      {/* List */}
      <div style={{ padding: '0 16px' }}>
        {Object.entries(grouped).map(([date, list]) => (
          <div key={date}>
            <div className="row between" style={{ padding: '14px 4px 8px' }}>
              <div style={{ fontSize: 11, color: 'var(--text-3)', letterSpacing: '0.08em', textTransform: 'uppercase', fontWeight: 600 }}>
                {fmtDay(date)}
              </div>
              <div className="num-mono tnum" style={{ fontSize: 11, color: 'var(--text-3)' }}>
                {fmtINR(list.reduce((a, t) => a + t.amt, 0), { sign: true })}
              </div>
            </div>
            <div className="card-flush" style={{ overflow: 'hidden' }}>
              {list.map((t, i) => (
                <div key={t.id}>
                  <TransactionRow t={{ ...t, when: t.time }}/>
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

function FilterChip({ icon, label, active }) {
  return (
    <button className={'chip' + (active ? ' active' : '')}>
      <Icon name={icon} size={13} color="currentColor" weight={1.6}/>
      {label}
      <Icon name="chevronDown" size={12} color="currentColor" weight={1.6}/>
    </button>
  );
}

Object.assign(window, { ScreenTransactions, FULL_TXNS, fmtDay, FilterChip });
