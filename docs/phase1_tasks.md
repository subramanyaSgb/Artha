# Phase 1 — Task Playbook for Claude Code

This is the session-by-session task list for building the Artha MVP with Claude Code in VS Code.

**How to use:** Open VS Code with the project folder, open Claude Code (Spark icon), and paste the **Prompt** block for each session in order. Verify the **Done when** checklist before moving to the next session.

Recommended pace: one session per sitting. Don't rush — let each session finish cleanly with a working build and a git commit.

---

## Session 0 — Prerequisites (one-time, do this before starting)

Do these **outside Claude Code** first:

- [ ] Install JDK 17 (`winget install Microsoft.OpenJDK.17` on Windows, or download from Adoptium)
- [ ] Install Android Studio (for SDK, ADB, AVD). You don't have to edit in it.
- [ ] Set `ANDROID_HOME` environment variable to your SDK path (typically `%LOCALAPPDATA%\Android\Sdk` on Windows)
- [ ] Install Node.js LTS
- [ ] Install Claude Code globally: `npm install -g @anthropic-ai/claude-code`
- [ ] Install the official Claude Code VS Code extension (search "Claude Code" in Extensions, publisher: Anthropic)
- [ ] Create a Pixel 7 AVD in Android Studio (API 34) so the emulator is ready
- [ ] Create an empty folder, e.g. `D:\projects\artha`. Open it in VS Code.
- [ ] Place `CLAUDE.md` at the root of that folder
- [ ] Create `docs/` folder and place `PRD.md` inside (rename `finance_app_prd.md` → `PRD.md`)
- [ ] Place `phase1_tasks.md` (this file) in `docs/`
- [ ] `git init`, then `git add . && git commit -m "chore: project context and PRD"`
- [ ] Open Claude Code (Spark icon in sidebar) and authenticate with your Max plan

You should now have:

```
artha/
├── CLAUDE.md
├── docs/
│   ├── PRD.md
│   └── phase1_tasks.md
└── .git/
```

---

## Session 1 — Project Bootstrap

**Goal:** Initialize the Android project with our exact tech stack. Verify it builds.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md sections 1-5 (Vision, User, Problem, Principles, Tech Stack).

Initialize an Android project skeleton in this folder with our exact tech stack from CLAUDE.md:
- Single module, package com.subramanya.artha
- Kotlin, Gradle 8.x, AGP latest stable, JVM 17
- Jetpack Compose + Material 3 + Material You
- Room with KSP, kotlinx-datetime, Compose Navigation, Vico for charts
- Min SDK 26, Target SDK 34
- Both light and dark theme files
- ktlint or Spotless configured
- Proper .gitignore (gradle, IDE, local.properties, build outputs)

Do NOT use Hilt, do NOT use kapt, do NOT add any nav library beyond Compose Navigation.

Plan the file structure first, show me, then create files. After creation, run ./gradlew assembleDebug and confirm it passes. Do not create any app screens yet — just the empty MainActivity with an empty NavHost and a placeholder Composable that shows "Artha — Phase 1 setup complete".
```

**Done when:**
- [ ] `./gradlew assembleDebug` succeeds
- [ ] App installs and shows the placeholder text on emulator
- [ ] `.gitignore` excludes `local.properties`, build outputs, `.idea/`
- [ ] Committed: `chore: project bootstrap`

---

## Session 2 — Data Layer (Room entities, DAOs, Repository, Category seeder)

**Goal:** All Room entities from PRD section 8.1, DAOs, the Repository, and category pre-seeding. Tests for balance computation.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md sections 8 (Data Model) and 9 (Pre-seeded Categories).

Build the complete data layer for Phase 1:

1. All Room entities from section 8.1 except those reserved for later phases. For Phase 1 we need: Account, Card, Category, Person, Tag, Transaction, TransactionPersonCrossRef, TransactionTagCrossRef. Skip Investment, Insurance, Budget, Goal, Subscription, RecurringRule, TransactionRule (those come in Phase 2-4).

2. DAOs returning Flow<T> for queries, suspend for mutations.

3. AppDatabase singleton with all entities. Use Room version 1 with no migrations yet (destructive migration in debug).

4. Repository classes (AccountRepository, CardRepository, TransactionRepository, CategoryRepository, PersonRepository, TagRepository) using constructor injection (no DI framework).

5. A CategorySeeder that populates the full category tree from PRD section 9 on first DB creation. Use Room's RoomDatabase.Callback. Every pre-seeded category must have isSystem = true. Use Material icon names that exist; fall back to "category" if unsure.

6. Balance computation: in AccountRepository, expose currentBalance(accountId): Flow<Double> that sums openingBalance + transactions affecting that account. Same for Card outstanding. These must be derived, not stored.

7. Unit tests for balance computation: at least 6 cases covering expense, income, transfer from account, transfer to account, card payment behavior, and the cumulative balance across multiple transactions.

Show me the plan first. Then implement. After implementation, run ./gradlew test and confirm all tests pass.
```

**Done when:**
- [ ] All entities, DAOs, Repositories exist
- [ ] Category seeder runs on first launch (verify by querying Room in a debug Composable temporarily)
- [ ] Balance tests all pass
- [ ] `./gradlew assembleDebug && ./gradlew test` both green
- [ ] Committed: `feat(data): room entities, repositories, category seeder, balance tests`

---

## Session 3 — Theming, Navigation Scaffold, Bottom Nav

**Goal:** Material 3 theme (light + dark + Material You), Compose Navigation, bottom nav with 5 items + More drawer.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md sections 6 (Information Architecture) and 7.11 (More Drawer).

Build the navigation and theming scaffold:

1. Material 3 theme with seed color #0F766E (teal-700), light + dark color schemes, Material You dynamic color enabled on Android 12+ with fallback.

2. Typography: use Material 3 defaults but ensure Indian number display reads well — make sure tabular figures are used for amount displays.

3. Compose Navigation NavHost with routes for: dashboard, transactions, accounts, cards, more. Stub Composables for each (just show their name in a Surface for now).

4. A Scaffold with bottom nav showing 5 items: Dashboard (icon: dashboard), Transactions (icon: receipt_long), Accounts (icon: account_balance), Cards (icon: credit_card), More (icon: more_horiz).

5. "More" item opens a modal bottom sheet (ModalBottomSheet) listing: Categories (enabled), Settings (enabled), About (enabled), Investments/Insurance (disabled with "Coming in Phase 2" subtitle), Budgets/Goals/Subscriptions/Recurring/People/Reports (disabled with "Coming in Phase 4" subtitle).

6. App bar at top with greeting "Hello, Guest 👋" placeholder on the left and today's date formatted as "Thu, 21 May" on the right.

Test both light and dark by toggling system theme on the emulator. Confirm Material You works on API 31+ (color changes when device wallpaper changes).
```

**Done when:**
- [ ] Bottom nav switches between 5 stub screens
- [ ] More drawer opens and shows enabled + disabled tiles correctly
- [ ] Light and dark themes both look good
- [ ] App bar shows date in correct format
- [ ] Committed: `feat(ui): theme, navigation scaffold, bottom nav with more drawer`

---

## Session 4 — Splash + Onboarding

**Goal:** End-to-end first-run flow. New user goes Splash → 3-step Onboarding → empty Dashboard.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md section 7.1 (Splash) and 7.2 (Onboarding).

Build the first-run flow:

1. Splash screen with a placeholder logo (teal circle with white "अ" Devanagari character, or "A" if "अ" doesn't render), app name "Artha", tagline "Your money. Your rules." Auto-dismiss after Room DB init completes, minimum 500ms.

2. Onboarding 3 steps using HorizontalPager:
   - Step 1: Welcome with body text about local-only storage (text from PRD)
   - Step 2: Name input (single TextField, "Next" disabled until non-empty)
   - Step 3: Add first account form — Name, Type chip picker (Bank Savings / Bank Current / Cash / Wallet), Institution text (optional), Opening Balance numeric input. "Add Another" stays on the form clearing fields; "Done" persists all added accounts and navigates to Dashboard.

3. Persist userName to a Settings store (use DataStore Preferences for now — single-source settings; full Room Settings table comes later if needed).

4. On app launch, check if userName is non-empty: if yes, skip onboarding, go straight to Dashboard. If empty, show onboarding.

Validate: required fields enforced, numeric input only on opening balance, INR symbol prefixed.

After done, test full flow on emulator: fresh install → splash → 3 steps → land on Dashboard showing "Hello, [name] 👋" in the app bar.
```

**Done when:**
- [ ] Fresh install routes through onboarding
- [ ] At least one account can be added in Step 3
- [ ] Reopening the app skips onboarding
- [ ] Greeting shows the name entered
- [ ] Committed: `feat(onboarding): splash and 3-step first-run flow`

---

## Session 5 — Add Transaction + Spouse-Prompt Dialog

**Goal:** The single hardest screen. Get this right before anything else.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md sections 7.5 (Add/Edit Transaction) and 7.5.1 (Spouse-Prompt Dialog).

Build the Add Transaction modal bottom sheet:

1. ModalBottomSheet at full height when triggered.

2. Top tab row: Expense | Income | Transfer. Switching tabs shows/hides relevant fields per PRD section 7.5.

3. All fields as specified in PRD 7.5: Amount (large display, INR prefix), Date+Time picker, From/To pickers showing accounts and cards as chips, Category picker (opens secondary sheet with searchable tree), Sub-category, Description, Payment App chip picker, People multi-select chips (with "+" to add new Person inline), Place text field (GPS button disabled with TODO), Tags multi-select, Receipt buttons (Camera / Gallery / None — just save URI, no OCR yet), Notes multiline.

4. Validation: amount > 0, source required, category required (except transfers), description required. Save button disabled until valid.

5. For Transfer tab: if destination is a credit card, auto-set type = CARD_PAYMENT and show a small chip "Credit Card Payment — won't count as expense".

6. Spouse-prompt dialog from section 7.5.1: intercept Save when EXPENSE has a Person with relation = SPOUSE AND Settings.spouseTransactionDefault == ASK. Use AlertDialog (not bottom sheet — easier to keep modal flow). Two radio options + two "Don't ask again" checkboxes. If checkbox ticked, persist to settings before completing save.

7. Wire up to TransactionRepository so saved transactions actually persist and balances update.

8. Trigger the sheet from a FAB on the Dashboard (placeholder Dashboard — we'll do the real one next session). FAB on long-press shows a toast: "AI Quick Entry coming in Phase 3".

Plan first. Then implement. Test scenarios:
- Add a ₹54 expense at "Mother Dairy" via PhonePe on a card → appears, card outstanding goes up
- Transfer ₹100 from savings to a credit card → auto-flagged as CARD_PAYMENT
- Add expense with a Spouse-tagged Person → dialog interrupts → pick Transfer → saves as TRANSFER
```

**Done when:**
- [ ] All three tabs work; fields validate
- [ ] Save persists to Room; balances update
- [ ] Card payment auto-detection works
- [ ] Spouse prompt fires correctly and respects "Don't ask again"
- [ ] FAB long-press shows the Phase 3 toast
- [ ] Committed: `feat(transaction): add/edit sheet with spouse-prompt dialog`

---

## Session 6 — Dashboard + Transactions List

**Goal:** The two main viewing surfaces.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md sections 7.3 (Dashboard) and 7.4 (Transactions Screen).

Build:

1. Dashboard exactly as section 7.3: Net Position hero card, This Month strip (Income/Expense), Accounts horizontal row, Cards horizontal row, Recent Transactions list with chip filter (Today/Week/Month default Today). Pull-to-refresh.

2. Transactions screen exactly as section 7.4: search, filter chip row, sort menu, group by Day with date headers, list items showing icon + title + subtitle + colored amount. Tap → Transaction Detail. Long-press → multi-select (with delete in app bar).

3. Empty states for both: Dashboard ("Add your first account..." if no accounts), Transactions ("No transactions match these filters").

4. Use existing Repository methods. Add new query methods to TransactionDao if needed for the filters (date range, type set, account set, etc.) — keep them composable via Room's @RawQuery or sealed filter spec, your choice.

Make sure Net Position math is correct: sum bank+cash − card outstanding. Indian number formatting on all amounts.
```

**Done when:**
- [ ] Dashboard shows correct totals, accounts, cards, recent transactions
- [ ] Transactions screen filters work; multi-select delete works
- [ ] Pull-to-refresh works on both
- [ ] Empty states look polished
- [ ] Committed: `feat(ui): dashboard and transactions list`

---

## Session 7 — Accounts Screen + Account Detail

**Goal:** Full account management.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md sections 7.7 (Accounts) and 7.8 (Account Detail).

Build:

1. Accounts screen: list sorted by displayOrder, each row showing icon/name/institution+last4/current balance. FAB "+" opens Add Account form. Long-press enables drag-to-reorder mode (use Modifier.draggable or a reorder lib if you must — but try without first).

2. Account Detail screen: header, hero card with current/opening/totals, Vico line chart of balance over last 30 days, transactions list filtered to this account. App bar actions: Edit, Archive (soft delete via isArchived=true).

3. Archived accounts hidden from main list. Add an "Archived" section accessible from the Accounts screen overflow menu showing archived accounts with "Restore" action.

4. Add Account form: full fields per PRD section 8.1 Account entity. Reuse from onboarding if possible.

Test the reorder persists across app restart.
```

**Done when:**
- [ ] Accounts list works, reorder persists
- [ ] Add Account form validates and saves
- [ ] Account Detail shows correct balance and chart
- [ ] Archive + Restore work
- [ ] Committed: `feat(accounts): accounts list, detail, add, archive`

---

## Session 8 — Cards Screen + Card Detail

**Goal:** Same pattern as accounts but for cards, with credit-card-specific UI.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md sections 7.9 (Cards) and 7.10 (Card Detail).

Build:

1. Cards screen: list of all cards sorted by displayOrder. Each row: icon, name, network badge (VISA/MC/Rupay/Amex), last 4, outstanding for credit, utilization bar for credit, due date chip in red if within 10 days. FAB "+" opens Add Card form.

2. Card Detail: header with name+network+type. For credit cards: outstanding (big), credit limit, available limit, utilization %, statement day, due day. Mini chart of outstanding over 30 days. Transactions list filtered to this card. "Pay Bill" action opens Add Transaction pre-filled as Transfer to this card.

3. Add Card form: all Card entity fields. Type chip (Credit/Debit/Prepaid) controls which fields show (creditLimit/statement/due only for Credit; linkedAccountId only for Debit).

4. Validate: credit limit > 0 for credit cards, statement and due days 1-31.

Test: pay-bill action correctly pre-fills and reduces outstanding after save.
```

**Done when:**
- [ ] Cards list shows credit utilization and due-date highlighting
- [ ] Card Detail correct for both credit and debit
- [ ] Pay Bill flow works end-to-end
- [ ] Committed: `feat(cards): cards list, detail, add, pay bill`

---

## Session 9 — Categories Screen + Settings + Transaction Detail

**Goal:** Round out remaining Phase 1 surfaces.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md sections 7.6 (Transaction Detail), 7.12 (Categories), 7.13 (Settings).

Build:

1. Categories screen: tree view (parents expand to children). Add/Rename/Reorder. System categories can be renamed but not deleted. Show "Category is in use by N transactions" toast on delete attempt if in use.

2. Add Category form: name, parent (optional), type (Expense/Income/Transfer/Investment), icon picker (a small grid of Material icons), color picker.

3. Settings screen with sections per PRD 7.13: Profile (userName edit, currency locked to INR), Appearance (theme mode, useDynamicColor), Behavior (spouseTransactionDefault display + "Reset spouse prompt" button), Data (Export all data → JSON to external files dir + share intent, Reset all data with two-step confirm), About (version, "Built with Claude Code").

4. Transaction Detail screen per PRD 7.6: read-only fields, edit/duplicate/delete actions, audit row at bottom showing created/edited timestamps.

5. Wire up "Categories" and "Settings" tiles in the More drawer to open these screens.

Test: rename a system category → persists. Reset spouse prompt from Settings → next spouse-tagged transaction triggers the dialog again.
```

**Done when:**
- [ ] Categories tree works, system categories protected from deletion
- [ ] Settings all sections functional including export and reset
- [ ] Transaction Detail edit/duplicate/delete works
- [ ] Committed: `feat(ui): categories, settings, transaction detail`

---

## Session 10 — Polish + Acceptance Tests

**Goal:** Empty states, animations, Indian formatting, pull-to-refresh, validation polish. Then run all 10 acceptance tests.

**Prompt to Claude Code:**

```
Read CLAUDE.md and docs/PRD.md section 13 Phase 1 (acceptance tests are described inline).

Polish pass — go through every screen and ensure:

1. Every list has an empty state (illustration + message + primary action where relevant)
2. Indian number formatting on every amount (1,00,000 not 100,000)
3. INR symbol prefixed everywhere; no "Rs." anywhere
4. All forms have inline validation messages, not just disabled save
5. All destructive actions have confirmation dialogs (Delete account, delete transaction, reset data)
6. Pull-to-refresh on Dashboard, Transactions, Accounts, Cards
7. Smooth Material 3 transitions between screens
8. Both light and dark themes look good on every screen — make a screenshot pass
9. App icon: ship a temporary placeholder (teal circle with "अ" / "A")
10. Add a launcher icon foreground + background in mipmap

Then walk through the 10 acceptance tests from PRD section 7-related testing (also listed below) and report which pass / which need fixes:

1. Fresh install → splash → 3-step onboarding → land on empty Dashboard with name
2. Add ICICI Savings with ₹50,000 opening → appears with correct balance
3. Add Jupiter Edge Rupay credit, ₹1,00,000 limit → appears with ₹0 outstanding
4. Add ₹54 expense at "Mother Dairy" via PhonePe on Jupiter Edge, category Food › Groceries → appears in Dashboard + Transactions; Jupiter outstanding ₹54; account unaffected
5. Transfer ₹54 ICICI → Jupiter → auto-typed CARD_PAYMENT; Jupiter outstanding ₹0; ICICI ₹49,946; transfer NOT in monthly Expense total
6. Expense with Person "Wife" (SPOUSE relation) → spouse prompt appears → pick "Transfer, don't ask again" → saved as TRANSFER; future spouse expenses auto-Transfer
7. Switch to Dark theme → all screens render correctly
8. Apply a filter matching nothing → empty state shown
9. Try to delete "Groceries" with a transaction using it → toast "Category is in use by 1 transaction"
10. Long-press Account → drag-reorder → persists after restart

For each failing test, fix and re-test.
```

**Done when:**
- [ ] All 10 acceptance tests pass
- [ ] Both themes look polished
- [ ] Indian number formatting and INR symbol everywhere correct
- [ ] All commits clean; final commit: `chore: phase 1 polish and acceptance tests`
- [ ] **Phase 1 complete.** Tag the commit: `git tag v0.1.0-phase1`

---

## After Phase 1

You should now have a working personal finance app on your phone. Use it for **at least 2 weeks** with real transactions before starting Phase 2. Real usage reveals gaps no spec can predict.

While using it, keep notes (in a `docs/feedback.md` file) of:

- Bugs found
- Friction in actual flows
- Things you wish worked differently
- Features you really want next from Phase 2-4

That feedback file will shape Phase 2's task playbook. Ping me when you're ready and we'll plan it together.

---

## Tips for Working With Claude Code

1. **Don't auto-accept everything blindly.** Especially in Sessions 2 and 5 (data layer and Add Transaction) — review diffs carefully. These are the foundations.
2. **If a session goes off-rails**, use "Rewind code to here" in the extension to undo without abandoning the conversation.
3. **If context gets compacted**, start a new session — re-state the goal and let it read CLAUDE.md + relevant PRD section fresh.
4. **Always run the build between major edits**, not just at the end. Catches errors while context is still fresh.
5. **Commit after each session.** Don't carry uncommitted work across sessions.
6. **If something subtle is wrong** (e.g., Spouse dialog shows but doesn't persist the default), copy the exact failing behavior + expected behavior into the next prompt. Be specific.
7. **Use the @file syntax** to direct attention: `@ui/transaction/AddTransactionScreen.kt fix the spouse dialog`.
