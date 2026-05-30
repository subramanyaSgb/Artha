# Artha — Personal Finance Manager

Artha is a native Android personal finance app for an Indian user (Subramanya GB). Local-only storage, manual transaction entry in Phase 1, AI Quick Entry via Gemini in Phase 3.

**For full feature specs, always read `docs/PRD.md`.** This file is the project-level context you read at the start of every session. The PRD is the source of truth for *what* to build; this file is *how* we build it.

---

## Project Memory (MANDATORY — read at start, update as we go)

A persistent memory lives at
`C:\Users\DSI-LPT-081\.claude\projects\c--Users-DSI-LPT-081-Desktop-SubramanyaGB-Test-Projects-Artha\memory\`.
`MEMORY.md` there is the index (one line per memory).

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

Work from the goal I state at session start; don't start speculative features. When I ask for something, first check whether it's already a deferred item (SMS parsing, recurring auto-fire via WorkManager, crash reporting, cloud sync) versus genuinely new. `docs/phase1_tasks.md` is retained as historical reference for the original MVP build.

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
- **Phase 3 done** — AI Quick Entry sheet (text/voice/photo) wired to Gemini. The API
  key is per-install BYOK: the user pastes it in **Settings → AI Quick Entry**, it's
  validated against Gemini, then stored in `SettingsPreferences` (DataStore) and read
  on-demand by `GeminiQuickEntryParser`. No `BuildConfig`/`local.properties` key.
  Empty key short-circuits to a friendly `NoApiKey` hint. (See the "Phase 3 — Gemini
  API key" section above.)
- **Phase 4 done** — Budgets + Goals + Subscriptions + Recurring + People.
- **Phase 5 partial** — Biometric/device-credential lock, Reports/Analytics, AES-GCM
  encrypted backup. **Deferred:** SMS parsing receiver, recurring-rule auto-fire via
  WorkManager, crash reporting, cloud sync. SMS in particular needs a bank-specific
  regex catalogue + runtime-permission UX flow; the Settings toggle + DataStore key
  are already in place so the receiver can drop in cleanly.

The app is feature-complete enough for daily use. Treat further work as
maintenance/extension rather than a fresh phase.
