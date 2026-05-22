// ─────────────────────────────────────────────────────────────
// Patterns & icon primitives for Artha
// - SVG patterns (jaali grid, block-print, dotted)
// - Material-style icons drawn as inline SVG (no third-party)
// - Devanagari brand glyph
// ─────────────────────────────────────────────────────────────

/* Jaali — temple lattice pattern. Subtle. Use as background fill. */
function PatternDefs() {
  return (
    <svg width="0" height="0" style={{ position: 'absolute' }} aria-hidden>
      <defs>
        {/* Jaali — 8-fold geometric lattice */}
        <pattern id="p-jaali" x="0" y="0" width="32" height="32" patternUnits="userSpaceOnUse">
          <g fill="none" stroke="currentColor" strokeWidth="0.5" opacity="0.5">
            <circle cx="16" cy="16" r="14" />
            <circle cx="0" cy="0" r="14" />
            <circle cx="32" cy="0" r="14" />
            <circle cx="0" cy="32" r="14" />
            <circle cx="32" cy="32" r="14" />
          </g>
        </pattern>
        {/* Block-print dot grid */}
        <pattern id="p-blockprint" x="0" y="0" width="12" height="12" patternUnits="userSpaceOnUse">
          <circle cx="6" cy="6" r="0.7" fill="currentColor" opacity="0.6" />
          <circle cx="0" cy="0" r="0.4" fill="currentColor" opacity="0.3" />
          <circle cx="12" cy="0" r="0.4" fill="currentColor" opacity="0.3" />
          <circle cx="0" cy="12" r="0.4" fill="currentColor" opacity="0.3" />
          <circle cx="12" cy="12" r="0.4" fill="currentColor" opacity="0.3" />
        </pattern>
        {/* Bandhani — small flower repeat */}
        <pattern id="p-bandhani" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
          <g fill="currentColor" opacity="0.4">
            <circle cx="10" cy="10" r="1.2" />
            <circle cx="10" cy="4" r="0.5" />
            <circle cx="10" cy="16" r="0.5" />
            <circle cx="4" cy="10" r="0.5" />
            <circle cx="16" cy="10" r="0.5" />
          </g>
        </pattern>
        {/* Note paper grain */}
        <pattern id="p-paper" x="0" y="0" width="80" height="80" patternUnits="userSpaceOnUse">
          <rect width="80" height="80" fill="none" />
          <g opacity="0.15">
            <line x1="0" y1="20" x2="80" y2="22" stroke="currentColor" strokeWidth="0.3" />
            <line x1="0" y1="40" x2="80" y2="38" stroke="currentColor" strokeWidth="0.3" />
            <line x1="0" y1="60" x2="80" y2="62" stroke="currentColor" strokeWidth="0.3" />
          </g>
        </pattern>
      </defs>
    </svg>
  );
}

/* Chhatri silhouette — dome with finial, used in empty states */
function Chhatri({ size = 64, color = 'currentColor' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" fill="none">
      {/* base platform */}
      <rect x="6" y="52" width="52" height="3" fill={color} opacity="0.8" />
      {/* columns */}
      <rect x="10" y="32" width="2" height="20" fill={color} opacity="0.6" />
      <rect x="20" y="32" width="2" height="20" fill={color} opacity="0.6" />
      <rect x="42" y="32" width="2" height="20" fill={color} opacity="0.6" />
      <rect x="52" y="32" width="2" height="20" fill={color} opacity="0.6" />
      {/* lintel */}
      <rect x="6" y="29" width="52" height="3" fill={color} opacity="0.8" />
      {/* dome */}
      <path d="M10 29 Q32 4 54 29 Z" fill={color} opacity="0.85" />
      {/* finial */}
      <line x1="32" y1="2" x2="32" y2="8" stroke={color} strokeWidth="1.5" />
      <circle cx="32" cy="2" r="1.2" fill={color} />
    </svg>
  );
}

/* Devanagari ardh-chandra (half-moon) — used as section ornament */
function ArdhChandra({ size = 12, color = 'currentColor' }) {
  return (
    <svg width={size} height={size * 0.5} viewBox="0 0 24 12" fill="none">
      <path d="M2 10 Q12 -2 22 10" stroke={color} strokeWidth="1.2" fill="none" />
      <circle cx="12" cy="3" r="1" fill={color} />
    </svg>
  );
}

/* Devanagari numerals (०१२३४५६७८९) */
const DEV_NUMERALS = ['०','१','२','३','४','५','६','७','८','९'];
function toDeva(n) {
  return String(n).split('').map(c => DEV_NUMERALS[parseInt(c, 10)] ?? c).join('');
}

/* ─────────────────────────────────────────────────────────────
   Indian number formatter — 1,23,456 (2,2,3 grouping)
   ─────────────────────────────────────────────────────────── */
function fmtINR(amount, { sign = false, compact = false } = {}) {
  const n = Math.abs(Math.round(amount));
  let s;
  if (compact && n >= 10000000) s = (n / 10000000).toFixed(n >= 100000000 ? 0 : 2).replace(/\.?0+$/, '') + ' Cr';
  else if (compact && n >= 100000) s = (n / 100000).toFixed(n >= 1000000 ? 0 : 2).replace(/\.?0+$/, '') + ' L';
  else {
    const str = String(n);
    if (str.length <= 3) s = str;
    else {
      const last3 = str.slice(-3);
      const rest = str.slice(0, -3);
      const grouped = rest.replace(/\B(?=(\d{2})+(?!\d))/g, ',');
      s = grouped + ',' + last3;
    }
  }
  const prefix = sign ? (amount < 0 ? '–' : '+') : (amount < 0 ? '–' : '');
  return prefix + '₹' + s;
}

/* ─────────────────────────────────────────────────────────────
   Icons — minimal stroke set, Material 3 inspired
   All accept { size, color, weight }
   ─────────────────────────────────────────────────────────── */
function Icon({ name, size = 22, color = 'currentColor', weight = 1.6 }) {
  const p = { stroke: color, strokeWidth: weight, fill: 'none', strokeLinecap: 'round', strokeLinejoin: 'round' };
  const filledP = { fill: color };
  const paths = {
    // Tab bar
    home: <><path d="M3 11l9-8 9 8" {...p}/><path d="M5 10v10h14V10" {...p}/></>,
    list: <><path d="M4 7h16M4 12h16M4 17h10" {...p}/></>,
    bank: <><path d="M3 10h18L12 4 3 10z" {...p}/><path d="M5 10v8M9 10v8M15 10v8M19 10v8M3 20h18" {...p}/></>,
    card: <><rect x="3" y="6" width="18" height="13" rx="2" {...p}/><path d="M3 11h18" {...p}/></>,
    more: <><circle cx="6" cy="12" r="1.2" {...filledP}/><circle cx="12" cy="12" r="1.2" {...filledP}/><circle cx="18" cy="12" r="1.2" {...filledP}/></>,

    // Actions
    plus: <><path d="M12 5v14M5 12h14" {...p}/></>,
    close: <><path d="M6 6l12 12M18 6L6 18" {...p}/></>,
    back: <><path d="M15 18l-6-6 6-6" {...p}/></>,
    chevronDown: <><path d="M6 9l6 6 6-6" {...p}/></>,
    chevronRight: <><path d="M9 6l6 6-6 6" {...p}/></>,
    chevronUp: <><path d="M6 15l6-6 6 6" {...p}/></>,
    search: <><circle cx="11" cy="11" r="7" {...p}/><path d="M20 20l-3.5-3.5" {...p}/></>,
    filter: <><path d="M3 6h18M6 12h12M10 18h4" {...p}/></>,
    edit: <><path d="M4 20h4l11-11-4-4L4 16v4z" {...p}/></>,
    trash: <><path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13" {...p}/></>,
    settings: <><circle cx="12" cy="12" r="3" {...p}/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3 1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8 1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z" {...p}/></>,

    // Domain
    receipt: <><path d="M6 3v18l3-2 3 2 3-2 3 2V3l-3 2-3-2-3 2-3-2z" {...p}/><path d="M9 9h6M9 13h6" {...p}/></>,
    wallet: <><path d="M3 7h15a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V7z" {...p}/><path d="M3 7l1-2h13l1 2" {...p}/><circle cx="17" cy="14" r="1.2" {...filledP}/></>,
    piggy: <><path d="M19 11a7 7 0 0 0-7-7c-3 0-5.5 1.7-6.5 4H4a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1h.5c.5 1.5 1.5 2.7 3 3.5V18a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1v-1h2v1a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1v-2.5c1.2-.8 2-2 2.3-3.5H20a1 1 0 0 0 1-1V11h-2z" {...p}/><circle cx="16" cy="10" r="0.8" {...filledP}/></>,
    shield: <><path d="M12 3l8 3v5c0 5-3.5 9-8 10-4.5-1-8-5-8-10V6l8-3z" {...p}/></>,
    flag: <><path d="M5 21V4M5 4h12l-2 4 2 4H5" {...p}/></>,
    refresh: <><path d="M3 12a9 9 0 0 1 15-6.7L21 8M21 3v5h-5M21 12a9 9 0 0 1-15 6.7L3 16M3 21v-5h5" {...p}/></>,
    people: <><circle cx="9" cy="8" r="3" {...p}/><circle cx="17" cy="10" r="2.5" {...p}/><path d="M3 20c0-3 2.5-5 6-5s6 2 6 5M15 20c0-2 1.5-3.5 4-3.5s4 1.5 4 3.5" {...p}/></>,
    chart: <><path d="M3 20h18M6 17V11M11 17V6M16 17v-8M21 17v-4" {...p}/></>,
    play: <><path d="M9 6v12l9-6z" {...filledP}/></>,
    mic: <><rect x="9" y="3" width="6" height="11" rx="3" {...p}/><path d="M5 11a7 7 0 0 0 14 0M12 18v3" {...p}/></>,
    image: <><rect x="3" y="4" width="18" height="16" rx="2" {...p}/><circle cx="9" cy="10" r="1.5" {...p}/><path d="M3 17l5-4 4 3 4-5 5 6" {...p}/></>,
    send: <><path d="M3 11l18-8-7 18-2-8-9-2z" {...p}/></>,
    sparkles: <><path d="M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5L12 3zM19 14l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7.7-2zM5 16l.5 1.5L7 18l-1.5.5L5 20l-.5-1.5L3 18l1.5-.5L5 16z" {...filledP}/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2" {...p}/><path d="M3 9h18M8 3v4M16 3v4" {...p}/></>,
    clock: <><circle cx="12" cy="12" r="9" {...p}/><path d="M12 7v5l3 2" {...p}/></>,
    tag: <><path d="M3 12V3h9l9 9-9 9-9-9z" {...p}/><circle cx="8" cy="8" r="1.2" {...filledP}/></>,
    category: <><circle cx="7" cy="7" r="3" {...p}/><rect x="14" y="4" width="6" height="6" rx="1" {...p}/><path d="M4 17l3-5 3 5zM14 14h6v6h-6z" {...p}/></>,
    notification: <><path d="M6 8a6 6 0 0 1 12 0v5l1.5 3h-15L6 13V8z" {...p}/><path d="M10 19a2 2 0 0 0 4 0" {...p}/></>,
    location: <><path d="M12 22s7-7 7-12a7 7 0 0 0-14 0c0 5 7 12 7 12z" {...p}/><circle cx="12" cy="10" r="2.5" {...p}/></>,
    arrowUp: <><path d="M12 19V5M5 12l7-7 7 7" {...p}/></>,
    arrowDown: <><path d="M12 5v14M5 12l7 7 7-7" {...p}/></>,
    arrowRight: <><path d="M5 12h14M13 5l7 7-7 7" {...p}/></>,
    arrowSwap: <><path d="M7 7h13l-3-3M17 17H4l3 3" {...p}/></>,
    check: <><path d="M5 12l4 4 10-10" {...p}/></>,
    dot: <circle cx="12" cy="12" r="3" {...filledP}/>,
    namaste: <><path d="M6 21V13a3 3 0 0 1 6 0v3M18 21V13a3 3 0 0 0-6 0v3M12 13l-2-8M12 13l2-8" {...p}/></>,
    fire: <><path d="M12 3c0 5-5 5-5 10a5 5 0 0 0 10 0c0-3-2-4-2-7-1 1-2 1-3-3z" {...p}/></>,
    rules: <><path d="M4 6h12M4 12h8M4 18h10M18 5l2 2-6 6-2-2 6-6zM18 17l2 2-2 2-2-2 2-2z" {...p}/></>,
    info: <><circle cx="12" cy="12" r="9" {...p}/><path d="M12 11v6M12 8h.01" {...p}/></>,
    eye: <><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z" {...p}/><circle cx="12" cy="12" r="3" {...p}/></>,
    download: <><path d="M12 3v13M5 12l7 7 7-7M5 21h14" {...p}/></>,
    upload: <><path d="M12 21V8M5 12l7-7 7 7M5 3h14" {...p}/></>,
    link: <><path d="M10 14a4 4 0 0 0 6 0l3-3a4 4 0 0 0-6-6l-1 1M14 10a4 4 0 0 0-6 0l-3 3a4 4 0 0 0 6 6l1-1" {...p}/></>,
  };
  const path = paths[name];
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden style={{ flexShrink: 0 }}>
      {path}
    </svg>
  );
}

/* Devanagari brand glyph in teal — used as logo or accent */
function BrandMark({ size = 32, bg = 'var(--teal-700)', color = '#fff', radius }) {
  const r = radius ?? Math.round(size * 0.22);
  return (
    <div style={{
      width: size, height: size, borderRadius: r,
      background: bg, color, display: 'flex',
      alignItems: 'center', justifyContent: 'center',
      fontFamily: 'var(--font-deva)', fontWeight: 400,
      fontSize: Math.round(size * 0.6), lineHeight: 1, paddingBottom: Math.round(size * 0.04),
      flexShrink: 0,
    }}>अ</div>
  );
}

Object.assign(window, {
  PatternDefs, Chhatri, ArdhChandra, toDeva, DEV_NUMERALS, fmtINR, Icon, BrandMark,
});
