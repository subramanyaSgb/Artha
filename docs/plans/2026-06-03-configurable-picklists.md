# User-Configurable Cosmetic Pick-Lists — Design & Plan

**Goal:** let the user customise the *cosmetic* pick-lists they choose from in forms — without
touching the load-bearing enums (`TransactionType`, `SourceKind`, `CategoryType`, `ValuationMode`,
`TransactionSource`) that drive logic.

Scope agreed 2026-06-03 (all three): **PaymentApp**, **category/tag colours + icons**, and
**Account / Card / Insurance types**.

---

## The core architectural decision (the risky part)

`PaymentApp`, `AccountType`, `CardType`, `InsuranceType` are Kotlin enums stored in Room as
`enum.name` strings via `@TypeConverter`s. To allow *custom* entries we must stop treating the
column as a closed enum. Two options:

### Option A — string column + "catalogue" table (RECOMMENDED)
- Change each column from enum-converted to a **plain `String`** (the stored data is *already*
  the enum name, so existing rows are byte-compatible — the migration is a no-op data-wise, it
  only drops the converter requirement).
- Add a small **catalogue table per list** (`payment_app`, `account_type`, …) holding
  `id (string) , label , icon , is_builtin , display_order`. Seed it with today's enum values
  (`is_builtin = 1`) on DB-create / migration.
- Forms read the catalogue (built-ins + user rows) for the picker; the entity stores the chosen
  `id`. Display/grouping resolves `id → label` via the catalogue (fallback to the raw id).
- **Pros:** existing data untouched; built-ins can't be deleted (only hidden); fully additive.
- **Cons:** code that `when`-matches on the enum (e.g. label mappers, default icons) must move to
  catalogue lookups. None of these enums currently drive *balance/logic* branches (verified in the
  feature review) — they're label/icon/grouping only — so this is safe.

### Option B — keep enums, add an "OTHER/custom" free-text
- Much smaller, but doesn't really deliver editable lists. Rejected.

**→ Decision needed from the user:** confirm Option A (string column + catalogue table) before any
migration is written. This is data-storage-level and should be an explicit yes.

Colours + icons are **not** enum-backed — category/tag rows already store `color: Long` and
`icon: String`. So those need no migration: just a DataStore-backed editable palette/icon set the
pickers read, plus a manage-UI. (Lowest risk → built first.)

---

## Phased plan (each phase = its own branch, built + verified + merged)

### Phase 1 — Colours + icons (NO migration, lowest risk) — DO FIRST
- A `PickListPreferences` (DataStore) holding a user colour palette (list of `Long`) and a user
  icon-key list, each defaulting to today's hardcoded `PALETTE` / `ICONS`.
- `CategoryFormSheet` + `TagFormSheet` read the palette/icon set from there (merge defaults +
  custom). Add a small "+ custom colour" (HSV picker) / "+ icon" affordance, and a
  **Settings → Look & feel** section to add/remove swatches + icons.
- No schema change. Ship, verify, merge.

### Phase 2 — PaymentApp catalogue (first enum, Option A) — MEDIUM
- DB v5→v6: add `payment_app` catalogue table, seed built-ins; change `transactions.payment_app`
  to a plain `String` column (drop the enum converter; data already compatible). Instrumented
  migration test.
- `PaymentAppCatalogueRepository`; the transaction sheet's `PaymentAppPicker` and the Reports
  "by app" grouping read it. Settings UI to add/rename/hide entries.
- Heaviest because reporting groups by it — covered by tests.

### Phase 3 — Account / Card / Insurance types — MEDIUM (repeat the Phase-2 pattern)
- DB v6→v7: three catalogue tables; change the three type columns to `String`; seed built-ins.
- Update the account/card/insurance forms + any label/icon/grouping lookups to use the catalogues.

### Cross-cutting
- Built-ins are never deletable (only hideable) so backups/old rows always resolve.
- `BackupCodec` must serialise the new catalogue tables (Phases 2–3) — else backups drop them.
- Each enum→string migration needs its instrumented `MigrationTestHelper` test (device-run pending).
- Keep `defaultValuationMode()` etc. working: `InvestmentType` stays an enum for now (it IS
  logic-bearing via valuation mode) — making it editable is explicitly out of scope here.

---

## Recommendation
Start **Phase 1** immediately (safe, no migration, visible win). Confirm **Option A** before
Phases 2–3 (they rewrite how four core columns are stored). Phases land one at a time so the app
stays releasable throughout.
