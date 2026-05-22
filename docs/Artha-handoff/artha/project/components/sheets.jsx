// ─────────────────────────────────────────────────────────────
// Sheets — Add Transaction + AI Quick Entry
// ─────────────────────────────────────────────────────────────

/* Big amount keypad-style input — looks like Wallet/Cash apps */
function AmountInput({ value, onChange, currency = '₹', color = 'var(--text-1)', autoFocus }) {
  const display = value || '0';
  return (
    <div style={{
      display: 'flex', alignItems: 'baseline', justifyContent: 'center', gap: 6,
      padding: '24px 16px 8px',
    }}>
      <span style={{
        fontSize: 32, color: 'var(--text-3)', fontFamily: 'var(--font-display)',
        fontWeight: 300, lineHeight: 1, marginBottom: 6,
      }}>{currency}</span>
      <input
        autoFocus={autoFocus}
        value={display}
        onChange={(e) => onChange(e.target.value.replace(/[^\d.]/g, ''))}
        inputMode="decimal"
        style={{
          width: 240, textAlign: 'center',
          background: 'transparent', border: 'none', outline: 'none',
          color, fontFamily: 'var(--font-display)', fontWeight: 300,
          fontSize: 64, lineHeight: 1,
          fontFeatureSettings: '"tnum", "lnum"',
          letterSpacing: '-0.02em',
        }}
      />
    </div>
  );
}

/* Segmented tab control — Expense / Income / Transfer */
function SegmentedTabs({ value, onChange, options }) {
  return (
    <div style={{
      display: 'flex',
      padding: 4,
      background: 'var(--surface-2)',
      borderRadius: 14,
      gap: 2,
    }}>
      {options.map(o => {
        const isActive = value === o.value;
        return (
          <button key={o.value} onClick={() => onChange(o.value)} className="touch" style={{
            flex: 1, height: 40, borderRadius: 11,
            background: isActive ? 'var(--surface-4)' : 'transparent',
            color: isActive ? o.color || 'var(--text-1)' : 'var(--text-2)',
            border: 'none', cursor: 'pointer',
            fontFamily: 'var(--font-ui)',
            fontSize: 13, fontWeight: 600, letterSpacing: '0.01em',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
            transition: 'all .15s',
          }}>
            {o.icon && <Icon name={o.icon} size={14} color="currentColor" weight={2}/>}
            {o.label}
          </button>
        );
      })}
    </div>
  );
}

/* Generic label + control row used throughout sheets */
function FieldRow({ label, children, optional, hint }) {
  return (
    <div style={{ marginTop: 18 }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        marginBottom: 8,
      }}>
        <div className="eyebrow" style={{ color: 'var(--text-2)' }}>
          {label}
          {optional && <span style={{ color: 'var(--text-3)', textTransform: 'none', letterSpacing: '0.04em', fontWeight: 500 }}> · optional</span>}
        </div>
        {hint && <div style={{ fontSize: 11, color: 'var(--text-3)' }}>{hint}</div>}
      </div>
      {children}
    </div>
  );
}

/* Account/pill picker — radio chips */
function PillRadio({ value, onChange, options, multi = false }) {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {options.map(o => {
        const v = o.value ?? o;
        const isActive = multi ? (value || []).includes(v) : value === v;
        return (
          <button key={v} onClick={() => onChange(multi ? toggleArr(value || [], v) : v)}
            className={'chip' + (isActive ? ' active' : '')}>
            {o.icon && <Icon name={o.icon} size={13} color="currentColor"/>}
            {o.label ?? o}
          </button>
        );
      })}
    </div>
  );
}
function toggleArr(arr, v) {
  return arr.includes(v) ? arr.filter(x => x !== v) : [...arr, v];
}

/* ─────────────────────────────────────────────────────────────
   AddTransactionSheet
   ─────────────────────────────────────────────────────────── */
function AddTransactionSheet({ onClose }) {
  const [type, setType] = useState('expense');
  const [amount, setAmount] = useState('');
  const [fromAcc, setFromAcc] = useState('HDFC Savings');
  const [toAcc, setToAcc] = useState('Niyo');
  const [cat, setCat] = useState(null);
  const [desc, setDesc] = useState('');
  const [app, setApp] = useState('GPay');
  const [people, setPeople] = useState([]);
  const [tags, setTags] = useState([]);

  const amountColor = type === 'income' ? 'var(--income)' : type === 'transfer' ? 'var(--indigo)' : 'var(--text-1)';
  const saveColor = type === 'income' ? 'var(--income)' : type === 'transfer' ? 'var(--indigo-deep)' : 'var(--teal-700)';
  const valid = parseFloat(amount) > 0;

  return (
    <div style={{ paddingBottom: 80 }}>
      <SegmentedTabs
        value={type}
        onChange={setType}
        options={[
          { value: 'expense', label: 'Expense', icon: 'arrowUp', color: 'var(--expense)' },
          { value: 'income', label: 'Income', icon: 'arrowDown', color: 'var(--income)' },
          { value: 'transfer', label: 'Transfer', icon: 'arrowSwap', color: 'var(--indigo)' },
        ]}
      />

      {/* Big amount entry */}
      <AmountInput value={amount} onChange={setAmount} color={amountColor} autoFocus/>

      {/* Date & time row */}
      <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 4 }}>
        <button className="chip">
          <Icon name="calendar" size={13} color="currentColor"/>
          Fri, 22 May
        </button>
        <button className="chip">
          <Icon name="clock" size={13} color="currentColor"/>
          9:41 AM
        </button>
      </div>

      {/* From / To */}
      {type === 'expense' && (
        <FieldRow label="From">
          <PillRadio value={fromAcc} onChange={setFromAcc}
            options={['HDFC Savings', 'ICICI Current', 'Cash', 'Niyo']}/>
        </FieldRow>
      )}
      {type === 'income' && (
        <FieldRow label="To">
          <PillRadio value={toAcc} onChange={setToAcc}
            options={['HDFC Savings', 'ICICI Current', 'Cash', 'Niyo']}/>
        </FieldRow>
      )}
      {type === 'transfer' && (
        <>
          <FieldRow label="From">
            <PillRadio value={fromAcc} onChange={setFromAcc}
              options={['HDFC Savings', 'ICICI Current', 'Cash', 'Niyo']}/>
          </FieldRow>
          <FieldRow label="To">
            <PillRadio value={toAcc} onChange={setToAcc}
              options={['HDFC Savings', 'ICICI Current', 'Cash', 'Niyo']}/>
          </FieldRow>
        </>
      )}

      {/* Category */}
      {type !== 'transfer' && (
        <FieldRow label="Category">
          <CategoryPicker value={cat} onChange={setCat} type={type}/>
        </FieldRow>
      )}

      {/* Description */}
      <FieldRow label="Description" optional>
        <input
          value={desc} onChange={(e) => setDesc(e.target.value)}
          placeholder={type === 'expense' ? 'Blue Tokai coffee' : type === 'income' ? 'May salary' : 'Forex top-up'}
          className="input" style={{ height: 48, fontSize: 14 }}
        />
      </FieldRow>

      {/* Payment app */}
      <FieldRow label="Paid via">
        <PillRadio value={app} onChange={setApp}
          options={['GPay','PhonePe','Paytm','CRED','BHIM','Bank app','Card swipe','Netbanking','Cash','Other']}/>
      </FieldRow>

      {/* People */}
      <FieldRow label="People" optional>
        <PillRadio value={people} multi onChange={setPeople}
          options={[
            { value: 'rahul', label: '+ Rahul' },
            { value: 'priya', label: '+ Priya' },
            { value: 'mom', label: '+ Mom' },
            { value: 'new', label: '+ Add person', icon: 'plus' },
          ]}/>
      </FieldRow>

      {/* Tags */}
      <FieldRow label="Tags" optional>
        <PillRadio value={tags} multi onChange={setTags}
          options={[
            { value: 'coffee', label: '# coffee' },
            { value: 'dinner', label: '# dinner' },
            { value: 'sip', label: '# sip' },
            { value: 'reimburse', label: '# reimburse' },
            { value: 'new', label: '+ New tag', icon: 'plus' },
          ]}/>
      </FieldRow>

      {/* Receipt + Place row */}
      <FieldRow label="Attach" optional>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="chip" style={{ height: 44, padding: '0 18px' }}>
            <Icon name="image" size={16} color="currentColor"/>
            Receipt
          </button>
          <button className="chip" style={{ height: 44, padding: '0 18px' }}>
            <Icon name="location" size={16} color="currentColor"/>
            Place
          </button>
          <button className="chip" style={{ height: 44, padding: '0 18px' }}>
            <Icon name="edit" size={16} color="currentColor"/>
            Note
          </button>
        </div>
      </FieldRow>

      {/* Save */}
      <div style={{ marginTop: 24 }}>
        <button className="btn-primary" style={{
          width: '100%',
          background: valid ? saveColor : 'var(--surface-3)',
          color: valid ? '#fff' : 'var(--text-3)',
        }} disabled={!valid} onClick={onClose}>
          Save {type === 'income' ? 'income' : type === 'transfer' ? 'transfer' : 'expense'}
          {valid && (<>· {fmtINR(parseFloat(amount))}</>)}
        </button>
      </div>
    </div>
  );
}

/* Category picker — chip row with icon */
function CategoryPicker({ value, onChange, type }) {
  const cats = type === 'income' ? INCOME_CATS : EXPENSE_CATS;
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {cats.map(c => (
        <button key={c.id} onClick={() => onChange(c.id)}
          className={'chip' + (value === c.id ? ' active' : '')}
          style={{ height: 38, paddingLeft: 8 }}
        >
          <span style={{
            width: 22, height: 22, borderRadius: 7,
            background: c.color, display: 'inline-flex',
            alignItems: 'center', justifyContent: 'center',
            color: '#fff',
          }}>
            <Icon name={c.icon} size={12} color="#fff" weight={2}/>
          </span>
          {c.name}
        </button>
      ))}
    </div>
  );
}
const EXPENSE_CATS = [
  { id: 'food', name: 'Food & Drink', icon: 'receipt', color: 'var(--acc-saffron)' },
  { id: 'transport', name: 'Transport', icon: 'arrowSwap', color: 'var(--acc-indigo)' },
  { id: 'bills', name: 'Bills', icon: 'fire', color: 'var(--acc-magenta)' },
  { id: 'shopping', name: 'Shopping', icon: 'tag', color: 'var(--acc-violet)' },
  { id: 'health', name: 'Health', icon: 'shield', color: 'var(--acc-emerald)' },
  { id: 'home', name: 'Home', icon: 'home', color: 'var(--acc-teal)' },
];
const INCOME_CATS = [
  { id: 'salary', name: 'Salary', icon: 'arrowDown', color: 'var(--income)' },
  { id: 'refund', name: 'Refund', icon: 'refresh', color: 'var(--acc-teal)' },
  { id: 'other', name: 'Other', icon: 'arrowDown', color: 'var(--acc-indigo)' },
];

/* ─────────────────────────────────────────────────────────────
   AI Quick Entry — magical/conversational treatment
   ─────────────────────────────────────────────────────────── */
function AIQuickEntry({ onClose, onCommit }) {
  const [text, setText] = useState('');
  const [parsing, setParsing] = useState(false);
  const [parsed, setParsed] = useState(null);

  // Mock parse — kick in when user types > 6 chars
  useEffect(() => {
    if (text.trim().length < 6) { setParsed(null); return; }
    setParsing(true);
    const t = setTimeout(() => {
      setParsed(mockParse(text));
      setParsing(false);
    }, 700);
    return () => clearTimeout(t);
  }, [text]);

  const examples = [
    'Auto to MG Road ₹180',
    'Paid Rahul ₹500 for chai',
    'Salary 92k from Acme',
    'Bigbasket 3240 on Regalia',
    'Transferred 5k to Niyo',
  ];

  return (
    <div style={{ paddingBottom: 30 }}>
      {/* Header glyph row */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 14,
        padding: '4px 4px 18px',
      }}>
        <div style={{
          width: 48, height: 48, borderRadius: 14,
          background: 'linear-gradient(135deg, var(--teal-700) 0%, var(--teal-950) 100%)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 0 0 1px var(--line-teal), 0 8px 24px -8px rgba(15,118,110,0.5)',
        }}>
          <Icon name="sparkles" size={24} color="#fff"/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 17, fontWeight: 700, color: 'var(--text-1)', letterSpacing: '-0.01em' }}>Quick add</div>
          <div style={{ fontSize: 12, color: 'var(--text-3)' }}>Gemini parses what happened. You confirm.</div>
        </div>
      </div>

      {/* Multiline input */}
      <div style={{
        position: 'relative',
        background: 'var(--surface-2)',
        border: '1px solid ' + (text ? 'var(--line-teal)' : 'var(--line-1)'),
        borderRadius: 18,
        padding: 16,
        transition: 'border .15s',
      }}>
        <textarea
          autoFocus
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="What happened? Try: ‘Coffee at Blue Tokai ₹420 GPay’"
          rows={3}
          style={{
            width: '100%', background: 'transparent', border: 'none', outline: 'none',
            color: 'var(--text-1)', fontFamily: 'var(--font-ui)', fontSize: 15,
            resize: 'none', lineHeight: 1.5,
          }}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="touch" style={{
              width: 38, height: 38, borderRadius: 19,
              background: 'var(--surface-3)', border: '1px solid var(--line-1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
            }}>
              <Icon name="mic" size={17} color="var(--text-1)"/>
            </button>
            <button className="touch" style={{
              width: 38, height: 38, borderRadius: 19,
              background: 'var(--surface-3)', border: '1px solid var(--line-1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
            }}>
              <Icon name="image" size={17} color="var(--text-1)"/>
            </button>
          </div>
          <button className="touch" disabled={!text} style={{
            width: 40, height: 40, borderRadius: 20,
            background: text ? 'var(--teal-700)' : 'var(--surface-3)',
            border: 'none', cursor: text ? 'pointer' : 'default',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name="send" size={17} color={text ? '#fff' : 'var(--text-3)'} weight={2}/>
          </button>
        </div>
      </div>

      {/* Example chips */}
      {!text && (
        <div style={{ marginTop: 16 }}>
          <div className="eyebrow" style={{ marginBottom: 8 }}>Or try one of these</div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {examples.map(e => (
              <button key={e} className="chip" onClick={() => setText(e)} style={{ height: 36 }}>
                <Icon name="sparkles" size={11} color="var(--teal-300)"/>
                {e}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Parsing indicator */}
      {parsing && (
        <div style={{
          marginTop: 18, padding: '16px 18px',
          background: 'var(--surface-2)', border: '1px solid var(--line-1)',
          borderRadius: 14, display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <div className="ai-pulse"/>
          <div style={{ fontSize: 13, color: 'var(--text-2)' }}>Gemini is reading your sentence…</div>
        </div>
      )}

      {/* Parsed preview */}
      {parsed && !parsing && (
        <div style={{
          marginTop: 18,
          background: 'var(--surface-2)', border: '1px solid var(--line-teal)',
          borderRadius: 18, overflow: 'hidden',
        }}>
          <div style={{
            padding: '10px 16px', borderBottom: '1px solid var(--line-1)',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Icon name="sparkles" size={13} color="var(--teal-300)"/>
              <div className="eyebrow" style={{ color: 'var(--teal-300)' }}>Parsed · review & confirm</div>
            </div>
            <button className="touch" style={{
              background: 'transparent', border: 'none', color: 'var(--text-2)',
              fontSize: 12, cursor: 'pointer', fontFamily: 'var(--font-ui)',
            }}>
              <Icon name="edit" size={14} color="currentColor" weight={1.6}/>
            </button>
          </div>
          <div style={{ padding: '14px 18px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
              <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-1)' }}>{parsed.desc}</div>
              <div className="num-display tnum" style={{ fontSize: 26, color: parsed.amt > 0 ? 'var(--income)' : 'var(--text-1)', fontWeight: 400, letterSpacing: '-0.01em' }}>
                {fmtINR(parsed.amt, { sign: true })}
              </div>
            </div>
            <div style={{ marginTop: 12, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              <ParsedChip label={parsed.cat} color="var(--acc-saffron)"/>
              <ParsedChip label={parsed.acc} icon="bank"/>
              {parsed.app && <ParsedChip label={parsed.app}/>}
              {parsed.people?.map(p => <ParsedChip key={p} label={`@${p}`}/>)}
              <ParsedChip label={parsed.date}/>
            </div>
          </div>
          <div style={{
            padding: 10, borderTop: '1px solid var(--line-1)',
            display: 'flex', gap: 8, background: 'var(--surface-3)',
          }}>
            <button className="btn-ghost" style={{ flex: 1, height: 44 }} onClick={onClose}>Cancel</button>
            <button className="btn-primary" style={{ flex: 2, height: 44 }} onClick={() => { onCommit?.(parsed); onClose(); }}>
              <Icon name="check" size={16} color="#fff" weight={2.2}/>
              Save transaction
            </button>
          </div>
        </div>
      )}

      <style>{`
        .ai-pulse {
          width: 14px; height: 14px; border-radius: 50%;
          background: var(--teal-500);
          animation: aipulse 1.2s ease-in-out infinite;
          box-shadow: 0 0 12px var(--teal-500);
        }
        @keyframes aipulse {
          0%,100% { transform: scale(0.8); opacity: 0.5; }
          50% { transform: scale(1.2); opacity: 1; }
        }
      `}</style>
    </div>
  );
}

function ParsedChip({ label, color, icon }) {
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      height: 26, padding: '0 10px',
      background: 'var(--surface-3)', border: '1px solid var(--line-1)',
      borderRadius: 13,
      fontSize: 12, color: 'var(--text-1)',
    }}>
      {color && <span style={{ width: 8, height: 8, borderRadius: 4, background: color }}/>}
      {icon && <Icon name={icon} size={11} color="var(--text-2)"/>}
      {label}
    </div>
  );
}

function mockParse(text) {
  const t = text.toLowerCase();
  // amount
  const m = text.match(/(?:₹|rs\.?|inr)?\s?(\d+(?:[,.]?\d+)*)\s?(k|l|cr)?/i);
  let amt = m ? parseFloat(m[1].replace(/,/g, '')) : 500;
  if (m && m[2]?.toLowerCase() === 'k') amt *= 1000;
  if (m && m[2]?.toLowerCase() === 'l') amt *= 100000;
  // sign
  const incomeWords = /salary|refund|received|credited|got|cashback/i;
  const isIncome = incomeWords.test(text);
  // category
  let cat = 'Miscellaneous', icon = 'receipt';
  if (/coffee|tokai|chai|cafe|tea/i.test(text)) { cat = 'Food & Drink'; }
  else if (/auto|uber|ola|taxi|cab|bus|metro/i.test(text)) { cat = 'Transport'; }
  else if (/bigbasket|swiggy|zomato|grocer|food/i.test(text)) { cat = 'Food & Drink'; }
  else if (/salary|acme/i.test(text)) { cat = 'Salary'; }
  else if (/transfer|sent|paid to|to .*niyo/i.test(text)) { cat = 'Transfer'; }
  else if (/bescom|electric|rent/i.test(text)) { cat = 'Bills'; }
  // account
  let acc = 'HDFC Savings';
  if (/regalia/i.test(text)) acc = 'HDFC Regalia •8842';
  else if (/icici/i.test(text)) acc = 'ICICI Current';
  else if (/cash/i.test(text)) acc = 'Cash';
  else if (/niyo/i.test(text)) acc = 'Niyo';
  // app
  let app = null;
  if (/gpay|g pay|google pay/i.test(text)) app = 'GPay';
  else if (/phonepe|pp/i.test(text)) app = 'PhonePe';
  else if (/cred/i.test(text)) app = 'CRED';
  else if (/bhim/i.test(text)) app = 'BHIM';
  // people
  const people = [];
  if (/rahul/i.test(text)) people.push('Rahul');
  if (/priya/i.test(text)) people.push('Priya');
  return {
    desc: text.replace(/₹?\d+[,.]?\d*\s?[klcr]?/i, '').trim() || 'Transaction',
    amt: isIncome ? amt : -amt,
    cat, acc, app, people,
    date: 'Today, 9:41 AM',
  };
}

Object.assign(window, {
  AmountInput, SegmentedTabs, FieldRow, PillRadio, CategoryPicker,
  AddTransactionSheet, AIQuickEntry, ParsedChip, mockParse,
  EXPENSE_CATS, INCOME_CATS,
});
