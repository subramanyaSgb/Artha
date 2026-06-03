# Follow-up Backlog (captured 2026-06-03)

Two items raised mid-session during the investment-valuation work. Agreed to do them on
**separate branches after** `feat/investment-valuation` merges, to keep that branch/PR clean.

---

## Item A — Direct credits to a credit card don't reduce its outstanding

**Bug (user-reported):** A purchase of ₹601 on a credit card, with ₹401 later returned and
posted as **direct income to the card**, did not reduce the card's outstanding — it stayed at
₹601 instead of dropping to ₹200. Only `REFUND`/`CASHBACK` (and `CARD_PAYMENT` to the card)
currently decrease outstanding.

**Root cause:** [`BalanceCalculator.computeCardOutstanding`](../../app/src/main/java/com/subramanya/artha/data/balance/BalanceCalculator.kt)
only treats `CARD_CREDITS = {REFUND, CASHBACK}` as outstanding-reducing for `source = CARD`.
Any other money-in type (notably `INCOME`) falls through `else -> Unit` and does nothing —
asymmetric with `computeAccountBalance`, where all `MONEY_INTO_SOURCE` types add to an account.

**Agreed fix:** When `source = CARD`, **all money-in types reduce outstanding** — i.e. apply
the existing `MONEY_INTO_SOURCE` set (`INCOME, REFUND, CASHBACK, INTEREST, LOAN_RECEIVED,
GIFT_RECEIVED, INVESTMENT_SELL`) as outstanding-reducing credits, in addition to the existing
`CARD_PAYMENT`-to-destination rule. Make the card side symmetric with the account side.

**TDD sketch:**
- Test: EXPENSE ₹601 to card → outstanding 601; then INCOME ₹401 to card → outstanding 200.
- Test: each money-in type on a card reduces outstanding by its amount.
- Test: charges (EXPENSE/LOAN_GIVEN/GIFT_SENT) still increase; ADJUSTMENT still signed.
- Then widen the credit set / reuse `MONEY_INTO_SOURCE` in `computeCardOutstanding`.
- Update the class JavaDoc's credit-card direction rules to list the broadened set.

**Branch:** `fix/card-outstanding-direct-credit` (off `main` after valuation merges).

---

## Item B — Add-transaction FAB on the Ledger screen

**Request:** The **Ledger** tab (`nav_transactions` = "Ledger",
[`TransactionsScreen.kt`](../../app/src/main/java/com/subramanya/artha/ui/transactions/TransactionsScreen.kt))
currently has **no FAB**. Add an add-transaction FAB there, opening the existing
`AddTransactionSheet` (as other screens already do).

**Approach:** Mirror the existing FAB + `AddTransactionSheet` wiring used on another screen
(e.g. Dashboard/Accounts). Add a `Scaffold` `floatingActionButton` to `TransactionsScreen`,
reuse the shared add-transaction state/VM pattern, new strings in `strings.xml` (content
description). No new transaction logic — pure UI entry-point.

**Branch:** `feat/ledger-add-transaction-fab` (off `main` after valuation merges).

---

## Item C — Balance-flow fan-out perf (deferred optimization)

Surfaced during the investment-valuation Task 9 review. `InvestmentRepository.observeValuesByInvestmentId()`
and `observeActiveWithMetrics()` each do `combine(observeAll(), transactionDao.observeAll())` and recompute
**every** investment's value on **any** transaction change — O(investments × transactions × 2 passes:
`computeInvestmentInvested` + `computeInvestmentInterest`). Multiple consumers (Dashboard, Goals, Reports,
Search) each collect their own copy, so the whole map recomputes N times per change. Negligible at
personal-app scale (tens of investments, low-thousands of txns), and it mirrors the pre-existing
`observeActiveWithMetrics` behaviour — so NOT fixed in the valuation branch.

**If/when it matters:** expose the value map as a single shared hot flow (`shareIn`/`stateIn(WhileSubscribed)`
on a repository-held scope) and precompute a `Map<investmentId, List<txn>>` index once instead of re-scanning
the full transaction list per investment. This is the "balance-flow fan-out" perf refactor already noted in
the session backlog.

**Update 2026-06-03:** the optimisation pass (merged `9a8b33f`) did the pre-indexing/batch part
(`BalanceCalculator.computeAccountBalances/computeCardOutstandings/computeInvestmentTotals`, one pass for all
entities) + `.flowOn(Default).distinctUntilChanged()`. STILL DEFERRED: cross-consumer **sharing** (`shareIn`
on an app-scoped `CoroutineScope` so Dashboard/Reports/Goals/Search collect one computation, not N), and the
**SQL-aggregate** rewrite (push `WHERE source_id=:id`/`GROUP BY`/`SUM` into DAO queries — the indices already
exist and are currently unused). Both are larger/riskier; the batch+flowOn already collapsed the worst cost.

---

## Audit findings still open (from the 2026-06-03 deep-dive; NOT yet fixed)

A 4-angle audit (data-flow, compose-perf, correctness, Room/DB) ran on 2026-06-03. The high-impact, safe
fixes were merged in `9a8b33f` (batch balances off-main; Ledger/Dashboard/Reports off-main; Rules `SetType`
direction guard + create-only; INVESTMENT_SELL edit preserved). These remain open:

**Correctness — D1/D2/D3 DONE (merged `18e81f8`, 2026-06-03):** D1 blocks hard-delete when transactions
reference the entity (routes to Archive); D2 records debit/prepaid card spends against the linked account
(no phantom outstanding); D3 added a complete all-tables `BackupCodec` for both export paths + an atomic
Room `withTransaction` restore (round-trip unit-tested, reviewed). Residual: D2 doesn't retro-fix existing
debit-card rows / unlinked debit cards; D3 restore has no instrumented (device) test yet. Original notes:

- **D1 — Hard-delete orphans transactions.** Deleting an account/card/investment (the Delete action on the
  detail screens, separate from Archive) leaves its transactions with dangling `sourceId`/`destinationId`;
  they still count in reports/totals and a TRANSFER's surviving leg references a missing counterpart. Fix:
  block hard-delete when transactions reference the entity (offer Archive), or cascade/reassign inside a Room
  `@Transaction`. **Decision needed:** block-or-cascade.
- **D2 — Debit/prepaid card spend creates a phantom "outstanding" and never debits the linked account.**
  Picking a debit card as the spend source records EXPENSE with `source = CARD`; `computeCardOutstanding`
  treats it as card debt and nothing flows to the card's `linkedAccountId`. Fix: resolve debit/prepaid charges
  to the linked account, or exclude non-credit cards from the source picker. **Decision needed:** model choice.
- **D3 — Backup is incomplete + has no restore.** `exportDataEncrypted` writes only accounts+transactions;
  the plain export omits investments/insurance/budgets/goals/subs/recurring/rules and the people/tag cross-ref
  tables; there is no import path at all. A user who trusts "backup" loses most data. Fix: serialise all
  tables (incl. cross-refs) and implement a validated restore (`BackupCrypto.decrypt` → Room). Larger feature.

**Perf — safe follow-ups: DONE (merged `6bc0403`, 2026-06-03):**
- ✅ **P-People** — `PeopleViewModel` precomputes net balances in one pass off the main thread.
- ✅ **P-Ledger-virtualize** — Ledger flattened to one keyed lazy item per transaction (`LedgerListItem`),
  day-card visual preserved via first/last-in-day corner rounding.
- ✅ **P-flowOn-rest** — `.flowOn(Default)` added to Search + PersonDetail VMs.
- ✅ **shareIn** — account/card/investment balance flows shared on an app scope (`WhileSubscribed`) so the
  single-pass compute runs once per change, not once per collecting screen.

**STILL deferred — SQL-aggregate rewrite (Item C):** push `WHERE source_id=:id`/`GROUP BY`/`SUM` into DAO
queries (the indices exist, unused). Intentionally NOT done — risks money-correctness divergence from the
proven Kotlin direction rules, for marginal gain now that batch + flowOn + shareIn collapsed the cost.
