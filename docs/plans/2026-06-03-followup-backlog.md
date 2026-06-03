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
