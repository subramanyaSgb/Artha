// ─────────────────────────────────────────────────────────────
// Sheets — Add Account, Add Card, Add Investment, Add Insurance,
// Add Goal, Add Budget, Add Subscription, Add Recurring,
// Add Person, Add Rule, Add Category, Add Tag
//
// All share the same vocabulary: AmountInput / FieldRow / PillRadio
// plus a couple of new helpers (ColorSwatch, IconChip, TextInput).
// ─────────────────────────────────────────────────────────────

function TextInput({ value, onChange, placeholder, type = 'text', large, suffix }) {
  return (
    <div style={{ position: 'relative' }}>
      <input
        type={type}
        value={value || ''}
        onChange={(e) => onChange?.(e.target.value)}
        placeholder={placeholder}
        className="input"
        style={{ height: large ? 56 : 48, fontSize: large ? 16 : 14, paddingRight: suffix ? 50 : 16 }}
      />
      {suffix && <div style={{
        position: 'absolute', right: 16, top: '50%', transform: 'translateY(-50%)',
        color: 'var(--text-3)', fontSize: 13, fontFamily: 'var(--font-mono)',
      }} className="tnum">{suffix}</div>}
    </div>
  );
}

function ColorSwatchRow({ value, onChange, swatches }) {
  return (
    <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
      {swatches.map(c => (
        <button key={c} onClick={() => onChange(c)} className="touch" style={{
          width: 30, height: 30, borderRadius: 15,
          background: c, border: 'none', cursor: 'pointer',
          outline: value === c ? '2px solid var(--text-1)' : 'none',
          outlineOffset: 2,
        }} />
      ))}
    </div>
  );
}

function IconChipRow({ value, onChange, icons }) {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {icons.map(name => (
        <button key={name} onClick={() => onChange(name)} className="touch" style={{
          width: 40, height: 40, borderRadius: 11,
          background: value === name ? 'var(--teal-900)' : 'var(--surface-2)',
          border: `1px solid ${value === name ? 'var(--teal-500)' : 'var(--line-1)'}`,
          color: value === name ? 'var(--teal-300)' : 'var(--text-2)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }}>
          <Icon name={name} size={18} color="currentColor"/>
        </button>
      ))}
    </div>
  );
}

const ACC_COLORS = ['var(--acc-teal)', 'var(--acc-indigo)', 'var(--acc-emerald)', 'var(--acc-saffron)', 'var(--acc-magenta)', 'var(--acc-violet)'];

/* ── Add Account ───────────────────────────────────────────── */
function AddAccountSheet({ onClose }) {
  const [name, setName] = useState('');
  const [type, setType] = useState('savings');
  const [inst, setInst] = useState('');
  const [last4, setLast4] = useState('');
  const [bal, setBal] = useState('');
  const [color, setColor] = useState(ACC_COLORS[0]);
  const [glyph, setGlyph] = useState('bank');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New account"/>
      <FieldRow label="Account name"><TextInput value={name} onChange={setName} placeholder="HDFC Savings"/></FieldRow>
      <FieldRow label="Type">
        <PillRadio value={type} onChange={setType} options={[
          { value: 'savings', label: 'Bank Savings' },
          { value: 'current', label: 'Bank Current' },
          { value: 'cash', label: 'Cash' },
          { value: 'wallet', label: 'Wallet' },
        ]}/>
      </FieldRow>
      <FieldRow label="Institution" optional><TextInput value={inst} onChange={setInst} placeholder="HDFC Bank"/></FieldRow>
      <FieldRow label="Last 4 digits" optional><TextInput value={last4} onChange={setLast4} placeholder="7421" type="text"/></FieldRow>
      <FieldRow label="Opening balance"><TextInput value={bal} onChange={setBal} placeholder="0" suffix="₹"/></FieldRow>
      <FieldRow label="Card color"><ColorSwatchRow value={color} onChange={setColor} swatches={ACC_COLORS}/></FieldRow>
      <FieldRow label="Icon"><IconChipRow value={glyph} onChange={setGlyph} icons={['bank', 'wallet', 'card', 'piggy', 'home']}/></FieldRow>

      {/* Preview */}
      <div style={{ marginTop: 22 }}>
        <div className="eyebrow" style={{ marginBottom: 8 }}>Preview</div>
        <AccountChip acc={{
          id: 'preview', name: name || 'My account', type: typeLabel(type),
          tag: last4 || null, bal: parseFloat(bal) || 0, color, glyph,
        }}/>
      </div>

      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save account</button>
    </div>
  );
}
function typeLabel(t) {
  return { savings: 'Bank Savings', current: 'Bank Current', cash: 'Cash', wallet: 'Wallet' }[t] || t;
}

/* ── Add Card ───────────────────────────────────────────── */
function AddCardSheet({ onClose }) {
  const [name, setName] = useState('');
  const [type, setType] = useState('credit');
  const [network, setNetwork] = useState('Visa');
  const [issuer, setIssuer] = useState('');
  const [last4, setLast4] = useState('');
  const [limit, setLimit] = useState('');
  const [stmt, setStmt] = useState('');
  const [due, setDue] = useState('');
  const [color, setColor] = useState('var(--acc-indigo)');

  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New card"/>
      <FieldRow label="Card name"><TextInput value={name} onChange={setName} placeholder="HDFC Regalia"/></FieldRow>
      <FieldRow label="Type">
        <PillRadio value={type} onChange={setType} options={[
          { value: 'credit', label: 'Credit' }, { value: 'debit', label: 'Debit' }, { value: 'prepaid', label: 'Prepaid' },
        ]}/>
      </FieldRow>
      <FieldRow label="Network">
        <PillRadio value={network} onChange={setNetwork} options={['Visa', 'Mastercard', 'RuPay', 'Amex', 'Diners']}/>
      </FieldRow>
      <FieldRow label="Issuer" optional><TextInput value={issuer} onChange={setIssuer} placeholder="HDFC Bank"/></FieldRow>
      <FieldRow label="Last 4 digits" optional><TextInput value={last4} onChange={setLast4} placeholder="8842"/></FieldRow>
      <FieldRow label="Credit limit"><TextInput value={limit} onChange={setLimit} placeholder="300000" suffix="₹"/></FieldRow>
      <div style={{ display: 'flex', gap: 10 }}>
        <div style={{ flex: 1 }}><FieldRow label="Statement day"><TextInput value={stmt} onChange={setStmt} placeholder="15"/></FieldRow></div>
        <div style={{ flex: 1 }}><FieldRow label="Due day"><TextInput value={due} onChange={setDue} placeholder="3"/></FieldRow></div>
      </div>
      <FieldRow label="Card color"><ColorSwatchRow value={color} onChange={setColor} swatches={ACC_COLORS}/></FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save card</button>
    </div>
  );
}

/* ── Add Investment ───────────────────────────────────────── */
function AddInvestmentSheet({ onClose }) {
  const [name, setName] = useState('');
  const [type, setType] = useState('mf');
  const [inst, setInst] = useState('');
  const [cur, setCur] = useState('');
  const [tax, setTax] = useState(null);
  const [color, setColor] = useState('var(--acc-indigo)');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New investment"/>
      <FieldRow label="Name"><TextInput value={name} onChange={setName} placeholder="Nifty 50 Index Fund"/></FieldRow>
      <FieldRow label="Type">
        <PillRadio value={type} onChange={setType} options={[
          { value: 'fd', label: 'FD' }, { value: 'rd', label: 'RD' },
          { value: 'sip', label: 'SIP' }, { value: 'mf', label: 'Mutual Fund' },
          { value: 'stocks', label: 'Stocks' }, { value: 'gold', label: 'Gold' },
          { value: 'bonds', label: 'Bonds' }, { value: 'ppf', label: 'PPF' },
          { value: 'epf', label: 'EPF' }, { value: 'nps', label: 'NPS' },
          { value: 'ulip', label: 'ULIP' }, { value: 'other', label: 'Other' },
        ]}/>
      </FieldRow>
      <FieldRow label="Institution / AMC" optional><TextInput value={inst} onChange={setInst} placeholder="UTI Mutual Fund"/></FieldRow>
      <FieldRow label="Current value"><TextInput value={cur} onChange={setCur} placeholder="124300" suffix="₹"/></FieldRow>
      <FieldRow label="Start date" optional>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="chip"><Icon name="calendar" size={13} color="currentColor"/>22 May 2026</button>
          <button className="chip" style={{ color: 'var(--text-3)' }}>+ Maturity date</button>
        </div>
      </FieldRow>
      <FieldRow label="Tax section" optional>
        <PillRadio value={tax} onChange={setTax} options={[
          { value: 'none', label: 'Not eligible' },
          { value: '80c', label: '80C' }, { value: '80d', label: '80D' },
          { value: '80ccd', label: '80CCD(1B)' }, { value: 'other', label: 'Other' },
        ]}/>
      </FieldRow>
      <FieldRow label="Color"><ColorSwatchRow value={color} onChange={setColor} swatches={ACC_COLORS}/></FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save investment</button>
    </div>
  );
}

/* ── Add Insurance ─────────────────────────────────────────── */
function AddInsuranceSheet({ onClose }) {
  const [name, setName] = useState('');
  const [type, setType] = useState('health');
  const [provider, setProvider] = useState('');
  const [sum, setSum] = useState('');
  const [premium, setPremium] = useState('');
  const [freq, setFreq] = useState('yearly');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New policy"/>
      <FieldRow label="Policy name"><TextInput value={name} onChange={setName} placeholder="HDFC Life Click 2 Protect"/></FieldRow>
      <FieldRow label="Type">
        <PillRadio value={type} onChange={setType} options={[
          { value: 'health', label: 'Health' }, { value: 'vehicle', label: 'Vehicle' },
          { value: 'life-term', label: 'Life (Term)' }, { value: 'life-endow', label: 'Life (Endowment / ULIP)' },
          { value: 'travel', label: 'Travel' }, { value: 'home', label: 'Home' },
          { value: 'other', label: 'Other' },
        ]}/>
      </FieldRow>
      <FieldRow label="Provider"><TextInput value={provider} onChange={setProvider} placeholder="HDFC Life"/></FieldRow>
      <FieldRow label="Sum assured"><TextInput value={sum} onChange={setSum} placeholder="1500000" suffix="₹"/></FieldRow>
      <FieldRow label="Premium amount"><TextInput value={premium} onChange={setPremium} placeholder="18400" suffix="₹"/></FieldRow>
      <FieldRow label="Frequency">
        <PillRadio value={freq} onChange={setFreq} options={[
          { value: 'monthly', label: 'Monthly' }, { value: 'quarterly', label: 'Quarterly' },
          { value: 'half', label: 'Half-yearly' }, { value: 'yearly', label: 'Yearly' },
          { value: 'single', label: 'Single-pay' },
        ]}/>
      </FieldRow>
      <FieldRow label="Tax section" optional>
        <PillRadio value={null} onChange={() => {}} options={['80C', '80D', 'None']}/>
      </FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save policy</button>
    </div>
  );
}

/* ── Add Goal ──────────────────────────────────────────── */
function AddGoalSheet({ onClose }) {
  const [name, setName] = useState('');
  const [target, setTarget] = useState('');
  const [eta, setEta] = useState('');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New goal"/>
      <FieldRow label="Goal name"><TextInput value={name} onChange={setName} placeholder="Emergency fund"/></FieldRow>
      <FieldRow label="Target amount"><TextInput value={target} onChange={setTarget} placeholder="600000" suffix="₹"/></FieldRow>
      <FieldRow label="Target date" optional>
        <button className="chip" style={{ height: 44, padding: '0 18px' }}>
          <Icon name="calendar" size={14} color="currentColor"/>
          {eta || 'Pick a date'}
        </button>
      </FieldRow>
      <FieldRow label="Linked accounts" optional hint="Funds sitting here count toward this goal">
        <div style={{ fontSize: 13, color: 'var(--text-3)', padding: '8px 0' }}>
          Nothing linked yet.
        </div>
        <PillRadio value={[]} multi onChange={() => {}} options={[
          { value: 'hdfc', label: 'HDFC Savings' },
          { value: 'icici', label: 'ICICI Current' },
        ]}/>
      </FieldRow>
      <FieldRow label="Linked investments" optional>
        <PillRadio value={[]} multi onChange={() => {}} options={[
          { value: 'fd1', label: 'HDFC FD 7.1%' }, { value: 'gold', label: 'SGB 2031' },
        ]}/>
      </FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save goal</button>
    </div>
  );
}

/* ── Add Budget ────────────────────────────────────────── */
function AddBudgetSheet({ onClose }) {
  const [name, setName] = useState('');
  const [scope, setScope] = useState('overall');
  const [period, setPeriod] = useState('monthly');
  const [amt, setAmt] = useState('');
  const [alertPct, setAlertPct] = useState('80');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New budget"/>
      <FieldRow label="Budget name"><TextInput value={name} onChange={setName} placeholder="May groceries"/></FieldRow>
      <FieldRow label="Scope">
        <PillRadio value={scope} onChange={setScope} options={[
          { value: 'overall', label: 'Overall' }, { value: 'category', label: 'Category' },
        ]}/>
      </FieldRow>
      {scope === 'category' && (
        <FieldRow label="Category">
          <CategoryPicker value={null} onChange={() => {}} type="expense"/>
        </FieldRow>
      )}
      <FieldRow label="Period">
        <PillRadio value={period} onChange={setPeriod} options={[
          { value: 'weekly', label: 'Weekly' }, { value: 'monthly', label: 'Monthly' }, { value: 'yearly', label: 'Yearly' },
        ]}/>
      </FieldRow>
      <FieldRow label="Cap"><TextInput value={amt} onChange={setAmt} placeholder="8000" suffix="₹"/></FieldRow>
      <FieldRow label="Alert at"><TextInput value={alertPct} onChange={setAlertPct} placeholder="80" suffix="%"/></FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save budget</button>
    </div>
  );
}

/* ── Add Subscription ──────────────────────────────────── */
function AddSubscriptionSheet({ onClose }) {
  const [name, setName] = useState('');
  const [provider, setProvider] = useState('');
  const [amt, setAmt] = useState('');
  const [freq, setFreq] = useState('monthly');
  const [status, setStatus] = useState('active');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New subscription"/>
      <FieldRow label="Name"><TextInput value={name} onChange={setName} placeholder="Spotify Family"/></FieldRow>
      <FieldRow label="Provider" optional><TextInput value={provider} onChange={setProvider} placeholder="Spotify"/></FieldRow>
      <FieldRow label="Amount per cycle"><TextInput value={amt} onChange={setAmt} placeholder="179" suffix="₹"/></FieldRow>
      <FieldRow label="Billing frequency">
        <PillRadio value={freq} onChange={setFreq} options={[
          { value: 'monthly', label: 'Monthly' }, { value: 'quarterly', label: 'Quarterly' }, { value: 'yearly', label: 'Yearly' },
        ]}/>
      </FieldRow>
      <FieldRow label="Status">
        <PillRadio value={status} onChange={setStatus} options={[
          { value: 'active', label: 'ACTIVE' }, { value: 'paused', label: 'PAUSED' }, { value: 'cancelled', label: 'CANCELLED' },
        ]}/>
      </FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save subscription</button>
    </div>
  );
}

/* ── Add Recurring ─────────────────────────────────────── */
function AddRecurringSheet({ onClose }) {
  const [name, setName] = useState('');
  const [tpl, setTpl] = useState('');
  const [freq, setFreq] = useState('monthly');
  const [day, setDay] = useState('1');
  const [auto, setAuto] = useState(false);
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New recurring rule" sub="The rule is stored; auto-firing on schedule arrives with WorkManager (Phase 5)."/>
      <FieldRow label="Rule name"><TextInput value={name} onChange={setName} placeholder="Rent"/></FieldRow>
      <FieldRow label="Transaction template"><TextInput value={tpl} onChange={setTpl} placeholder="Rent — Bangalore flat"/></FieldRow>
      <FieldRow label="Frequency">
        <PillRadio value={freq} onChange={setFreq} options={[
          { value: 'daily', label: 'Daily' }, { value: 'weekly', label: 'Weekly' },
          { value: 'monthly', label: 'Monthly' }, { value: 'yearly', label: 'Yearly' },
        ]}/>
      </FieldRow>
      <FieldRow label="Day of month (1–31)"><TextInput value={day} onChange={setDay} placeholder="1"/></FieldRow>
      <div style={{ marginTop: 18, padding: '12px 14px', background: 'var(--surface-2)', borderRadius: 12, display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, color: 'var(--text-1)' }}>Auto-confirm without notification</div>
          <div style={{ fontSize: 11, color: 'var(--text-3)', marginTop: 2 }}>Fire and forget · for fixed amounts like rent</div>
        </div>
        <Toggle checked={auto} onChange={() => setAuto(!auto)}/>
      </div>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save rule</button>
    </div>
  );
}

/* ── Add Person ───────────────────────────────────────── */
function AddPersonSheet({ onClose }) {
  const [name, setName] = useState('');
  const [rel, setRel] = useState('friend');
  const [contact, setContact] = useState('');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New person"/>
      <FieldRow label="Name"><TextInput value={name} onChange={setName} placeholder="Rahul"/></FieldRow>
      <FieldRow label="Relation">
        <PillRadio value={rel} onChange={setRel} options={[
          { value: 'spouse', label: 'Spouse' }, { value: 'parent', label: 'Parent' },
          { value: 'sibling', label: 'Sibling' }, { value: 'child', label: 'Child' },
          { value: 'friend', label: 'Friend' }, { value: 'colleague', label: 'Colleague' },
          { value: 'business', label: 'Business' }, { value: 'other', label: 'Other' },
        ]}/>
      </FieldRow>
      <FieldRow label="Contact" optional><TextInput value={contact} onChange={setContact} placeholder="+91 …"/></FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save person</button>
    </div>
  );
}

/* ── Add Rule ─────────────────────────────────────────── */
function AddRuleSheet({ onClose }) {
  const [name, setName] = useState('');
  const [priority, setPriority] = useState('100');
  const [when, setWhen] = useState('all');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New rule" sub="Lower priority numbers run first. Seeded rules use 10–90; user rules default to 100."/>
      <FieldRow label="Rule name"><TextInput value={name} onChange={setName} placeholder="Auto-tag coffee"/></FieldRow>
      <FieldRow label="Priority"><TextInput value={priority} onChange={setPriority} placeholder="100"/></FieldRow>

      <FieldRow label="When">
        <PillRadio value={when} onChange={setWhen} options={[
          { value: 'all', label: 'All conditions match' }, { value: 'any', label: 'Any condition matches' },
        ]}/>
        <div style={{ marginTop: 10 }}>
          <ConditionRow field="Description" op="contains" val="tokai"/>
          <button className="chip" style={{ marginTop: 8 }}>
            <Icon name="plus" size={14} color="currentColor"/> Add condition
          </button>
        </div>
      </FieldRow>

      <div style={{ height: 1, background: 'var(--line-1)', margin: '20px 0' }}/>

      <FieldRow label="Then">
        <ActionRow action="Set category" val="Food & Drink"/>
        <ActionRow action="Add tag" val="#coffee" style={{ marginTop: 8 }}/>
        <button className="chip" style={{ marginTop: 8 }}>
          <Icon name="plus" size={14} color="currentColor"/> Add action
        </button>
      </FieldRow>

      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save rule</button>
    </div>
  );
}

function ConditionRow({ field, op, val, style }) {
  return (
    <div style={{ display: 'flex', gap: 6, alignItems: 'center', ...style }}>
      <div style={{ flex: '1 1 0', minWidth: 0, padding: '0 12px', height: 38, borderRadius: 10, background: 'var(--surface-2)', border: '1px solid var(--line-1)', display: 'flex', alignItems: 'center', fontSize: 12, color: 'var(--text-1)' }}>{field}</div>
      <div style={{ width: 78, padding: '0 10px', height: 38, borderRadius: 10, background: 'var(--surface-2)', border: '1px solid var(--line-1)', display: 'flex', alignItems: 'center', fontSize: 12, color: 'var(--teal-300)', fontFamily: 'var(--font-mono)' }}>{op}</div>
      <div style={{ flex: '1 1 0', minWidth: 0, padding: '0 12px', height: 38, borderRadius: 10, background: 'var(--surface-2)', border: '1px solid var(--line-1)', display: 'flex', alignItems: 'center', fontSize: 12, color: 'var(--text-1)' }}>{val}</div>
    </div>
  );
}
function ActionRow({ action, val, style }) {
  return (
    <div style={{ display: 'flex', gap: 6, alignItems: 'center', ...style }}>
      <div style={{ width: 130, padding: '0 12px', height: 38, borderRadius: 10, background: 'var(--teal-950)', border: '1px solid var(--teal-900)', display: 'flex', alignItems: 'center', fontSize: 12, color: 'var(--teal-300)', fontFamily: 'var(--font-mono)' }}>{action}</div>
      <div style={{ flex: 1, padding: '0 12px', height: 38, borderRadius: 10, background: 'var(--surface-2)', border: '1px solid var(--line-1)', display: 'flex', alignItems: 'center', fontSize: 12, color: 'var(--text-1)' }}>{val}</div>
    </div>
  );
}

/* ── Add Category ─────────────────────────────────────── */
function AddCategorySheet({ onClose }) {
  const [name, setName] = useState('');
  const [type, setType] = useState('expense');
  const [parent, setParent] = useState('none');
  const [icon, setIcon] = useState('category');
  const [color, setColor] = useState('var(--acc-saffron)');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New category"/>
      <FieldRow label="Name"><TextInput value={name} onChange={setName} placeholder="Coffee shops"/></FieldRow>
      <FieldRow label="Type">
        <PillRadio value={type} onChange={setType} options={[
          { value: 'expense', label: 'Expense' }, { value: 'income', label: 'Income' },
          { value: 'transfer', label: 'Transfer' }, { value: 'investment', label: 'Investment' },
        ]}/>
      </FieldRow>
      <FieldRow label="Parent" optional>
        <PillRadio value={parent} onChange={setParent} options={[
          { value: 'none', label: 'None — top-level' },
          { value: 'food', label: 'Food & Drink' },
          { value: 'transport', label: 'Transport' },
          { value: 'bills', label: 'Bills & Utilities' },
        ]}/>
      </FieldRow>
      <FieldRow label="Icon"><IconChipRow value={icon} onChange={setIcon} icons={['category', 'tag', 'receipt', 'arrowSwap', 'fire', 'shield', 'home', 'people', 'flag', 'play', 'sparkles']}/></FieldRow>
      <FieldRow label="Color"><ColorSwatchRow value={color} onChange={setColor} swatches={ACC_COLORS}/></FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save category</button>
    </div>
  );
}

/* ── Add Tag ─────────────────────────────────────────── */
function AddTagSheet({ onClose }) {
  const [name, setName] = useState('');
  const [color, setColor] = useState('var(--acc-indigo)');
  return (
    <div style={{ paddingBottom: 60 }}>
      <SheetTitle title="New tag"/>
      <FieldRow label="Tag name"><TextInput value={name} onChange={setName} placeholder="coffee"/></FieldRow>
      <FieldRow label="Color"><ColorSwatchRow value={color} onChange={setColor} swatches={ACC_COLORS}/></FieldRow>
      <button className="btn-primary" style={{ width: '100%', marginTop: 26 }} onClick={onClose}>Save tag</button>
    </div>
  );
}

function SheetTitle({ title, sub }) {
  return (
    <div style={{ padding: '4px 4px 6px' }}>
      <div style={{ fontSize: 22, fontWeight: 600, color: 'var(--text-1)', letterSpacing: '-0.01em' }}>{title}</div>
      {sub && <div style={{ marginTop: 6, fontSize: 12, color: 'var(--text-3)', lineHeight: 1.5 }}>{sub}</div>}
    </div>
  );
}

Object.assign(window, {
  AddAccountSheet, AddCardSheet, AddInvestmentSheet, AddInsuranceSheet,
  AddGoalSheet, AddBudgetSheet, AddSubscriptionSheet, AddRecurringSheet,
  AddPersonSheet, AddRuleSheet, AddCategorySheet, AddTagSheet,
  TextInput, ColorSwatchRow, IconChipRow, SheetTitle, ACC_COLORS,
});
