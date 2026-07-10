# Artha

A native Android personal finance manager built for the Indian context — local-first, privacy-respecting, and offline by default.

> *Artha* (अर्थ) — Sanskrit for *wealth, purpose, meaning*. One of the four *Puruṣārthas*.

---

## Overview

Artha is a single-user personal finance app designed around how money actually moves in India: UPI handles, joint family accounts, religious giving, RDs and SIPs, endowment-linked insurance, and the everyday mix of cash, cards, and bank transfers. Nothing leaves the device unless you explicitly export it.

- **Local-only storage** — Room database on-device. No cloud, no telemetry, no account required.
- **Indian-first** — ₹ symbol, lakh/crore grouping (`₹1,00,000`), pre-seeded categories including a *Religious & Spiritual* sub-tree, spouse-prompt for joint expenses.
- **Manual + AI entry** — Type transactions, or use AI Quick Entry (text / voice / photo) powered by your own Gemini API key.
- **Computed balances** — Account and card balances are derived from the transaction log, never stored independently. The ledger is the source of truth.
- **Material You** — Dynamic color on Android 12+, full light & dark themes on every screen.

---

## Features

### Core ledger
- Expense / Income / Transfer / Investment transactions
- Accounts (savings, current, cash, wallet) with auto-computed balances
- Credit cards with billing-cycle aware balances
- Hierarchical categories (pre-seeded + user-managed)
- Three hardcoded rules out of the box; user-defined rules engine

### Money flow
- Dashboard with hero balance strip and recent transactions
- Reports & analytics with Vico charts (category breakdown, trends)
- People — track who owes you and whom you owe
- Budgets, goals, subscriptions, and recurring transactions

### Investments & insurance
- RD / SIP / FD top-ups recorded as `INVESTMENT_BUY`
- Endowment-investment linkage so insurance premiums roll into your investment view

### AI Quick Entry (Phase 3)
- Natural-language input: *"paid 250 for chai at MG road"* → categorized draft transaction
- Voice and photo (receipt) input
- **Bring-your-own-key** — paste a Gemini key into Settings; it's validated against Gemini before being stored in DataStore. No key is baked into the APK.

### Hardening (Phase 5)
- Biometric / device-credential app lock
- AES-GCM encrypted local backup (export/import)
- Reports & analytics screens

---

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin (JVM 17) |
| UI | Jetpack Compose, Material 3, Material You dynamic color |
| Architecture | MVVM — `ViewModel` + `StateFlow` + Repository |
| Persistence | Room (KSP), DataStore Preferences |
| Navigation | Compose Navigation |
| Charts | Vico (`com.patrykandpatrick.vico:compose-m3`) |
| Date/time | `kotlinx-datetime` |
| AI | Google Generative AI SDK (Gemini) |
| Security | AndroidX Biometric, Security Crypto (EncryptedSharedPreferences) |
| Build | Android Gradle Plugin + Gradle 8.x |

**Min SDK:** 26 · **Target SDK:** 34 · **Compile SDK:** 35
**Package:** `com.subramanya.artha`

---

## Architecture

- **Single-module app** — package-by-feature: `data/`, `domain/`, `ui/<feature>/`, `ui/common/`.
- **One ViewModel per screen.** No shared/global ViewModels.
- **DAOs return `Flow<T>`;** repositories map DTOs to domain models.
- **Balances are derived, not stored.** Repositories cache and recompute on insert/update/delete — the transaction log is canonical.
- **No DI framework** — manual constructor injection keeps the dependency tree visible.
- **UUID string IDs**, `Double` for INR amounts (no `BigDecimal` — this isn't a bank), `Long` epoch-millis for dates.

---

## Getting started

### Prerequisites
- Android Studio (Hedgehog or newer)
- JDK 17
- An Android device or emulator running API 26+

### Build & run

```bash
# Clone
git clone https://github.com/subramanyaSgb/Artha.git
cd Artha

# Debug build
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug

# Unit tests
./gradlew test

# Instrumentation tests (needs device/emulator)
./gradlew connectedDebugAndroidTest

# Lint
./gradlew lint
```

On Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`.

### Enabling AI Quick Entry

AI Quick Entry is opt-in and per-install. There is **no build-time key** and **no `local.properties` hook** — keys are user-supplied and revocable from inside the app.

1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey).
2. In Artha: **Settings → AI Quick Entry → Paste key**.
3. The key is validated against Gemini, then stored locally in `SettingsPreferences` (DataStore).
4. Leave the field empty and AI Quick Entry stays disabled — the rest of the app works exactly the same.

---

## Project structure

```
Artha/
├── app/
│   ├── src/main/java/com/subramanya/artha/
│   │   ├── data/          # Room entities, DAOs, repositories
│   │   ├── domain/        # Pure-Kotlin domain models
│   │   ├── ui/
│   │   │   ├── common/    # Shared composables, theming
│   │   │   ├── dashboard/ # Per-feature screen packages
│   │   │   ├── people/
│   │   │   ├── reports/
│   │   │   └── ...
│   │   └── utils/         # IndianNumberFormat, formatters, etc.
│   └── build.gradle.kts
├── docs/
│   ├── PRD.md             # Source of truth for what to build
│   ├── phase1_tasks.md    # Phase-by-phase task list
│   └── ...
├── CLAUDE.md              # Project-level dev context
└── README.md
```

---

## Roadmap

| Phase | Scope | Status |
| --- | --- | --- |
| 1 | MVP skeleton, manual entry, bundled bank import, polish | Done |
| 2 | Investments + Insurance + Rules Engine + endowment linkage | Done |
| 3 | AI Quick Entry (text / voice / photo) via user-supplied Gemini key | Done |
| 4 | Budgets, Goals, Subscriptions, Recurring, People | Done |
| 5 | Biometric lock, Reports/Analytics, AES-GCM encrypted backup | Partial |
| — | SMS parsing receiver, recurring auto-fire (WorkManager), cloud sync | Deferred |

The app is feature-complete enough for daily use. Further work is treated as maintenance and extension.

---

## Design principles

- **Local-first.** No backend, no account, no telemetry. If a feature needs the cloud, it's not in Artha.
- **The ledger is canonical.** Balances are computed, not stored. Add a transaction → balances update everywhere.
- **Indian formatting is non-negotiable.** ₹1,00,000 — never `Rs.`, never `100,000`. All amount rendering goes through `utils/IndianNumberFormat.kt`.
- **Compose-only UI.** No `findViewById`, no XML layouts beyond the manifest, no View-based widgets.
- **Readable over clever.** Well-named identifiers, minimal comments, explicit null handling — no `!!`.

---

## Privacy

- Transactions, accounts, categories, and settings live in a Room database in the app's private storage.
- The optional Gemini key is stored in DataStore preferences on-device. It is sent only to Google's Gemini endpoint when you use AI Quick Entry.
- Backups are AES-GCM encrypted with a key derived from a user-supplied passphrase. Restore requires the same passphrase.
- Nothing is uploaded automatically. Ever.

---

## Contributing

Artha is a personal project, built for one user's workflow. Feature requests and forks are welcome, but expect opinionated design choices around Indian use cases.

If you're filing an issue, please include:
- Device + Android version
- Steps to reproduce
- Whether you had AI Quick Entry enabled (and the model behavior, if AI-related)

---

## License

TBD. Until a license is added, all rights are reserved by the maintainers.
