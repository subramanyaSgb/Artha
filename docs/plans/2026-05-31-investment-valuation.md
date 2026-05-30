# Investment Valuation Redesign — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Each code/bugfix step uses superpowers:test-driven-development (red → green → commit).

**Goal:** Make deposit investments (FD/RD/PPF/EPF/Bonds) derive their displayed value from contributions + posted interest so top-ups update the headline number, while market instruments keep a manual current value. Preserve existing data via the project's first real Room migration.

**Architecture:** Add `valuationMode` (DERIVED|MARKET) and `openingContribution` to `InvestmentEntity`. Value becomes computed in `BalanceCalculator` (`value = invested + interest` for DERIVED, `= currentValue` for MARKET; `gain = value − invested` uniformly). Interest is an `INTEREST` transaction whose destination is the investment — it grows the deposit and already counts as income in `MonthlyAggregator`. A `Migration(3,4)` back-fills `opening_contribution = current_value` and sets `valuation_mode` by type.

**Tech Stack:** Kotlin, Room (KSP), Jetpack Compose, MVVM, JUnit4. Pure-logic in `data/balance` is unit-tested with `./gradlew test`; the migration has an instrumented test (needs a device/emulator).

**Build/test note (Windows):** every Gradle call must pin JDK 17 inline — env vars don't persist between shells:
```
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; Set-Location "c:\Users\DSI-LPT-081\Desktop\SubramanyaGB\Test_Projects\Artha\.worktrees\investment-valuation"; .\gradlew.bat test --console=plain
```
Worktree: `.worktrees/investment-valuation` (branch `feat/investment-valuation`), `local.properties` already copied in.

**Key risk — data safety:** `DatabaseProvider` currently uses `.fallbackToDestructiveMigration()` (any schema bump WIPES data). Task 4 adds a real migration AND keeps fallback only as a last-resort backstop, so the user's live phone data survives the v3→v4 upgrade. Verify the migration before shipping.

---

### Task 1: `ValuationMode` enum + default-by-type mapping

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/data/entity/enums/ValuationMode.kt`
- Test: `app/src/test/java/com/subramanya/artha/data/entity/enums/ValuationModeTest.kt`

**Step 1 — Write the failing test**
```kotlin
package com.subramanya.artha.data.entity.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class ValuationModeTest {
    @Test fun `deposit types default to DERIVED`() {
        listOf(InvestmentType.FD, InvestmentType.RD, InvestmentType.PPF,
               InvestmentType.EPF, InvestmentType.BONDS).forEach {
            assertEquals("$it should be DERIVED", ValuationMode.DERIVED, it.defaultValuationMode())
        }
    }

    @Test fun `market types default to MARKET`() {
        listOf(InvestmentType.SIP, InvestmentType.MUTUAL_FUND, InvestmentType.EQUITY,
               InvestmentType.GOLD_PHYSICAL, InvestmentType.GOLD_DIGITAL, InvestmentType.NPS,
               InvestmentType.ULIP, InvestmentType.OTHER).forEach {
            assertEquals("$it should be MARKET", ValuationMode.MARKET, it.defaultValuationMode())
        }
    }
}
```

**Step 2 — Run, expect FAIL** (`ValuationMode` unresolved):
`...gradlew.bat test --tests "*ValuationModeTest" --console=plain` → FAIL (compile).

**Step 3 — Implement**
```kotlin
package com.subramanya.artha.data.entity.enums

/**
 * How an investment's current value is determined.
 *  - DERIVED: deposits (FD/RD/PPF/EPF/Bonds). value = contributions + posted interest.
 *  - MARKET:  market instruments. value = the manually-entered current price.
 */
enum class ValuationMode { DERIVED, MARKET }

/** Sensible default mode for a freshly-created investment of this type. */
fun InvestmentType.defaultValuationMode(): ValuationMode = when (this) {
    InvestmentType.FD, InvestmentType.RD, InvestmentType.PPF,
    InvestmentType.EPF, InvestmentType.BONDS -> ValuationMode.DERIVED
    else -> ValuationMode.MARKET
}
```

**Step 4 — Run, expect PASS.**

**Step 5 — Commit**
```
git add app/src/main/java/.../enums/ValuationMode.kt app/src/test/java/.../enums/ValuationModeTest.kt
git commit -m "feat(investments): add ValuationMode enum + default-by-type mapping"
```

---

### Task 2: `BalanceCalculator` — opening principal, interest, computed value

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/data/balance/BalanceCalculator.kt`
- Test: `app/src/test/java/com/subramanya/artha/data/balance/InvestmentValuationTest.kt` (new)

**Step 1 — Write failing tests.** Reuse the `buy/sell/txn` helper style from `InvestmentInvestedTest.kt`. Add an `interest()` helper: `INTEREST` txn with `sourceType = EXTERNAL`, `destinationType = INVESTMENT`, `destinationId = inv`.
```kotlin
// invested now includes openingContribution
@Test fun `invested includes opening contribution`() {
    val txns = listOf(buy(accountA, investmentX, 5_000.0))
    assertEquals(65_000.0,
        BalanceCalculator.computeInvestmentInvested(investmentX, txns, openingContribution = 60_000.0), EPS)
}

@Test fun `interest credited to the investment is summed`() {
    val txns = listOf(interest(to = investmentX, amount = 400.0))
    assertEquals(400.0, BalanceCalculator.computeInvestmentInterest(investmentX, txns), EPS)
}

@Test fun `derived value is opening plus contributions plus interest`() {
    val txns = listOf(buy(accountA, investmentX, 5_000.0), interest(investmentX, 400.0))
    val v = BalanceCalculator.computeInvestmentValue(
        mode = ValuationMode.DERIVED, currentValue = 0.0,
        openingContribution = 60_000.0, investmentId = investmentX, transactions = txns)
    assertEquals(65_400.0, v, EPS)   // the user's RD case + interest
}

@Test fun `market value is the manual current value regardless of contributions`() {
    val txns = listOf(buy(accountA, investmentX, 5_000.0))
    val v = BalanceCalculator.computeInvestmentValue(
        mode = ValuationMode.MARKET, currentValue = 90_000.0,
        openingContribution = 0.0, investmentId = investmentX, transactions = txns)
    assertEquals(90_000.0, v, EPS)
}

@Test fun `interest does not count as invested`() {
    val txns = listOf(buy(accountA, investmentX, 5_000.0), interest(investmentX, 400.0))
    assertEquals(5_000.0, BalanceCalculator.computeInvestmentInvested(investmentX, txns), EPS)
}
```
(Add the `interest` + needed helpers locally in this test file, mirroring `InvestmentInvestedTest`.)

**Step 2 — Run, expect FAIL** (new fns + param unresolved).

**Step 3 — Implement.** In `BalanceCalculator`:
- Add `openingContribution: Double = 0.0` param to `computeInvestmentInvested`, seed `var invested = openingContribution`. (Default keeps existing `InvestmentInvestedTest` and call-sites compiling.)
- Add:
```kotlin
/** Interest credited INTO this investment (compounding deposits). */
fun computeInvestmentInterest(investmentId: String, transactions: List<TransactionEntity>): Double {
    var interest = 0.0
    for (txn in transactions) {
        if (txn.type == TransactionType.INTEREST &&
            txn.destinationType == SourceKind.INVESTMENT &&
            txn.destinationId == investmentId
        ) interest += txn.amount
    }
    return interest
}

/** Displayed value of an investment, per its valuation mode. */
fun computeInvestmentValue(
    mode: ValuationMode,
    currentValue: Double,
    openingContribution: Double,
    investmentId: String,
    transactions: List<TransactionEntity>,
): Double = when (mode) {
    ValuationMode.MARKET -> currentValue
    ValuationMode.DERIVED ->
        computeInvestmentInvested(investmentId, transactions, openingContribution) +
            computeInvestmentInterest(investmentId, transactions)
}
```
Add `import ...enums.ValuationMode`.

**Step 4 — Run, expect PASS.** Also run the whole suite (`...gradlew.bat test`) — `InvestmentInvestedTest` must still pass unchanged.

**Step 5 — Commit** `feat(investments): compute invested(+opening), interest, and per-mode value`.

---

### Task 3: Entity + domain model + mapper fields

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/data/entity/InvestmentEntity.kt`
- Modify: `app/src/main/java/com/subramanya/artha/domain/model/Investment.kt`
- Modify: `app/src/main/java/com/subramanya/artha/data/mapper/InvestmentMapper.kt`

**Steps:**
1. `InvestmentEntity`: add fields (update the stale class JavaDoc too):
   ```kotlin
   @ColumnInfo(name = "valuation_mode") val valuationMode: ValuationMode,
   @ColumnInfo(name = "opening_contribution") val openingContribution: Double,
   ```
   (import `ValuationMode`.) Keep `currentValue` (used by MARKET).
2. `Investment` domain: add `val valuationMode: ValuationMode` and `val openingContribution: Double`.
3. `InvestmentMapper`: map both new fields in `toDomain()` and `toEntity()`.
4. Compile-check: `...gradlew.bat compileDebugKotlin`. This WILL surface every construction site of `Investment`/`InvestmentEntity` (form VM, seeders, tests). Fix each to pass the new fields — for now in non-form call-sites default `valuationMode = type.defaultValuationMode()` and `openingContribution = currentValue` (preserves current behavior). The form VM is properly handled in Task 8.
5. Run `...gradlew.bat test` — green.

**Commit** `feat(investments): add valuationMode + openingContribution to entity/domain/mapper`.

---

### Task 4: Room migration v3 → v4 (data-preserving)

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/data/db/AppDatabase.kt` (`version = 3` → `4`)
- Create: `app/src/main/java/com/subramanya/artha/data/db/Migrations.kt`
- Modify: `app/src/main/java/com/subramanya/artha/data/db/DatabaseProvider.kt` (add `.addMigrations(MIGRATION_3_4)`, keep `.fallbackToDestructiveMigration()` as backstop)
- Modify: `app/src/main/java/com/subramanya/artha/data/db/Converters.kt` (add `ValuationMode` converter)
- Test: `app/src/androidTest/java/com/subramanya/artha/data/db/InvestmentMigrationTest.kt` (instrumented — runs on device)

**Steps:**
1. Converter:
   ```kotlin
   @TypeConverter fun fromValuationMode(v: ValuationMode): String = v.name
   @TypeConverter fun toValuationMode(v: String): ValuationMode = ValuationMode.valueOf(v)
   ```
2. `Migrations.kt`:
   ```kotlin
   package com.subramanya.artha.data.db
   import androidx.room.migration.Migration
   import androidx.sqlite.db.SupportSQLiteDatabase

   val MIGRATION_3_4 = object : Migration(3, 4) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE investments ADD COLUMN opening_contribution REAL NOT NULL DEFAULT 0")
           db.execSQL("UPDATE investments SET opening_contribution = current_value")
           db.execSQL("ALTER TABLE investments ADD COLUMN valuation_mode TEXT NOT NULL DEFAULT 'MARKET'")
           db.execSQL("UPDATE investments SET valuation_mode = 'DERIVED' " +
               "WHERE type IN ('FD','RD','PPF','EPF','BONDS')")
       }
   }
   ```
3. Bump `@Database(version = 4 ...)`. Wire `.addMigrations(MIGRATION_3_4)` in `DatabaseProvider.build()` before `.fallbackToDestructiveMigration()`.
4. Instrumented test (`room-testing` may need adding to `build.gradle.kts` as `androidTestImplementation`; verify): create a v3 `investments` row (RD, current_value=60000) + a v3 `INVESTMENT_BUY` of 5000 to it, run `MigrationTestHelper.runMigrationsAndValidate(db, 4, true, MIGRATION_3_4)`, assert `opening_contribution = 60000`, `valuation_mode = 'DERIVED'`, and (via the DAO post-open) computed value = 65000.
5. Verify: unit suite green (`...gradlew.bat test`). The instrumented test runs with `...gradlew.bat connectedDebugAndroidTest` **only when an emulator/device is attached** — if none is available, note it as pending device verification (do NOT claim it passed).

**Commit** `feat(db): migration v3->v4 for investment valuation (data-preserving)`.

---

### Task 5: `InvestmentWithMetrics.value` + repository computes per mode

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/domain/model/Investment.kt` (`InvestmentWithMetrics`)
- Modify: `app/src/main/java/com/subramanya/artha/data/repository/InvestmentRepository.kt`

**Steps:**
1. Add `val value: Double` to `InvestmentWithMetrics`; change the `absoluteGain` doc to "value − invested".
2. In `observeActiveWithMetrics()` replace the body per investment:
   ```kotlin
   val invested = BalanceCalculator.computeInvestmentInvested(entity.id, txns, entity.openingContribution)
   val value = BalanceCalculator.computeInvestmentValue(
       entity.valuationMode, entity.currentValue, entity.openingContribution, entity.id, txns)
   val gain = value - invested
   val pct = if (invested == 0.0) Double.NaN else (gain / invested) * 100.0
   InvestmentWithMetrics(entity.toDomain(), invested, value, gain, pct)
   ```
3. Update `observeInvested(id)` to pass `openingContribution` (fetch the entity in the flow, or add an `observeValue(id)` helper used by the detail screen).
4. Verify build + `...gradlew.bat test`.

**Commit** `feat(investments): expose computed value on InvestmentWithMetrics`.

---

### Task 6: Investments list + hero display

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/investments/InvestmentsViewModel.kt:31`
- Modify: `app/src/main/java/com/subramanya/artha/ui/investments/InvestmentsScreen.kt` (hero ~206-224, row ~413-416)

**Steps (read each file first):**
1. `InvestmentsViewModel`: `totalCurrentValue = metrics.sumOf { it.value }` (was `it.investment.currentValue`).
2. Row + hero: render `row.value` instead of `row.investment.currentValue`. For DERIVED rows show subline "Invested ₹X · Interest ₹(value−invested)"; for MARKET keep "Invested ₹X · Gain ₹Z (p%)". Use existing `IndianNumberFormat`; new strings in `strings.xml` (e.g. `investments_subline_interest`).
3. Build + `...gradlew.bat test` (+ manual: app shows RD = 65,000).

**Commit** `feat(investments): list/hero use computed value`.

---

### Task 7: Investment detail — value, interest action, contribution shortcut

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/investments/InvestmentDetailViewModel.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ui/investments/InvestmentDetailScreen.kt`

**Steps (read files first):**
1. Detail VM: compute `value` via `BalanceCalculator.computeInvestmentValue(...)` and `gain = value − invested` (replace the `investment.currentValue - invested` at ~line 52). Expose `value`, `invested`, `interest`, `gain`, `pct`.
2. Hero (`HeroBlock`, ~195-243): show `value`; DERIVED → "Invested · Interest"; MARKET → "Invested · Gain (%)".
3. Add a **"Post interest"** action (visible for DERIVED): opens a small sheet (amount + date) → creates an `INTEREST` transaction `sourceType=EXTERNAL, destinationType=INVESTMENT, destinationId=this` via `transactionRepository.save(...)`. (Mirror how the INVEST tab builds a transaction.)
4. Add a **"Add contribution"** button that launches the existing Add Transaction sheet on the INVEST tab pre-filled with destination = this investment.
5. Strings in `strings.xml`. Build + `...gradlew.bat test`.

**Commit** `feat(investments): detail shows computed value + post-interest/contribution actions`.

---

### Task 8: Investment form — type-aware value field

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/investments/InvestmentFormSheet.kt`
- Modify: the investment form ViewModel/state (same package)

**Steps (read files first):**
1. On type selection, set `valuationMode = type.defaultValuationMode()`.
2. DERIVED: render one field "Amount currently in this RD/FD" → `openingContribution`; do not show a manual current-value field; show a read-only computed value when editing an existing one. Persist `currentValue = openingContribution` for new DERIVED rows (harmless; value is computed).
3. MARKET: render "Current value" → `currentValue` and optional "Invested so far" → `openingContribution`.
4. Ensure `upsert`/`update` carry both new fields. Strings in `strings.xml`. Build + `...gradlew.bat test`.

**Commit** `feat(investments): type-aware value field in the add/edit form`.

---

### Task 9: Dashboard net worth uses computed value

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/dashboard/DashboardViewModel.kt`

**Steps:**
1. Find where investments contribute to net position (currently sums `currentValue`). Switch to summing `InvestmentWithMetrics.value` (reuse `investmentRepository.observeActiveWithMetrics()` or add `observeTotalValue()`).
2. Build + `...gradlew.bat test`. Manual: dashboard net worth reflects the RD increase.

**Commit** `feat(dashboard): net worth uses computed investment value`.

---

### Task 10: Full verification

**Steps:**
1. `...gradlew.bat test --console=plain` → all green (run superpowers:verification-before-completion; paste real output).
2. `...gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
3. If a device/emulator is available: `...gradlew.bat connectedDebugAndroidTest` for the migration test; otherwise record it as pending device verification.
4. Manual smoke (or guide the user): existing RD now shows 65,000; posting ₹400 interest → 65,400 and appears as income; a market investment still uses manual value.
5. Then superpowers:finishing-a-development-branch to merge `feat/investment-valuation` → `main`.

**Commit** any final touch-ups; the branch is ready to merge.

---

## Notes / open verification
- `room-testing` dependency may need adding for Task 4's instrumented test — confirm in `app/build.gradle.kts`.
- Interest transactions use `sourceType = EXTERNAL` (external money into the deposit) so they never touch account balances and still count as income in `MonthlyAggregator`.
- No change to `TransactionType`/`SourceKind`/`CategoryType` (load-bearing). Configurability of cosmetic pick-lists is a separate, later effort.
