---
name: review-screen
description: Use when reviewing, auditing, or improving an Artha app screen/page — for finding bugs, fixing light-mode/theming, accessibility, performance, missing states, or adding features to one specific screen. Lets the user pick which screen, then works it end-to-end.
---

# Review Screen

Drives one Artha screen through a fixed, ordered review-and-improve process so every page
gets the same coverage the Dashboard got. Companion to the project memory
`screen-review-playbook` and `light-mode-migration` / `compose-textfield-local-state` /
`balance-perf-convention` notes.

## Step 1 — Show the available screens, let the user pick

Present this list (grouped) and ask the user to choose ONE by name or number. Do NOT start
reviewing until they pick. If the user already named a screen when invoking, skip straight to
Step 2. Sheets/dialogs are reviewed as part of the screen that hosts them.

**Bottom nav:** 1) Dashboard ✅done  2) Transactions (Ledger)  3) Accounts  4) Cards  5) More sheet
**Details:** 6) Account Detail  7) Card Detail  8) Transaction Detail  9) Investment Detail  10) Insurance Detail  11) Person Detail
**Features:** 12) Investments  13) Insurances  14) People  15) Budgets  16) Goals  17) Subscriptions  18) Recurring  19) Rules  20) Reports  21) Categories  22) Tags  23) Settings  24) About  25) Search
**Gate/onboarding:** 26) Splash  27) Onboarding flow  28) Biometric lock

(If unsure a screen exists, confirm with Glob on `app/src/main/java/com/subramanya/artha/ui/**`.)

## Step 2 — Load context

Read the screen file + its ViewModel + UiState + any sub-composables/dialogs/sheets it hosts.

## Step 3 — Run the 9 phases (create a TodoWrite item per phase)

Work in order. Build green (`assembleDebug`) between meaningful changes. Fix one thing at a time.

1. **Correctness / logic** — honest computed values; affordances do what their label/icon implies;
   correct navigation (+`launchSingleTop` on repeatable nav); exhaustive `when` / no silent `else`;
   resolve real names/icons (never raw id/enum/UUID); edge cases (empty/single/large/negative/null/future).
2. **Deeper sweep** — separately scan UI / performance / logic; dead or unreachable code; mark any
   **WONTFIX** explicitly with its trade-off.
3. **States** — loading (skeleton, no zero/empty flash), empty + first-run (with a CTA), error.
4. **Theming / light mode** — no hardcoded dark tokens; map to the `MaterialTheme.colorScheme` slot the
   dark scheme already aliases (so dark stays identical) or use `LocalArthaIsDark` helpers; verify BOTH modes.
5. **Accessibility** — ≥48dp tap targets (`minimumInteractiveComponentSize()`); merge clickable composites
   (`semantics(mergeDescendants = true)`); meaningful `contentDescription`.
6. **Strings & formatting** — all user-facing copy in `strings.xml`; money via `IndianNumberFormat` (₹, lakh/crore);
   locale-safe number/date formatting.
7. **Performance** — reducers off the main thread (`Dispatchers.Default`); no repeated full-list rescans or
   redundant DB subscriptions (reuse batch helpers); TextFields on LOCAL synchronous state, never an async StateFlow.
8. **Improvements & features** — only after 1–7 are green. Surface data the screen ignores; propose options →
   let the user pick → confirm design forks BEFORE building (these are net-new features; plan first).
9. **Verify & wrap** — `assembleDebug` ✅ + `testDebugUnitTest` ✅; state what is device-visual-pending;
   update memory; commit at a logical checkpoint (only when the user asks).

## Rules throughout

- One change at a time; build between. Flag risky/large changes before making them.
- Surface scope creep instead of silently growing the task.
- Present findings as a ranked issue list (severity) before fixing; let the user steer which to fix.
- Keep dark mode pixel-identical when doing the light-mode pass.

## Output per screen

A ranked issue list → fixes (with build/test evidence) → optional improvements/features → a short
"what changed" summary and what still needs device verification.
