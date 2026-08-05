# Insurance Redesign — AI Policy-PDF Import + Rich Detail View

**Date:** 2026-08-05
**Status:** Design approved, ready for implementation
**DB:** v11 → v12

## Problem

The Insurance list and detail screens show little beyond amounts. The user wants:
1. On the Insurance section, two add paths — **manual entry** and **upload policy PDF**.
2. Uploading a PDF runs the existing AI vision chain to extract policy fields, saved
   alongside the stored PDF.
3. A **rich, sectioned detail page** showing real policy information (see the reference
   mockup at `Desing_Plan_Insurence/Policy Bond Details.dc (1).html`, a rendered sample
   health policy — the target content for the detail page).

## Section 1 — Data model & storage

**No new tables.** Extend `insurances` (v11 → v12) with 6 nullable columns:

| Column | Type | Purpose |
|---|---|---|
| `plan_name` | String? | e.g. "Care Supreme — Floater" |
| `policy_term` | String? | e.g. "2 years" |
| `life_assured` | String? | the covered person |
| `uin` | String? | regulatory UIN |
| `insurer_helpline` | String? | quick-contact from the doc |
| `details_json` | String? | **rich blob**: members[], riders[], coverage[], exclusions[], contacts{}, premiumBreakdown{}, status, validity |

- Real columns for what we query (premium, dates, type grouping). JSON blob for
  display-only lists — mirrors the existing `RuleSpecJson` pattern. No normalized
  member/rider/coverage tables (rejected as over-engineering).
- The already-unused `policyDocUri` column finally stores the uploaded PDF, persisted
  via `ReceiptStore` (same path as receipts — already in backup zip + orphan prune).
  **Add insurance's doc column to the prune union set in MainActivity** (see the
  receipt-store-prune-scope note).
- `MIGRATION_11_12`: nullable `ALTER TABLE` adds (cheap in SQLite). Regenerate schema
  JSON → `12.json`. Add the instrumented migration test. Real migration, no destructive
  fallback (per db-migration-policy).

## Section 2 — PDF → AI extraction pipeline

Flow: pick PDF → render first 3 pages to images → one AI call → rich JSON → review.

1. **Pick** — system picker (`ACTION_OPEN_DOCUMENT`, `application/pdf` + `image/*`).
   Buffer the content URI to a `ByteArray` immediately (picker URIs are one-time-use).
2. **Render** — Android built-in `PdfRenderer` (no dependency): pages
   `0..min(2, pageCount-1)` → Bitmaps ~1500px wide → base64 JPEG (reuse
   `bitmapToBase64`). If the file is an image not a PDF, use it directly. Fewer than
   3 pages / render failure → send what exists.
   `ponytail:` first-3-pages cap; "scan more pages" deferred (YAGNI) until a real
   policy needs page 4+.
3. **One AI call** — new `PolicyDocParser`, same 4-provider chain
   (Groq→RoutesMe→NIM→OpenRouter) and `callProvider` as `UpiReceiptParser`, but sends
   the 3 page images in the `content` array + a policy-specific prompt. Returns strict
   JSON: core fields typed + rich part kept as a raw JSON string for `details_json`.
4. **Parse & map** — reuse the hardened helpers: `str()` null-token guard, ₹-amount
   regex, strict `YYYY-MM-DD` date parser (date-parse-greedy-year gotcha). Type hint
   fuzzy-matched to the insurance-type catalogue.

**Failure:** parse returns null → review screen opens blank + "couldn't read the PDF,
fill manually" (mirrors `ShareReceiptUiState.ScanError`). No crash, no data loss.

## Section 3 — UI

1. **Entry choice** — the Insurance FAB opens a bottom sheet with two options:
   ✏️ **Enter manually** (existing `InsuranceFormSheet`) / 📄 **Upload policy PDF**
   (picker → import screen). Reuse the two-pill visual pattern from the receipt
   Expense/Income toggle.

2. **Parsing + review screen** (`InsurancePolicyImportScreen`, modeled on
   `ShareReceiptScreen`):
   - **Scanning** — spinner + "Reading your policy…".
   - **Review** — extracted fields editable in the existing form layout (reuse
     `InsuranceFormSheet` fields), pre-filled from `PolicyData`; rich sections shown
     read-only below; PDF attached.
   - **Error** — "couldn't read it, fill manually" → blank manual form.
   - Save writes core columns + `details_json` + `policyDocUri`.

3. **Rich detail page** (`InsuranceDetailScreen`, enhanced) — sectioned like the
   mockup, each section rendering **only if** its JSON data exists (manual policies
   with no blob degrade gracefully to the core facts):
   - **Hero** — sum insured, insurer, type, status, validity bar.
   - **Premium & payment** — breakdown from JSON if present, else flat premium.
   - **Insured members** — cards from `members[]`.
   - **Nominee**, **Coverage & benefits**, **Riders**, **Waiting periods/exclusions**
     — each from its JSON array, collapsible.
   - **Insurer contacts** — helpline (dial button, reuse agent-dial), claims email, branch.
   - **Documents** — "View policy PDF" opens `policyDocUri`; existing linked-investment
     card stays.

   **List row** unchanged (name, provider, premium, sum assured, due countdown) — the
   redesign targets the detail page, which was the original complaint.

## Out of scope (deferred)

- Normalized member/rider/coverage tables.
- Multi-page batching / "scan more pages" beyond the first 3.
- Editing the rich JSON sections in-app (they're display-only; re-upload to refresh).

## Testing

- `MIGRATION_11_12` instrumented test (v11 → v12, columns present, old rows survive).
- `PolicyDocParser` JSON decode unit test (assert core fields + the rich blob passthrough,
  and a date assertion per the date-parse gotcha) using the sample policy's field set.
- Manual device test with the user's own real policy PDF.
