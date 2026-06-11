# Artha — Improvement & Feature Plan (2026-06-11)

Drafted after the premium-feel fix session. Shipped that day (for context):
adaptive-icon safe-zone fix, audit bugs C-1/H-1/M-5/M-7, receipt persistence via
`ReceiptStore` + off-main decode, Account/Card detail category icons via shared
`TransactionVisuals`, `flowOn(Default)` on 7 ViewModels + single Dashboard log
subscription, and the Transaction Detail page revamp.

Items below are NOT yet built. Ranked within each section (top = recommended first).

---

## A. Cross-cutting "premium feel"

1. **Screen transitions** — Compose Navigation currently snaps between tabs and
   detail screens. Add shared-axis/fade-through `enterTransition`/`exitTransition`
   on the NavHost + predictive-back support (`android:enableOnBackInvokedCallback`).
   Single biggest "feels premium" win after the perf fixes.
2. **Haptics** — `LocalHapticFeedback` ticks on save, delete-confirm, reorder
   drag, and tab switch.
3. **Consistent loading skeletons** — a few screens still flash empty on cold
   open; reuse the Accounts/Cards skeleton template everywhere.
4. **Themed splash** — `androidx.core.splashscreen` with the अ mark, so cold
   start doesn't show a blank window.
5. **R8/minified release build + baseline profile** — debug builds are what's
   being used daily; a minified release with a baseline profile noticeably cuts
   startup and jank on-device.
6. **Ledger pagination (later)** — Room `PagingSource` once the log grows past
   ~5k transactions. The batch/shareIn pipeline is fine at current volume.

## B. Explicitly requested (FeaturesiWant.txt)

1. **Full export/import including settings** — extend `BackupCodec` with a
   `settings` block (all `SettingsPreferences` keys), bump `SCHEMA_VERSION` → 2
   (v1 restores must keep working), and package receipts into a `.zip`
   (JSON + `receipts/` dir) so images survive reinstall. One file out, one file in.
2. **Category-wise transaction report** — new Reports section: date-range picker,
   per-category totals (sub-categories rolled into parents like the Dashboard
   breakdown), bar/donut via Vico, and tap-through to a category-filtered Ledger.

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

- Orphaned receipt cleanup: delete the copied file when a transaction is deleted
  or its receipt replaced (compare old vs new `receiptUri` in the repository).
- EXIF rotation in `ReceiptStore.persist` (androidx.exifinterface) — gallery
  photos can render sideways.
- Run the instrumented migration tests on a device (still pending since v7).
- Standing deferred items (unchanged): SMS parsing receiver, crash reporting,
  cloud sync.
