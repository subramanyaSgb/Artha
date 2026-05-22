// ─────────────────────────────────────────────────────────────
// Screen — Accounts (full grid view)
// Screen — Cards (credit card stack)
// ─────────────────────────────────────────────────────────────

function ScreenAccounts({ user, onAdd, accounts = SAMPLE.accounts }) {
  const totalLiquid = accounts.reduce((a, x) => a + x.bal, 0);
  return (
    <div style={{ paddingBottom: 100 }}>
      <DashHeader user={user} date="Fri, 22 May"/>

      <div style={{ padding: '4px 20px 16px' }}>
        <div className="eyebrow" style={{ marginBottom: 4 }}>Where your money sits</div>
        <div style={{ fontSize: 26, fontFamily: 'var(--font-display)', color: 'var(--text-1)', letterSpacing: '-0.01em' }}>
          Accounts
        </div>
      </div>

      {/* Total liquid card */}
      <div style={{
        margin: '0 16px 16px', padding: '18px 20px',
        background: 'var(--surface-2)', border: '1px solid var(--line-1)',
        borderRadius: 18, position: 'relative', overflow: 'hidden',
      }}>
        <div style={{ position: 'absolute', inset: 0, color: 'var(--teal-500)', opacity: 0.04, pointerEvents: 'none' }}>
          <svg width="100%" height="100%"><rect width="100%" height="100%" fill="url(#p-bandhani)"/></svg>
        </div>
        <div style={{ position: 'relative' }}>
          <div className="eyebrow">Total liquid</div>
          <div className="num-display tnum" style={{ fontSize: 40, marginTop: 4, color: 'var(--text-1)', fontWeight: 300, letterSpacing: '-0.02em' }}>{fmtINR(totalLiquid)}</div>
          <div style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 4, fontFamily: 'var(--font-mono)' }} className="tnum">
            across {accounts.length} accounts
          </div>
        </div>
      </div>

      {/* Accounts list */}
      <div style={{ padding: '0 16px' }}>
        {accounts.map(a => (
          <AccountRow key={a.id} acc={a}/>
        ))}
        <button onClick={onAdd} className="touch" style={{
          width: '100%', marginTop: 10,
          padding: '16px',
          background: 'transparent', border: '1px dashed var(--line-2)',
          borderRadius: 16,
          color: 'var(--text-2)', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          fontFamily: 'var(--font-ui)', fontSize: 14, fontWeight: 500,
        }}>
          <Icon name="plus" size={18} color="currentColor"/>
          New account
        </button>
      </div>
    </div>
  );
}

function AccountRow({ acc, onClick }) {
  return (
    <div onClick={onClick} className="touch card" style={{
      padding: '14px 16px', marginBottom: 10,
      display: 'flex', alignItems: 'center', gap: 14,
    }}>
      {/* Color swatch */}
      <div style={{
        width: 44, height: 44, borderRadius: 13,
        background: `linear-gradient(140deg, ${acc.color} 0%, color-mix(in srgb, ${acc.color} 65%, #000) 100%)`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0, position: 'relative', overflow: 'hidden',
      }}>
        <svg width="100%" height="100%" style={{ position: 'absolute', inset: 0, opacity: 0.15 }}>
          <rect width="100%" height="100%" fill="url(#p-bandhani)" color="white"/>
        </svg>
        <Icon name={acc.glyph} size={20} color="#fff" weight={1.8}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-1)' }}>{acc.name}</div>
        <div style={{ marginTop: 2, fontSize: 11.5, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">
          {acc.type}{acc.tag ? ` • ${acc.tag}` : ''}
        </div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <div className="num-display tnum" style={{ fontSize: 20, color: 'var(--text-1)', letterSpacing: '-0.01em', fontWeight: 400 }}>{fmtINR(acc.bal)}</div>
      </div>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Cards screen — stacked deck of credit cards
   ─────────────────────────────────────────────────────────── */
function ScreenCards({ user, onAdd, cards = SAMPLE.cards }) {
  const totalUsed = cards.reduce((a, c) => a + c.used, 0);
  const totalLimit = cards.reduce((a, c) => a + c.limit, 0);
  const utilPct = Math.round(totalUsed / totalLimit * 100);
  return (
    <div style={{ paddingBottom: 100 }}>
      <DashHeader user={user} date="Fri, 22 May"/>

      <div style={{ padding: '4px 20px 16px' }}>
        <div className="eyebrow" style={{ marginBottom: 4 }}>Plastic on file</div>
        <div style={{ fontSize: 26, fontFamily: 'var(--font-display)', color: 'var(--text-1)', letterSpacing: '-0.01em' }}>
          Cards
        </div>
      </div>

      {/* Utilization summary */}
      <div style={{
        margin: '0 16px 18px',
        padding: '16px 18px',
        background: 'var(--surface-2)', border: '1px solid var(--line-1)',
        borderRadius: 16,
      }}>
        <div className="row between">
          <div className="eyebrow">Total outstanding</div>
          <div style={{ fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">{utilPct}% util</div>
        </div>
        <div className="num-display tnum" style={{ fontSize: 32, color: 'var(--text-1)', fontWeight: 300, marginTop: 2, letterSpacing: '-0.02em' }}>{fmtINR(totalUsed)}</div>
        <div style={{ fontSize: 12, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }} className="tnum">of {fmtINR(totalLimit)} limit</div>
        {/* utilization bar */}
        <div style={{ marginTop: 10, height: 6, background: 'var(--surface-4)', borderRadius: 3, overflow: 'hidden' }}>
          <div style={{ width: `${utilPct}%`, height: '100%', background: utilPct > 30 ? 'var(--ochre)' : 'var(--teal-500)' }}/>
        </div>
      </div>

      {/* Card list */}
      <div style={{ padding: '0 16px' }}>
        {cards.map(c => <CreditCardTile key={c.id} card={c}/>)}

        <button onClick={onAdd} className="touch" style={{
          width: '100%', marginTop: 6,
          padding: '16px',
          background: 'transparent', border: '1px dashed var(--line-2)',
          borderRadius: 16,
          color: 'var(--text-2)', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          fontFamily: 'var(--font-ui)', fontSize: 14, fontWeight: 500,
        }}>
          <Icon name="plus" size={18} color="currentColor"/>
          Add card
        </button>
      </div>
    </div>
  );
}

function CreditCardTile({ card, onClick }) {
  const pct = Math.round(card.used / card.limit * 100);
  return (
    <div onClick={onClick} className="touch" style={{
      width: '100%', height: 190, marginBottom: 14,
      borderRadius: 20, padding: '20px 22px',
      background: `linear-gradient(135deg, ${card.color} 0%, color-mix(in srgb, ${card.color} 55%, #000) 100%)`,
      color: '#fff', position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
    }}>
      {/* jaali texture */}
      <svg width="100%" height="100%" style={{ position: 'absolute', inset: 0, opacity: 0.15, color: '#fff' }}>
        <rect width="100%" height="100%" fill="url(#p-jaali)"/>
      </svg>
      {/* chhatri silhouette top-right */}
      <div style={{ position: 'absolute', top: -10, right: -10, opacity: 0.10 }}>
        <Chhatri size={120} color="#fff"/>
      </div>

      <div style={{ position: 'relative', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontSize: 10, opacity: 0.8, letterSpacing: '0.14em', textTransform: 'uppercase', fontWeight: 600 }}>{card.network}</div>
          <div style={{ fontSize: 17, fontWeight: 600, marginTop: 4 }}>{card.name}</div>
        </div>
        <div style={{
          padding: '4px 8px', borderRadius: 6,
          background: 'rgba(255,255,255,0.14)',
          fontSize: 10, fontWeight: 700, letterSpacing: '0.05em',
        }}>
          DUE IN {card.due}d
        </div>
      </div>

      <div style={{ position: 'relative' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
          <div>
            <div style={{ fontSize: 10, opacity: 0.7, letterSpacing: '0.12em', textTransform: 'uppercase', fontWeight: 600 }}>Outstanding</div>
            <div className="num-display tnum" style={{ fontSize: 28, marginTop: 2, letterSpacing: '-0.01em', fontWeight: 400 }}>{fmtINR(card.used)}</div>
          </div>
          <div className="num-mono tnum" style={{ fontSize: 11, opacity: 0.85 }}>•••• {card.last4}</div>
        </div>
        {/* utilisation bar */}
        <div style={{ height: 4, background: 'rgba(255,255,255,0.18)', borderRadius: 2, overflow: 'hidden' }}>
          <div style={{ width: `${pct}%`, height: '100%', background: '#fff' }}/>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6, fontSize: 10, opacity: 0.8, fontFamily: 'var(--font-mono)' }} className="tnum">
          <span>{pct}% of {fmtINR(card.limit, { compact: true })}</span>
          <span>Limit available {fmtINR(card.limit - card.used, { compact: true })}</span>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { ScreenAccounts, AccountRow, ScreenCards, CreditCardTile });
