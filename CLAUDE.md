# Artha — Personal Finance Manager

Artha is a native Android personal finance app for an Indian user (Subramanya GB). Local-only storage, manual transaction entry in Phase 1, AI Quick Entry via Gemini in Phase 3.

**For full feature specs, always read `docs/PRD.md`.** This file is the project-level context you read at the start of every session. The PRD is the source of truth for *what* to build; this file is *how* we build it.

---

## Current Phase

**Phase 1 — MVP Skeleton.** See `docs/phase1_tasks.md` for the session-by-session task list. Do not build features outside Phase 1 scope unless I explicitly ask.

Phase scope reminder: Splash, Onboarding, Dashboard, Transactions list, Add Transaction (Expense/Income/Transfer), Accounts + detail, Cards + detail, Settings, Categories management, More drawer. Pre-seeded categories including Religious & Spiritual sub-tree. Spouse-prompt dialog. Three hardcoded rules. No AI, no investments, no insurance, no budgets/goals/subs/recurring, no SMS parsing, no cloud.

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
- Unit tests: `./gradlew test`
- Instrumentation tests: `./gradlew connectedDebugAndroidTest` (needs emulator/device)
- Install on connected device: `./gradlew installDebug`
- Lint: `./gradlew lint`
- Format: configure ktlint / Spotless in Session 1

### Phase 3 — Gemini API key

AI Quick Entry uses Google's Generative AI SDK. The user pastes their own key
into **Settings → AI Quick Entry**, which validates the key against Gemini
before storing it in `SettingsPreferences` (DataStore). No build-time key, no
`local.properties` hook, no `BuildConfig` baking — keys are per-install and
revocable from inside the app. Empty key → the parser returns `NoApiKey` and
the UI nudges the user toward Settings.

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
- **Phase 3 done** — AI Quick Entry sheet (text/voice/photo) wired to Gemini via
  BuildConfig.GEMINI_API_KEY. Empty key short-circuits to a friendly hint.
- **Phase 4 done** — Budgets + Goals + Subscriptions + Recurring + People.
- **Phase 5 partial** — Biometric/device-credential lock, Reports/Analytics, AES-GCM
  encrypted backup. **Deferred:** SMS parsing receiver, recurring-rule auto-fire via
  WorkManager, crash reporting, cloud sync. SMS in particular needs a bank-specific
  regex catalogue + runtime-permission UX flow; the Settings toggle + DataStore key
  are already in place so the receiver can drop in cleanly.

The app is feature-complete enough for daily use. Treat further work as
maintenance/extension rather than a fresh phase.
