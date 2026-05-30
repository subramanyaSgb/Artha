# Investment Valuation Redesign

*Design doc — 2026-05-31. Status: approved, ready for implementation.*

## Problem

`InvestmentEntity.currentValue` is a manually-entered field that **no transaction
ever updates**. Recording a ₹5,000 contribution to a ₹60,000 RD adds an
`INVESTMENT_BUY` transaction (correctly counted in the computed *invested* amount)
but the headline value the user sees stays ₹60,000, because every screen renders
the stale manual `currentValue`. For a recurring/fixed deposit this is the wrong
mental model: a deposit's value *is* its contributions plus accrued interest — there
is no separate "market price."

## Decision

Valuation becomes **per-type**, captured by a stored `valuationMode`:

- **DERIVED** — deposits: `FD`, `RD`, `PPF`, `EPF`, `BONDS`.
  Value is computed; contributions and posted interest grow it automatically.
- **MARKET** — `SIP`, `MUTUAL_FUND`, `EQUITY`, `GOLD_PHYSICAL`, `GOLD_DIGITAL`,
  `NPS`, `ULIP`, `OTHER`. `currentValue` stays a manual market price (unchanged
  behavior); contributions are cost basis.

Uniform identity in both modes: **`gain = value − invested`**.

Interest is posted by the user as `INTEREST` transactions whose **destination is the
investment** (compounding). It grows the deposit's value **and counts as income** in
reports (FY/tax tracking matters to this user). Payout interest paid to a bank
account stays an ordinary income transaction and does not touch the investment.

## Data model

New fields on `InvestmentEntity`:

| Field | Type | Meaning |
|---|---|---|
| `valuationMode` | `DERIVED` \| `MARKET` | Defaulted from type at creation; stored & overridable. |
| `openingContribution` | Double | Principal already in the investment when first added to the app (parallel to `Account.openingBalance`). Counts toward *invested* in both modes. |
| `currentValue` | Double | Retained. Manual market price for `MARKET` only; ignored for `DERIVED`. |

## Computation (pure, in `BalanceCalculator`, TDD'd)

```
invested  = openingContribution
          + Σ INVESTMENT_BUY(dest = inv)
          − Σ INVESTMENT_SELL(src = inv)

interest  = Σ INTEREST(dest = inv)          // compounding interest credited in

value     = (DERIVED) invested + interest
          = (MARKET)  currentValue          // manual

gain      = value − invested                // == interest, for DERIVED
gainPct   = gain / invested                 // "—" when invested == 0
```

`value` is no longer "read the stored field" — it is computed per mode, so top-ups
move the number immediately.

## UI & flows

- **Add/Edit Investment form** ([InvestmentFormSheet]): the value field is
  type-aware.
  - Deposit type → "Amount currently in this RD/FD" bound to `openingContribution`,
    plus a read-only live computed value once contributions/interest exist. No manual
    current value.
  - Market type → "Current value" (manual → `currentValue`) plus optional "Invested
    so far" (→ `openingContribution`).
  - `valuationMode` set from type; an "Advanced" override stays collapsed by default.
- **Contribution (top-up):** reuse the existing INVEST tab
  (`INVESTMENT_BUY → investment`); add a "+ Add contribution" shortcut on Investment
  Detail that opens it pre-filled (source = last-used account, dest = this investment).
- **Post interest (deposits):** new "Post interest" action on a deposit's detail →
  amount + date sheet → `INTEREST` txn with `destination = investment`.
- **Display:**
  - Deposit row/detail: headline = computed value; subline "Invested ₹X · Interest ₹Y".
  - Market row/detail: headline = `currentValue`; subline "Invested ₹X · Gain ₹Z (p%)"
    with an "Update value" action.
  - Investments hero + Dashboard net worth sum each investment's **computed** value.

## Migration (Room schema v3 → v4)

A real `Migration(3, 4)` (never `fallbackToDestructiveMigration`):

```sql
ALTER TABLE investments ADD COLUMN opening_contribution REAL NOT NULL DEFAULT 0;
UPDATE investments SET opening_contribution = current_value;

ALTER TABLE investments ADD COLUMN valuation_mode TEXT NOT NULL DEFAULT 'MARKET';
UPDATE investments SET valuation_mode = 'DERIVED'
  WHERE type IN ('FD','RD','PPF','EPF','BONDS');
```

Net effect on the user's RD: `opening_contribution = 60,000`, existing ₹5,000 top-up
adds on → value **₹65,000**, fixed by the migration itself.

## Edge cases

- `invested == 0` → gainPct renders "—" (existing convention).
- Market migration sets opening = old value → gain starts at 0 (no phantom gains)
  until the user updates value or adds contributions.
- Sell/maturity (`INVESTMENT_SELL → account`) reduces invested/value; may reach 0.
- Only `dest = investment` interest compounds into the deposit; payout interest to a
  bank account is unaffected.

## Tests (write first, watch fail, then implement)

- `BalanceCalculator`: derived value = opening + contributions + interest; market
  value = currentValue; gain = value − invested; sell reduces; invested=0 → NaN%;
  opening-only (no txns) case.
- `MigrationTest` (instrumented): seed a v3 RD at 60k + a 5k buy → assert v4
  opening=60k, mode=DERIVED, computed value=65k.
- Repository/ViewModel: deposit row & hero reflect computed value; posted interest
  shows as income and leaves other monthly totals correct.

## Out of scope

- Auto-computed RD/FD interest schedules (user posts interest manually).
- NAV/units auto-revaluation for market instruments (still manual).

[InvestmentFormSheet]: ../../app/src/main/java/com/subramanya/artha/ui/investments/InvestmentFormSheet.kt
