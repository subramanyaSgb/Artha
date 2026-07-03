# Artha — Improvement & Feature Plan (2026-06-11)

Drafted after the premium-feel fix session. Shipped that day (for context):
adaptive-icon safe-zone fix, audit bugs C-1/H-1/M-5/M-7, receipt persistence via
`ReceiptStore` + off-main decode, Account/Card detail category icons via shared
`TransactionVisuals`, `flowOn(Default)` on 7 ViewModels + single Dashboard log
subscription, and the Transaction Detail page revamp.

Items below are NOT yet built. Ranked within each section (top = recommended first).

---

## A. Cross-cutting "premium feel"

1. ~~**Screen transitions**~~ — **SHIPPED 2026-06-11.** Push/pop slides existed
   already; tab ↔ tab switches now use Material fade-through and predictive back
   is opted in (`enableOnBackInvokedCallback`).
2. ~~**Haptics**~~ — **SHIPPED 2026-06-11** (`ui/common/Haptics.kt`): tick on tab
   switch, confirm on transaction save, heavy on destructive dialog confirms.
3. **Consistent loading skeletons** — a few screens still flash empty on cold
   open; reuse the Accounts/Cards skeleton template everywhere.
4. ~~**Themed splash**~~ — **SHIPPED 2026-06-11** via core-splashscreen
   (brand-teal window + अ mark, swaps to Theme.Artha on first frame).
5. **R8/minified release build + baseline profile** — debug builds are what's
   being used daily; a minified release with a baseline profile noticeably cuts
   startup and jank on-device.
6. **Ledger pagination (later)** — Room `PagingSource` once the log grows past
   ~5k transactions. The batch/shareIn pipeline is fine at current volume.

## B. Explicitly requested (FeaturesiWant.txt)

1. ~~**Full export/import including settings**~~ — **SHIPPED 2026-06-11.**
   Backup schema v2: settings block in backup.json, both exports write a ZIP
   (backup.json + receipts/), encrypted path wraps the ZIP, restore handles
   .zip/.json/.artha, re-points receipt URIs, applies settings last.
2. ~~**Category-wise transaction report**~~ — **SHIPPED 2026-06-11.** Parent
   rollup, per-category icon/colour/count/share rows, inline transaction
   drill-down with tap-through to detail. (A dedicated date-range picker beyond
   This/Last month + FY, and CSV export, remain open ideas.)

## C. Per-screen improvements

| Screen | Improvements (ranked) |
|---|---|
| **Dashboard** | Quick-action chips under hero (Expense / Income / Transfer one-tap); month selector for the monthly strip; premiums-due card tap-through to Insurance. |
| **Ledger** | Migrate rows to `TransactionCategoryAvatar` (today's tint is always grey — diverges from Dashboard); receipt 📎 indicator on rows; multi-select for batch delete/tag; date-range filter chip. |
| **Transaction Detail** | Tap receipt → full-screen pinch-zoom viewer; "Open in Maps" when place is set; show split-group siblings when `isSplit`. (TD3b hydrate-via-flow refactor — low impact, still parked.) |
| **Add Transaction** | Camera capture (FileProvider plumbing — button is a TODO toast); recently-used category suggestions above the picker; inline amount calculator (e.g. `120+85`). |
| **Accounts / Cards** | Mini balance sparkline on list rows; card bill-due date + reminder notification. |
| **Investments** | Detail txn rows have NO leading icon — adopt `TransactionCategoryAvatar`; CAGR/XIRR per investment; value-history chart from contribution log. |
| **Insurance** | IS1 (known leftover): InsuranceDetailScreen still uses the hardcoded `insuranceTypeDisplayName` — switch to the catalogue label from the VM. |
| **People** | Person Detail rows use first-letter avatar — adopt shared visuals; "Settle up" action that pre-fills a transfer for the outstanding net. |
| **Budgets** | Month rollover option; near-limit notification (80%/100%). |
| **Goals** | Link a goal to an account/investment so progress auto-tracks. |
| **Subscriptions** | Renewal notifications via WorkManager; price-change history. |
| **Recurring** | Destination picker for TRANSFER rules (the engine guard now skips them — this is the real H-1 fix); pause / skip-next-occurrence actions. |
| **Reports** | Category-wise report (B-2); month-vs-month comparison; CSV export. |
| **Search** | Filter chips (type / account / tag / has-receipt). |
| **Settings** | Backup v2 with settings + receipts (B-1); auto-lock timeout options. |
| **AI Quick Entry** | Downsample photo before the Gemini upload (faster, cheaper); multi-transaction parse from one paragraph/photo. |

## D. Maintenance / hygiene

- ~~Orphaned receipt cleanup~~ — **SHIPPED 2026-06-11** (startup sweep of
  filesDir/receipts against `TransactionDao.allReceiptUris`).
- ~~EXIF rotation in `ReceiptStore.persist`~~ — **SHIPPED 2026-06-11**
  (androidx.exifinterface; bitmap rotated upright before re-encode).
- Run the instrumented migration tests on a device (still pending since v7).
- Standing deferred items (unchanged): SMS parsing receiver, crash reporting,
  cloud sync.
