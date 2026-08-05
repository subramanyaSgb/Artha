# Insurance Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Let the user add an insurance policy by uploading its PDF — AI extracts the fields and rich detail, saved alongside the stored PDF — and show a rich sectioned detail page; manual entry stays unchanged behind an entry-choice sheet.

**Architecture:** Extend the existing `insurances` table (v11→v12) with 5 flat columns + one `details_json` blob (no new tables). A new `PolicyDocParser` reuses `UpiReceiptParser`'s 4-provider vision chain; `PdfRenderer` rasterizes the first 3 PDF pages to images for one multi-image AI call. New import screen mirrors `ShareReceiptScreen`; the detail screen gains sections that render only when their JSON exists.

**Tech Stack:** Kotlin, Room (KSP), Jetpack Compose, `android.graphics.pdf.PdfRenderer` (built-in, no dep), `org.json`, existing `HttpURLConnection` vision chain.

**Branch:** `feature/insurance-redesign` (based off `release/v0.39.0` — it depends on the Groq/RoutesMe chain, which is NOT on main yet). Work in place (no worktree).

**Design ref:** `docs/plans/2026-08-05-insurance-redesign-design.md`. Test policy: `Desing_Plan_Insurence/Policy Bond Details.dc (1).html` (rendered sample; user will device-test with their real PDF).

---

## Task 1: DB migration v11→v12 (schema + columns)

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/data/db/Migrations.kt` (add after `MIGRATION_10_11`)
- Modify: `app/src/main/java/com/subramanya/artha/data/db/AppDatabase.kt:74` (version), and its `addMigrations(...)` call
- Modify: `app/src/main/java/com/subramanya/artha/data/entity/InsuranceEntity.kt` (add columns)
- Test: `app/src/androidTest/.../MigrationTest.kt` (existing MigrationTestHelper suite — add an 11→12 case)
- Generated: `app/schemas/com.subramanya.artha.data.db.AppDatabase/12.json`

**Step 1 — Add the migration.** In `Migrations.kt`, append:

```kotlin
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Insurance redesign: rich policy fields + one JSON blob for display-only
        // sections (members/riders/coverage/exclusions/contacts). All nullable so
        // existing rows stay valid. See 2026-08-05-insurance-redesign-design.md.
        db.execSQL("ALTER TABLE insurances ADD COLUMN plan_name TEXT")
        db.execSQL("ALTER TABLE insurances ADD COLUMN policy_term TEXT")
        db.execSQL("ALTER TABLE insurances ADD COLUMN life_assured TEXT")
        db.execSQL("ALTER TABLE insurances ADD COLUMN uin TEXT")
        db.execSQL("ALTER TABLE insurances ADD COLUMN insurer_helpline TEXT")
        db.execSQL("ALTER TABLE insurances ADD COLUMN details_json TEXT")
    }
}
```

**Step 2 — Bump version + register.** `AppDatabase.kt`: `version = 12`. Add `MIGRATION_11_12` to the `addMigrations(...)` list (find where `MIGRATION_10_11` is registered — same call).

**Step 3 — Add entity columns.** In `InsuranceEntity.kt`, add matching nullable fields with `@ColumnInfo`:

```kotlin
@ColumnInfo(name = "plan_name") val planName: String? = null,
@ColumnInfo(name = "policy_term") val policyTerm: String? = null,
@ColumnInfo(name = "life_assured") val lifeAssured: String? = null,
@ColumnInfo(name = "uin") val uin: String? = null,
@ColumnInfo(name = "insurer_helpline") val insurerHelpline: String? = null,
@ColumnInfo(name = "details_json") val detailsJson: String? = null,
```

**Step 4 — Regenerate schema JSON.** Run: `.\gradlew.bat :app:kspDebugKotlin` (JAVA_HOME pinned to Temurin 17). Expected: `app/schemas/.../12.json` created. If Room complains the entity/schema mismatch, fix column names to match exactly.

**Step 5 — Migration test.** Add an 11→12 case to the existing MigrationTestHelper test: create a v11 DB with one insurance row, run `MIGRATION_11_12`, assert the 6 new columns exist and the old row's data survives.

**Step 6 — Verify build.** Run: `.\gradlew.bat assembleDebug`. Expected: BUILD SUCCESSFUL.

**Step 7 — Commit.** `git add` the 4 files + `12.json` + test → `git commit -m "feat(insurance): DB v11->v12 add rich policy columns + details_json"`

---

## Task 2: Domain model + mapper + parser data class

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/domain/model/Insurance.kt` (add 6 fields)
- Modify: `InsuranceMapper` (in `InsuranceRepository.kt` or its own file — map new fields both ways)
- Create: `app/src/main/java/com/subramanya/artha/utils/PolicyData.kt`

**Step 1 — Extend `Insurance`** with the 6 new fields (`planName`, `policyTerm`, `lifeAssured`, `uin`, `insurerHelpline`, `detailsJson`), all `String? = null` (no `var`).

**Step 2 — Update the mapper** — add the 6 fields to both `toDomain()` and `toEntity()`. Missing a field here silently drops it on save; double-check both directions.

**Step 3 — Create `PolicyData`** — what the AI returns:

```kotlin
/** Core policy fields the AI extracts, plus the raw rich JSON for details_json. */
data class PolicyData(
    val name: String?, val typeHint: String?, val provider: String?,
    val policyNumber: String?, val sumAssured: Double?,
    val premiumAmount: Double?, val premiumFrequencyHint: String?,
    val startDateMillis: Long?, val endDateMillis: Long?, val nextDueMillis: Long?,
    val nominee: String?, val taxSection: String?,
    val planName: String?, val policyTerm: String?, val lifeAssured: String?,
    val uin: String?, val insurerHelpline: String?,
    /** The full rich extraction as raw JSON string → stored in details_json, rendered in detail. */
    val detailsJson: String?,
)
```

**Step 4 — Build.** `.\gradlew.bat assembleDebug` → SUCCESSFUL.

**Step 5 — Commit.** `git commit -m "feat(insurance): domain + mapper for rich fields; PolicyData"`

---

## Task 3: PolicyDocParser (AI extraction) — with decode test

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/utils/PolicyDocParser.kt`
- Test: `app/src/test/java/com/subramanya/artha/utils/PolicyDocParserTest.kt`

**Approach:** mirror `UpiReceiptParser` — same 4-provider chain, same `callProvider`/`post`/`str`/date helpers (extract shared helpers to a small `AiVisionHttp` util OR duplicate the few private helpers; prefer duplication if extraction balloons — decide during build, keep it lazy). Difference: accepts a `List<String>` of base64 page images (multi-image content array) and a policy-specific prompt; `decode()` returns `PolicyData` with the rich part passed through as a raw JSON string.

**Step 1 — Failing decode test.** Test `decode()` (make it internal/visible for test) with a JSON string matching the sample policy's fields:

```kotlin
@Test fun `decode maps core fields and passes rich blob through`() {
    val json = """{"name":"Care Supreme","provider":"Meridian Health Insurance Ltd.",
      "type":"HEALTH","policyNumber":"92838249","sumAssured":10000000,
      "premiumAmount":64780,"premiumFrequency":"SINGLE","startDate":"2024-11-22",
      "endDate":"2026-11-21","nominee":"Lakshmi Gopala","taxSection":"80D",
      "planName":"Care Supreme — Floater","policyTerm":"2 years",
      "lifeAssured":"Gopala Krishnan","uin":"MHIHLIP24063V012425",
      "insurerHelpline":"1800 266 4545",
      "members":[{"name":"Gopala Krishnan","relation":"Self","age":42}]}"""
    val d = PolicyDocParser(null,null,null,null).decodeForTest(json)
    assertEquals("Care Supreme", d?.name)
    assertEquals(10000000.0, d?.sumAssured)
    // date asserted explicitly (date-parse greedy-year gotcha)
    assertEquals(expectedNov22_2024Millis, d?.startDateMillis)
    assertTrue(d?.detailsJson?.contains("Gopala Krishnan") == true) // members blob passed through
}
```

**Step 2 — Run, expect FAIL** (class doesn't exist): `.\gradlew.bat :app:testDebugUnitTest --tests "com.subramanya.artha.utils.PolicyDocParserTest"`

**Step 3 — Implement `PolicyDocParser`.** Constructor takes the 4 key providers (same as UpiReceiptParser). `suspend fun parse(pageImagesB64: List<String>): PolicyData?` runs the provider chain sending all page images. `decode(json)` maps core fields (reuse `str()`, ₹-amount regex, strict `YYYY-MM-DD` `parseDate`), and sets `detailsJson = json.toString()` (or a filtered sub-object with just the rich keys). The prompt (verbatim in the file) demands strict JSON with core keys + rich arrays (members[], riders[], coverage[], exclusions[], contacts{}, premiumBreakdown{}, status, validity), dates as `YYYY-MM-DD`.

**Step 4 — Run test, expect PASS.**

**Step 5 — Build.** `.\gradlew.bat assembleDebug` → SUCCESSFUL.

**Step 6 — Commit.** `git commit -m "feat(insurance): PolicyDocParser AI extraction + decode test"`

---

## Task 4: PDF → images (PdfRenderer) — util + test-where-possible

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/utils/PdfToImages.kt`

**Step 1 — Implement.** `fun renderFirstPages(context: Context, uri: Uri, maxPages: Int = 3): List<String>` (base64 JPEGs):
- Buffer the URI to a ByteArray first (picker URIs are one-time-use — see that memory).
- If MIME/type is an image (not PDF), decode it directly to one base64 and return `[it]`.
- Else write bytes to a temp file, open `ParcelFileDescriptor` → `PdfRenderer`.
- For `page in 0 until min(maxPages, pageCount)`: render to a Bitmap sized ~1500px wide (white background — PDFs are transparent), `bitmapToBase64` (JPEG 85). Close each page.
- Wrap per-page render in runCatching; skip a page that fails. Return whatever succeeded.
- `ponytail:` first-3-pages cap; "scan more" is a future button.

**Step 2 — Build.** `.\gradlew.bat assembleDebug` → SUCCESSFUL. (PdfRenderer needs a device/instrumentation to exercise; unit-testable parts are the image-passthrough branch — add a tiny JVM test only if it's cheap, else rely on device test in Task 7.)

**Step 3 — Commit.** `git commit -m "feat(insurance): PdfRenderer first-3-pages to base64 images"`

---

## Task 5: Import ViewModel + screen (parse → review → save)

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/ui/insurance/InsurancePolicyImportViewModel.kt`
- Create: `app/src/main/java/com/subramanya/artha/ui/insurance/InsurancePolicyImportScreen.kt`
- Modify: `ArthaApplication.kt` — construct `PolicyDocParser` (wire all 4 keys) exposed like `aiQuickEntryParser`, OR build it in the ViewModel factory (match how `UpiReceiptParser` is built in `ShareReceiptScreen`).
- Modify: `ui/navigation/ArthaNavHost.kt` — add a `policyImport(uriString)` route + composable (mirror `shareReceipt` at :88 and its NavHost entry).
- Modify: `app/src/main/res/values/strings.xml` — import screen strings.

**Step 1 — ViewModel** modeled on `ShareReceiptViewModel`: `sealed interface state { Scanning; Parsed(editable fields incl. rich sections read-only + detailsJson); Error; Saved(insuranceId) }`. On init: `renderFirstPages(uri)` → `policyDocParser.parse(images)` → resolve typeHint against `insuranceTypeRepository.observeVisible()`, frequency hint against `PremiumFrequency`, pre-fill. `save()` builds `Insurance` (core fields + new columns + `detailsJson` + `policyDocUri = ReceiptStore.persist(context, uri)`) → `insuranceRepository.upsert`.

**Step 2 — Screen** modeled on `ShareReceiptScreen`: Scanning spinner ("Reading your policy…"); Parsed reuses `InsuranceFormSheet`'s field composables pre-filled, with the rich sections shown read-only below; Error → "couldn't read it, fill manually" button that routes to the manual form. TextFields hold local state (Compose-TextField-local-state memory).

**Step 3 — Nav wiring** — add route constant + `fun policyImport(uri)` + NavHost `composable(...)` entry with the uri arg, mirroring shareReceipt.

**Step 4 — Build.** `.\gradlew.bat assembleDebug` → SUCCESSFUL.

**Step 5 — Commit.** `git commit -m "feat(insurance): policy-PDF import screen + VM (parse/review/save)"`

---

## Task 6: Entry-choice sheet (manual vs upload)

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/insurance/InsurancesScreen.kt` — the FAB action.
- Modify: `strings.xml` — two option labels.

**Step 1 — Replace FAB-opens-form** with a small bottom sheet: ✏️ Enter manually (opens existing `InsuranceFormSheet`) / 📄 Upload policy PDF (launch `ACTION_OPEN_DOCUMENT` picker for `application/pdf`+`image/*` via `rememberLauncherForActivityResult`, then `navController` → `policyImport(uri)`). Reuse the two-pill visual from the receipt Expense/Income toggle (`ExpenseIncomeToggle` pattern in `ShareReceiptScreen.kt`).

**Step 2 — Build + Commit.** `.\gradlew.bat assembleDebug`; `git commit -m "feat(insurance): entry-choice sheet manual vs upload PDF"`

---

## Task 7: Rich detail page + ReceiptStore prune + device test

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/insurance/InsuranceDetailScreen.kt` — add sections.
- Modify: `InsuranceDetailViewModel.kt` — parse `detailsJson` into a display model.
- Modify: `MainActivity.kt` — add the insurance `policyDocUri` column to the ReceiptStore orphan-prune union set (receipt-store-prune-scope memory — REQUIRED so uploaded PDFs aren't pruned).
- Modify: `strings.xml` — section headers.

**Step 1 — Parse `detailsJson`** in the ViewModel into a small display data class (members, riders, coverage, exclusions, contacts, premiumBreakdown, status, validity) — all optional. Malformed/absent JSON → null → sections hidden.

**Step 2 — Detail sections**, each rendered only if its data is non-empty: Hero (existing, keep), Premium & payment breakdown, Insured members cards, Nominee, Coverage & benefits, Riders, Waiting periods/exclusions, Insurer contacts (helpline dial — reuse existing agent-dial), Documents ("View policy PDF" opens `policyDocUri` via intent). Manual policies (no blob) show only the existing core facts — verify that path still looks right.

**Step 3 — Prune scope** — add insurance's `policyDocUri` to the DAO-query union in MainActivity's `pruneOrphans`. (If skipped, uploaded PDFs get deleted on next startup.)

**Step 4 — Build.** `.\gradlew.bat assembleDebug` → SUCCESSFUL.

**Step 5 — Device test.** Install (`.\gradlew.bat installDebug`), upload the user's real policy PDF: verify extraction fills fields, PDF is stored + viewable, detail page shows the rich sections, and a manually-added policy still renders cleanly.

**Step 6 — Commit.** `git commit -m "feat(insurance): rich sectioned detail page + prune scope for policy PDFs"`

---

## Final: release

Per release-automation-always: bump version, build debug APK, push branch, `gh release create` with the APK. (Only after the branch is ready / merged per the user's call — v0.39.0 merge to main is a prerequisite discussion.)

## Notes / risks
- **Provider token limits:** 3 full-page images is more tokens than a receipt. If Groq 8K TPM/free-tier 429s, the chain falls through to the next provider automatically. Acceptable.
- **PdfRenderer** has no unit path — its correctness is proven by the Task 7 device test, not JVM tests.
- **Keep it lazy:** if extracting shared HTTP helpers from UpiReceiptParser gets messy, duplicate the ~40 lines instead of building an abstraction for two callers.
