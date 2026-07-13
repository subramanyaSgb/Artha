# Artha — Personal Finance Manager

Artha is a native Android personal finance app for an Indian user (Subramanya GB). Local-only storage, manual transaction entry in Phase 1, AI Quick Entry via Gemini in Phase 3.

**For full feature specs, always read `docs/PRD.md`.** This file is the project-level context you read at the start of every session. The PRD is the source of truth for *what* to build; this file is *how* we build it.

---

## Project Memory (MANDATORY — read at start, update as we go)

A persistent memory lives at
`C:\Users\DSI-LPT-081\.claude\projects\d--SubramanyaGB-Test-Projects-Artha\memory\`.
`MEMORY.md` there is the index (one line per memory).
(Project was moved from `…\Desktop\SubramanyaGB\Test_Projects\Artha` to
`d:\SubramanyaGB\Test_Projects\Artha` on 2026-06-03; memory migrated with it. The
old `c--…Desktop…` memory dir is the historical copy.)

- **Every session, before planning:** read `memory/MEMORY.md` and open any memory
  file whose description looks relevant. Treat these as standing context alongside
  this file and the PRD.
- **As we work — after each meaningful exchange, not only at session end:** whenever
  we make a decision, hit a gotcha, agree on a convention, or the user states a
  preference, write or update the matching memory file *immediately*, then add/refresh
  its one-line pointer in `MEMORY.md`. One fact per file, kebab-case name, frontmatter
  with `type: user | feedback | project | reference`. Link related memories with
  `[[name]]`.
- **Don't duplicate** what the code, git history, or this CLAUDE.md already records —
  capture only the non-obvious *why*. Update the existing file instead of creating a
  near-duplicate; delete a memory that turns out wrong.
- **Project-wide decisions** (library choice, convention) also get reflected here in
  CLAUDE.md so they load even without memory recall.

Enforcement: a **Stop hook** (`.claude/hooks/memory-reminder.js`, wired in
`.claude/settings.json`) fires once at the end of each turn and blocks the stop to ask
whether memory needs updating — so the check happens automatically, not just from my own
diligence. It only nudges once per turn (guards on `stop_hook_active`), so trivial turns
just get a one-line "nothing to record." After editing these hook files, the hook only
goes live once `/hooks` is opened or Claude Code is restarted (the config watcher.)

---

## Current Phase

**Maintenance & extension.** All planned phases are complete — Phases 1–4 done, Phase 5 partial — see **Phase Roadmap (status)** at the bottom for exactly what's built and what's deferred. The app is feature-complete for daily use.

Work from the goal I state at session start; don't start speculative features. When I ask for something, first check whether it's already a deferred item (crash reporting, cloud sync) versus genuinely new. `docs/phase1_tasks.md` is retained as historical reference for the original MVP build.

---

## Tech Stack (use these exactly, do not substitute)

- Kotlin, target JVM 17
- Android Gradle Plugin latest stable, Gradle 8.x
- Jetpack Compose, Material 3, Material You dynamic color
- Light + dark themes (both required on every screen)
- Room (KSP, not KAPT) for local persistence
- MVVM with `ViewModel` + `StateFlow` + Repository pattern
- Compose Navigation (no Voyager, no third-party nav lib)
- Vico (`com.patrykandpatrick.vico:compose-m3`) for charts
- `kotlinx-datetime` for date/time
- Min SDK 26, Target SDK 34
- Package: `com.subramanya.artha`

## Architecture Conventions

- Single-module app for now. Do not introduce multi-module before Phase 3.
- Package by feature: `data/`, `domain/`, `ui/<feature>/`, `ui/common/`
- One ViewModel per screen. No SharedViewModel patterns.
- DAOs return `Flow<T>`; Repository maps DTOs → domain models
- **Account/Card balances are COMPUTED from transactions, never stored independently.** Cache in Repository, recompute on transaction insert/update/delete. This is non-negotiable.
- All amounts use `Double` (not `BigDecimal` in v1 — INR-only, we're not building a bank)
- All dates stored as `Long` (epoch millis); display via `kotlinx-datetime` formatters
- All IDs are UUID strings, not auto-increment ints
- **Hilt is NOT used in Phase 1.** Manual constructor injection only. Keep the dependency tree small.

## Codebase Architecture (big-picture wiring)

The conventions above say *what* the rules are; this is *where the wires actually connect* — the parts you'd otherwise have to reconstruct by reading `ArthaApplication`, `MainActivity`, and `ui/navigation/` together.

- **Manual DI lives in `ArthaApplication`.** Every repository, `SettingsPreferences`, the `AppDatabase`, and the `aiQuickEntryParser` are `by lazy` singletons on the Application instance. Anything that needs one reaches it via `LocalContext.current.applicationContext as ArthaApplication`, then hands it to a `ViewModel` through a `…ViewModelFactory` constructor. There is no service locator beyond this — if you add a repository, wire it here.
- **Repository-held shared flows use a process-lifetime `appScope`** (`SupervisorJob + Dispatchers.Default`) with `shareIn(WhileSubscribed)`. Balance/valuation flows only compute while a screen collects them. Pass `appScope` to any repository that caches a `shareIn` flow (see `AccountRepository`, `CardRepository`, `InvestmentRepository`).
- **Startup is a state machine in `MainActivity`.** `MainActivity` is a `FragmentActivity` (BiometricPrompt requires it). `ArthaRoot` applies theme + optional `BiometricLockGate`; `ArthaInner` runs a `Loading → NeedsOnboarding → Ready` machine that forces DB init + `CategorySeeder`, runs the bundled bank import once per `CURRENT_BUNDLED_IMPORT_VERSION`, prunes orphan receipts, then shows Splash / Onboarding / `MainApp`.
- **Navigation is split in `ui/navigation/`.** `ArthaDestination` = the bottom-nav tabs; `SubRoutes` = everything else (detail screens keyed by UUID arg, plus More-drawer destinations). `MainApp` owns the `Scaffold` (top bar, bottom bar, More sheet) and hosts `ArthaNavHost`. Deep-links (shared UPI receipt image via `ACTION_SEND`, SMS-review notification tap) arrive as intents → observable state → `LaunchedEffect` navigation.
- **Balance/valuation math is isolated in `data/balance/`** (`BalanceCalculator`, `BudgetCalculator`, `MonthlyTotals`) and unit-tested in `app/src/test/.../data/balance/`. This is the code the "balances are computed, never stored" rule protects — touch it only with tests.
- **AI parsing is behind the `AiQuickEntryParser` interface** (`ai/`). The live implementation is `NvidiaNimQuickEntryParser` (NIM, see the AI-provider note below). `GeminiQuickEntryParser` is legacy and no longer wired — don't extend it without checking `ArthaApplication.aiQuickEntryParser`.
- **Rules, recurring, and SMS are self-contained subsystems.** `domain/rules/` (`RuleEngine` + `RuleSpecJson` codec, stored as JSON in a `String` column), `domain/recurring/` (`RecurringFireEngine`) fired by `worker/RecurringFireWorker` (WorkManager, scheduled in `ArthaApplication.onCreate`), and `sms/` (`SmsReceiver` → `BankSmsParser` → `pending_sms` review queue surfaced by `ReviewScreen`). Each has JVM tests under `app/src/test/`.
- **Room specifics.** `AppDatabase` is currently **version 10**, `exportSchema = true` → `app/schemas/<v>.json` (committed, validated by `MigrationTestHelper`). Migrations live in `data/db/Migrations.kt`; first-run seeding runs through `db/seed/` callbacks. When you change the schema: bump the version, add a migration, regenerate the schema JSON, and add/adjust the instrumented migration test.

## Coding Style

- Composable functions over classes. One screen per file; small helpers in same file.
- No `var` in data classes.
- No `!!` operator. Use `requireNotNull(...)` or explicit null checks.
- All user-facing strings in `strings.xml`. No hardcoded strings in Composables.
- Indian number formatting via `utils/IndianNumberFormat.kt` helper — never `%,d`. `1,00,000` not `100,000`.
- INR symbol always prefixed: `₹1,000`. Never `Rs.` or `INR 1000`.
- ktlint or Spotless configured; run before commit.

## Build & Run

- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew test` (or `./gradlew testDebugUnitTest` for just the debug variant)
- Single test class/method: `./gradlew testDebugUnitTest --tests "com.subramanya.artha.data.balance.BalanceCalculatorTest"` (append `.methodName` for one method)
- Instrumentation tests: `./gradlew connectedDebugAndroidTest` (needs emulator/device; includes the Room `MigrationTestHelper` tests)
- On Windows, use `.\gradlew.bat` in place of `./gradlew`.
- Install on connected device: `./gradlew installDebug`
- Lint: `./gradlew lint`
- Format: configure ktlint / Spotless in Session 1

### Worktrees

Worktree directory convention: create feature worktrees under `.worktrees/`
(project-local, gitignored). `local.properties` (the SDK path) is gitignored and is
**not** copied into a new worktree, so copy it in or Gradle can't find the Android SDK.

### AI provider & key (updated 2026-07-03 — supersedes the Phase-3 BYOK note)

ALL AI tasks (AI Quick Entry + UPI receipt parsing) use **NVIDIA NIM**, model
`nvidia/nemotron-3-nano-omni-30b-a3b-reasoning`, via `HttpURLConnection` POST to
`https://integrate.api.nvidia.com/v1/chat/completions`. (Not Gemini; not glm-5.2/
minimax — nemotron-omni won on verified vision accuracy.) It's a reasoning model:
`reasoning_content` is separate, clean JSON stays in `message.content`; use
`max_tokens` ~4096.

**Key is now BAKED**, not BYOK. Single-user app: the key lives in `local.properties`
(gitignored) as `NIM_API_KEY`, is exposed via `BuildConfig.NIM_API_KEY`, and read by
`ArthaApplication.nimApiKey()` (a non-blank DataStore value still wins, but there's no
in-app UI to set one). The Settings key-entry UI was removed; only the AI on/off toggle
remains. **This intentionally reverses the earlier "no BuildConfig baking, keys
per-install" rule.**

⚠️ The repo + release APKs are PUBLIC, so the baked key is extractable by decompiling
the APK. Accepted tradeoff (NIM keys are free + revocable). Key never goes in source/git
— only `local.properties`. After a fresh clone/worktree, set `NIM_API_KEY` there (same
file as the SDK path) or AI silently no-ops.

## What NOT to Do

- Do not add features outside the current phase's scope. Check `docs/PRD.md` section 13 if unsure.
- Do not add cloud sync, Firebase, or any backend in Phase 1.
- Do not introduce DI frameworks (Hilt, Koin) before Phase 3.
- Do not add navigation libraries beyond Compose Navigation.
- Do not skip tests for balance-computation logic. It must be correct.
- Do not commit `local.properties`. Ensure it's in `.gitignore`.
- Do not use `findViewById`, XML layouts (except for the manifest), or View-based widgets. Pure Compose only.

## Working With Me on This Project

- At session start I'll state the goal. **Read this file, then read the relevant PRD section(s) before making any plan.**
- Plan briefly before editing: list the files you'll touch and the approach. Wait for my go-ahead on anything substantial.
- Make changes incrementally. Build between major steps; don't accumulate untested code.
- If we make a project-wide decision worth persisting (e.g., a library choice, a convention), update this CLAUDE.md so future sessions inherit it.
- Use git. Commit at logical checkpoints with conventional commits (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`).
- **Always verify `./gradlew assembleDebug` passes before declaring a task complete.**
- If a task drifts beyond its scope, surface it — don't silently grow it.

## Important Context About Me (the user)

- Technical Lead at Deevia Software, doing industrial computer vision in Python/FastAPI. Strong dev background, but **Android Compose is not my daily stack** — prefer well-commented, readable code over clever idioms.
- I prefer concise communication. Don't over-explain self-evident code.
- I run **Windows** for local dev. Path separators and shell commands should account for that — prefer cross-platform Gradle tasks over `.sh` scripts.
- I appreciate when you flag risky changes before making them, even if I'm in auto-accept mode.

## Phase Roadmap (status)

- **Phase 1 done** — MVP skeleton, manual entry, bundled bank import, polish pass.
- **Phase 2 done** — Investments + Insurance + Rules Engine UI + endowment-investment linkage.
- **Phase 3 done** — AI Quick Entry sheet (text/voice/photo) wired to Gemini. The API
  key is per-install BYOK: the user pastes it in **Settings → AI Quick Entry**, it's
  validated against Gemini, then stored in `SettingsPreferences` (DataStore) and read
  on-demand by `GeminiQuickEntryParser`. No `BuildConfig`/`local.properties` key.
  Empty key short-circuits to a friendly `NoApiKey` hint. (See the "Phase 3 — Gemini
  API key" section above.)
- **Phase 4 done** — Budgets + Goals + Subscriptions + Recurring + People.
- **Phase 5 partial** — Biometric/device-credential lock, Reports/Analytics, AES-GCM
  encrypted backup, **SMS auto-import (done, v0.13.0** — review-queue + hybrid regex/NIM
  parser, live-only RECEIVE_SMS; DB v7→v8 `pending_sms` table; see the `sms-auto-import`
  memory), recurring-rule auto-fire via WorkManager (done). **Deferred:** crash reporting,
  cloud sync. SMS follow-ups still open: Dashboard surfacing (only a Settings entry today)
  and transaction-level dedup (SMS vs shared UPI receipt).
- **Configurable pick-lists done (2026-06-06)** — All three phases shipped:
  Phase 1 (custom colours/icons + Settings manage-UI), Phase 2 (PaymentApp catalogue,
  DB v5→v6), Phase 3 (Account/Card/Insurance type catalogues, DB v6→v7). (The pick-list
  work landed the DB at v7; it has since advanced to **v10** — SMS auto-import and later
  migrations. `AppDatabase.version` is authoritative.) Four `enum` TypeConverters removed;
  columns are plain `String` with seeded
  built-in catalogues. Instrumented migration tests written; pending device-run.
- **Bug fixes (2026-06-06)** — Receipt thumbnail now renders (BitmapFactory decode
  from content URI); Investments LazyColumn trailing spacer added to clear FAB.

The app is feature-complete enough for daily use. Treat further work as
maintenance/extension rather than a fresh phase.
