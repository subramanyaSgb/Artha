// ─────────────────────────────────────────────────────────────
// Phone shell + chrome (top bar, bottom nav, FAB, sheet host)
// ─────────────────────────────────────────────────────────────

/* PhoneShell — custom dark Android frame, 412×892 viewport */
function PhoneShell({ children, overlays, statusBg }) {
  return (
    <div style={{
      width: 412, height: 892, borderRadius: 44,
      background: '#000',
      border: '6px solid #2a2a2a',
      boxShadow: '0 40px 80px -20px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.04) inset',
      overflow: 'hidden',
      display: 'flex', flexDirection: 'column',
      position: 'relative',
    }}>
      <PhoneStatusBar bg={statusBg} />
      <div className="phone-content" style={{
        flex: 1, overflow: 'auto',
        background: 'var(--surface-1)',
        display: 'flex', flexDirection: 'column',
      }}>
        {children}
      </div>
      <PhoneNavBar />
      {/* Overlays — positioned absolute relative to the phone frame, not the scroll area */}
      {overlays}
    </div>
  );
}

function PhoneStatusBar({ bg = 'transparent' }) {
  return (
    <div style={{
      height: 36, display: 'flex', alignItems: 'center',
      justifyContent: 'space-between', padding: '0 24px',
      background: bg, color: 'var(--text-1)',
      fontSize: 13, fontWeight: 500,
      fontFamily: 'var(--font-ui)',
      letterSpacing: '0.01em',
      position: 'relative', flexShrink: 0,
    }}>
      <span>9:41</span>
      {/* center pill cutout */}
      <div style={{
        position: 'absolute', left: '50%', top: 6, transform: 'translateX(-50%)',
        width: 88, height: 24, borderRadius: 12, background: '#000',
      }} />
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        {/* signal */}
        <svg width="16" height="12" viewBox="0 0 16 12" fill="currentColor">
          <rect x="0" y="8" width="2.5" height="4" rx="0.5"/>
          <rect x="4" y="6" width="2.5" height="6" rx="0.5"/>
          <rect x="8" y="3" width="2.5" height="9" rx="0.5"/>
          <rect x="12" y="0" width="2.5" height="12" rx="0.5" opacity="0.4"/>
        </svg>
        {/* wifi */}
        <svg width="15" height="12" viewBox="0 0 15 12" fill="none" stroke="currentColor" strokeWidth="1.4">
          <path d="M1 4.5 Q7.5 -0.5 14 4.5"/>
          <path d="M3.5 7 Q7.5 3 11.5 7"/>
          <circle cx="7.5" cy="9.5" r="1" fill="currentColor"/>
        </svg>
        {/* battery */}
        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: 1,
        }}>
          <div style={{
            width: 22, height: 11, borderRadius: 3,
            border: '1px solid currentColor', position: 'relative',
            padding: 1.5, boxSizing: 'border-box',
          }}>
            <div style={{ width: '70%', height: '100%', background: 'currentColor', borderRadius: 1 }} />
          </div>
          <div style={{ width: 2, height: 5, background: 'currentColor', borderRadius: '0 1px 1px 0', marginLeft: -.5 }} />
        </div>
      </div>
    </div>
  );
}

function PhoneNavBar() {
  return (
    <div style={{
      height: 18, display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'transparent', flexShrink: 0,
    }}>
      <div style={{ width: 124, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.35)' }}/>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Bottom tab bar
   ─────────────────────────────────────────────────────────── */
function BottomTabs({ active, onChange }) {
  const tabs = [
    { id: 'home', label: 'Home', icon: 'home' },
    { id: 'transactions', label: 'Ledger', icon: 'list' },
    { id: 'accounts', label: 'Accounts', icon: 'bank' },
    { id: 'cards', label: 'Cards', icon: 'card' },
    { id: 'more', label: 'More', icon: 'more' },
  ];
  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 18,
      background: 'rgba(7, 16, 13, 0.92)',
      backdropFilter: 'blur(20px)',
      borderTop: '1px solid var(--line-1)',
      display: 'flex', justifyContent: 'space-around',
      padding: '6px 8px 10px',
      zIndex: 'var(--z-nav)',
    }}>
      {tabs.map(t => {
        const isActive = active === t.id;
        return (
          <button key={t.id} className="touch" onClick={() => onChange(t.id)} style={{
            background: 'transparent', border: 'none', padding: 0,
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
            cursor: 'pointer', flex: 1, minWidth: 0,
          }}>
            <div style={{
              width: 56, height: 28, borderRadius: 14,
              background: isActive ? 'var(--teal-900)' : 'transparent',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              transition: 'background .15s',
            }}>
              <Icon name={t.icon} size={22} color={isActive ? 'var(--teal-300)' : 'var(--text-2)'} weight={isActive ? 2 : 1.6}/>
            </div>
            <span style={{
              fontSize: 11, fontWeight: 500,
              color: isActive ? 'var(--teal-300)' : 'var(--text-2)',
              fontFamily: 'var(--font-ui)',
            }}>{t.label}</span>
          </button>
        );
      })}
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Floating Action Button
   ─────────────────────────────────────────────────────────── */
function FAB({ onClick, icon = 'plus', label, extended = false, color = 'var(--teal-700)', bottom = 110 }) {
  return (
    <button onClick={onClick} className="touch" style={{
      position: 'absolute', bottom, right: 20,
      height: 56, width: extended ? 'auto' : 56,
      padding: extended ? '0 22px 0 18px' : 0,
      borderRadius: 18,
      background: color,
      color: '#fff',
      border: '1px solid rgba(255,255,255,0.08)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
      cursor: 'pointer', zIndex: 'var(--z-fab)',
      boxShadow: '0 12px 30px -8px rgba(15, 118, 110, 0.6), 0 0 0 1px rgba(20,184,166,0.2)',
      fontFamily: 'var(--font-ui)', fontSize: 14, fontWeight: 600,
    }}>
      <Icon name={icon} size={22} color="#fff" weight={2}/>
      {extended && <span>{label}</span>}
    </button>
  );
}

/* ─────────────────────────────────────────────────────────────
   Top bar — used by sub-screens (back arrow + title)
   ─────────────────────────────────────────────────────────── */
function TopBar({ title, onBack, right }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 4,
      padding: '14px 8px 14px 4px', minHeight: 56,
      background: 'var(--surface-1)',
    }}>
      {onBack && (
        <button className="touch" onClick={onBack} style={{
          width: 44, height: 44, borderRadius: 22,
          background: 'transparent', border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon name="back" size={22} color="var(--text-1)" weight={1.8}/>
        </button>
      )}
      <div style={{
        flex: 1, fontSize: 20, fontWeight: 600,
        color: 'var(--text-1)', fontFamily: 'var(--font-ui)',
        letterSpacing: '-0.01em',
        paddingLeft: onBack ? 0 : 16,
      }}>{title}</div>
      {right && <div style={{ paddingRight: 4 }}>{right}</div>}
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Bottom sheet (modal) — for Add flows + AI Quick Entry
   ─────────────────────────────────────────────────────────── */
function Sheet({ open, onClose, children, title, height = '88%', tall = false }) {
  if (!open) return null;
  return (
    <div style={{
      position: 'absolute', inset: 0, zIndex: 'var(--z-sheet)',
      display: 'flex', flexDirection: 'column',
    }}>
      {/* scrim */}
      <div onClick={onClose} style={{
        position: 'absolute', inset: 0,
        background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(4px)',
        animation: 'fadeIn .2s ease',
      }} />
      {/* sheet */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        height: tall ? '94%' : height,
        background: 'var(--surface-3)',
        borderRadius: '24px 24px 0 0',
        borderTop: '1px solid var(--line-2)',
        display: 'flex', flexDirection: 'column',
        animation: 'slideUp .25s cubic-bezier(0.2, 0.8, 0.2, 1)',
        overflow: 'hidden',
      }}>
        {/* handle */}
        <div style={{ padding: '10px 0 4px', display: 'flex', justifyContent: 'center' }}>
          <div style={{ width: 36, height: 4, borderRadius: 2, background: 'var(--line-3)' }}/>
        </div>
        {title && (
          <div style={{
            padding: '8px 20px 8px',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          }}>
            <div style={{ fontSize: 22, fontWeight: 600, color: 'var(--text-1)', letterSpacing: '-0.01em' }}>{title}</div>
            <button onClick={onClose} className="touch" style={{
              width: 36, height: 36, borderRadius: 18,
              background: 'var(--surface-2)', border: 'none', cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon name="close" size={18} color="var(--text-1)" weight={1.8}/>
            </button>
          </div>
        )}
        <div style={{ flex: 1, overflow: 'auto', padding: '8px 20px 20px' }} className="phone-content">
          {children}
        </div>
      </div>
      <style>{`
        @keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
        @keyframes slideUp { from { transform: translateY(100%) } to { transform: translateY(0) } }
      `}</style>
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Section header — recurring throughout the app
   ─────────────────────────────────────────────────────────── */
function SectionHeader({ title, action, actionOnClick, ornament = true }) {
  return (
    <div className="row between" style={{ padding: '8px 20px 8px', minHeight: 36 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {ornament && (
          <div style={{ width: 14, height: 1, background: 'var(--teal-500)', opacity: 0.6 }}/>
        )}
        <div className="eyebrow" style={{ color: 'var(--text-2)' }}>{title}</div>
      </div>
      {action && (
        <button onClick={actionOnClick} className="touch" style={{
          background: 'transparent', border: 'none', color: 'var(--teal-300)',
          fontSize: 13, fontWeight: 500, padding: '4px 8px', borderRadius: 6, cursor: 'pointer',
          fontFamily: 'var(--font-ui)',
        }}>{action}</button>
      )}
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────
   Segmented chip group (used in many filters)
   ─────────────────────────────────────────────────────────── */
function ChipGroup({ options, value, onChange, scroll = false, style }) {
  return (
    <div style={{
      display: 'flex', gap: 8, padding: scroll ? '0 20px' : 0,
      overflowX: scroll ? 'auto' : 'visible',
      ...style,
    }} className="phone-content">
      {options.map(o => (
        <button key={o.value ?? o} onClick={() => onChange(o.value ?? o)}
          className={'chip' + ((value === (o.value ?? o)) ? ' active' : '')}>
          {o.label ?? o}
        </button>
      ))}
    </div>
  );
}

Object.assign(window, {
  PhoneShell, PhoneStatusBar, PhoneNavBar,
  BottomTabs, FAB, TopBar, Sheet, SectionHeader, ChipGroup,
});
