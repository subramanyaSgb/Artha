# Artha — Pixel-Faithful Handoff to Claude Code

You are reproducing a high-fidelity HTML/JSX prototype as a Jetpack Compose + Material 3 Android app. **Match the prototype exactly.** Where the prototype and your instinct disagree, the prototype wins.

---

## 0. How to read this repo (do this first, every session)

The prototype is in this folder. Before you touch any Kotlin:

1. **Open `Artha.html` in a browser.** Click every bottom tab. Open every "More" item. Open the FAB. Open the **Quick add with Gemini** card. Open every Add sheet (Add account, Add card, …). You cannot reproduce what you have not seen rendered.
2. **Read these files in order.** Quote exact values from them — never invent or "round" them:
   - `components/tokens.css` — every color, font-family, radius, spacing token. The CSS variable names map 1:1 to Compose tokens (see §1).
   - `components/patterns.jsx` — SVG patterns (jaali, bandhani, block-print), `fmtINR()` Indian number formatter, `toDeva()` Devanagari numerals, the `<Icon name=…>` set.
   - `components/chrome.jsx` — `PhoneShell`, `BottomTabs`, `FAB`, `TopBar`, `Sheet`, `SectionHeader`, `ChipGroup`. These map to `NavigationBar`, `FloatingActionButton`, `TopAppBar`, `ModalBottomSheet`, etc.
   - `components/screen-dashboard.jsx` — the hero, flow strip, accounts row, AI entry card, recent list, transaction row. **Highest fidelity required here — it's the most-viewed surface.**
   - `components/screen-transactions.jsx`, `screen-accounts.jsx`, `screens-more-1.jsx`, `screens-more-2.jsx` — every other tab and More screen.
   - `components/sheets.jsx`, `sheets-extra.jsx` — every bottom sheet (Add Transaction, AI Quick Entry, Add Account, Add Card, Add Investment, Add Insurance, Add Goal, Add Budget, Add Subscription, Add Recurring, Add Person, Add Rule, Add Category, Add Tag).
3. **Hard rule:** every numeric or color value you write in Kotlin must trace to a `var(--…)` in `tokens.css` or a literal in one of the source files. If you can't trace it, you're guessing — go back and look it up.

---

## 1. Design Tokens → Compose mapping

### 1.1 ColorScheme (build a custom `darkColorScheme()`)

| CSS token | Hex | Material 3 slot | Notes |
|---|---|---|---|
| `--surface-1` | `#0b1612` | `background`, `surface` | base canvas |
| `--surface-2` | `#111d18` | `surfaceContainer` | every card |
| `--surface-3` | `#17261f` | `surfaceContainerHigh` | sheet, dialog |
| `--surface-4` | `#1e3028` | `surfaceContainerHighest` | hover/pressed, segmented control fill |
| `--teal-700` | `#0F766E` | `primary` | brand, primary buttons |
| `--teal-500` | `#14B8A6` | `primaryContainer`/accent | sparkline, accent strokes |
| `--teal-300` | `#5EEAD4` | `onPrimaryContainer` | text on teal tint, active tab |
| `--teal-900` | `#134e48` | tab indicator container | |
| `--text-1` | `#F0EAD6` | `onSurface` | **warm off-white, NOT pure white** |
| `--text-2` | `#A8B3AC` | `onSurfaceVariant` | secondary text |
| `--text-3` | `#6F7A74` | `outline` | tertiary text, dim labels |
| `--line-1` | `rgba(176,200,188,0.08)` | divider | hairlines, card borders — **use these, not elevation shadows** |
| `--line-2` | `rgba(176,200,188,0.14)` | stronger divider | chip outlines |
| `--line-teal` | `rgba(20,184,166,0.32)` | hero border | the special hairline around the Net Position card |
| `--income` | `#56BD8C` | extended `income` | positive amounts |
| `--expense` | `#E58B6F` | extended `expense` | negative amounts — **warm coral, never red** |
| `--ochre` | `#C2841C` | extended `warn` | budget alert |
| `--acc-teal/indigo/emerald/saffron/magenta/violet` | see CSS | account/category accents | 6-slot palette, in this order |

### 1.2 Typography

Four families. Add the Google Fonts in Compose with the `androidx.compose.ui:ui-text-google-fonts` provider OR bundle as XML resources.

```kotlin
val UI       = FontFamily("Plus Jakarta Sans")        // every UI element
val Display  = FontFamily("Instrument Serif")          // every big number
val Mono     = FontFamily("IBM Plex Mono")             // every small inline number
val Deva     = FontFamily("Tiro Devanagari Hindi")     // every अ glyph, Devanagari numerals
```

Type roles (read these off the source — these are the actual sizes used):

| Role | Family | Size · weight · tracking | Where |
|---|---|---|---|
| `displayHero` | Instrument Serif | 60sp · 300 · −0.02em | Net Position number |
| `displayLarge` | Instrument Serif | 44–48sp · 300 · −0.02em | Portfolio / Total cover / Net worth heroes |
| `displayMedium` | Instrument Serif | 28–32sp · 400 · −0.01em | Tile values, breakdown |
| `displaySmall` | Instrument Serif | 20–22sp · 400 · −0.01em | Row amounts |
| `headlineMedium` | Instrument Serif | 26sp · 400 · −0.01em | Screen titles ("This month", "Accounts", "Cards") |
| `titleMedium` | Plus Jakarta | 14–15sp · 600 | Row primary text |
| `bodyMedium` | Plus Jakarta | 13–14sp · 400/500 | Body |
| `bodySmall` | Plus Jakarta | 11.5–12sp · 400 | Secondary text |
| `eyebrow` | Plus Jakarta | 10sp · 600 · 0.18em letterspacing · UPPERCASE | All section labels — **defining motif** |
| `numMono` | IBM Plex Mono | 11–13sp · 400/500 · −0.01em | Inline counts, percentages, dates |
| `deva` | Tiro Devanagari | varies | अ avatar, अर्थ on About |

**Mandatory feature flags** on every numeric Text: `lining-nums` + `tabular-nums`. In Compose:
```kotlin
Text(
  fmtIndianINR(482610),
  style = MaterialTheme.typography.displayHero,
  fontFeatureSettings = "tnum, lnum"
)
```
Without `tnum` your columns of money won't align — this is non-negotiable.

### 1.3 Shape / radius

| CSS | dp | Used by |
|---|---|---|
| `--r-xs` 6 | `RoundedCornerShape(6)` | tiny pills, eyebrows |
| `--r-sm` 10 | `RoundedCornerShape(10)` | inputs |
| `--r-md` 14 | `RoundedCornerShape(14)` | most cards |
| `--r-lg` 20 | `RoundedCornerShape(20)` | hero, sheet body |
| `--r-xl` 28 | `RoundedCornerShape(28)` | sheet top corners (24/24/0/0) |
| `--r-pill` ∞ | `CircleShape` | chips, buttons |

### 1.4 Spacing

4dp base scale. `s-1=4 s-2=8 s-3=12 s-4=16 s-5=20 s-6=24 s-7=32`. **Side padding for the phone is always 16dp** (`s-4`). Sheet inner padding is `20dp`. Card inner padding is `14–16dp`. Don't drift.

### 1.5 Indian number formatting

Indian grouping is **2,2,3** — `1,23,456` not `123,456`. Use this on every amount, no exceptions:

```kotlin
fun fmtINR(amount: Long, compact: Boolean = false): String {
  val n = abs(amount)
  val s = when {
    compact && n >= 10_000_000 -> "%.2f Cr".format(n / 1e7).trimEnd('0').trimEnd('.')
    compact && n >= 100_000     -> "%.2f L".format(n / 1e5).trimEnd('0').trimEnd('.')
    else -> NumberFormat.getInstance(Locale("en", "IN")).format(n)
  }
  val sign = if (amount < 0) "–" else ""   // en-dash, NOT hyphen-minus
  return "$sign₹$s"
}
```

`–₹420` uses an en-dash. `+₹92,000` uses `+`. Never write `Rs.` or `INR`.

---

## 2. App shell & navigation

- 5-tab `NavigationBar` at the bottom: **Home · Ledger · Accounts · Cards · More**. Active tab gets a `Surface(color = teal-900, shape = RoundedCornerShape(50)) { Icon(tint = teal-300) }` pill behind the icon. Label is `teal-300` when active, `text-2` when inactive.
- Top of every screen is the **DashHeader**: 40dp rounded square with अ in teal-300 on teal-900, then the small eyebrow date ("FRI, 22 MAY"), then "Namaste, Subramanya". A 40dp square search icon-button on the right. **No emoji wave.** The current build uses a wave emoji — replace it with this layout.
- "More" pushes sub-screens with a `TopAppBar`-style back arrow + screen title. Title typography is Instrument Serif 26sp (`headlineMedium` in §1.2).
- `FloatingActionButton` on Home only: extended, 56dp tall, label "Add", icon plus, color teal-700, sits 110dp from the bottom (above the nav bar).

---

## 3. Per-screen acceptance checklist

For every screen below, the implementation passes only when **all** items are true.

### 3.1 Dashboard — `screen-dashboard.jsx`

The hero is the whole point. Get this perfect.

- [ ] **Hero card.** Background `surface-2`, **1dp border `line-teal`**, radius 20dp, 22dp horizontal padding, 20dp top / 18dp bottom. **Not a solid blue fill.** The current build uses a solid `#2A6FDB` block — that is the thing you are replacing.
- [ ] Inside the hero, top-right: 32dp square with 8dp radius, 1dp `line-teal` border, transparent background, containing the अ glyph in Tiro Devanagari 18sp, color `teal-300`.
- [ ] Hero number: `fmtINR(netPosition)` rendered in Instrument Serif **60sp, weight 300, tracking −0.02em**, color `text-1`. Indian grouping mandatory.
- [ ] Below number: a row of mono 13sp showing `↑ ₹12,400  •  +2.6%  this month`. Arrow + amount + pct are colored `income` if positive, `expense` if negative. The label "this month" is Plus Jakarta `text-3`.
- [ ] **Sparkline** — 13 data points minimum. SVG `polyline` 1.2px teal-500, with a gradient fill below (teal-500 0.3 → 0). End cap is a 1.8px filled teal-500 circle. 36dp tall. **Not optional — every variant of the hero shows the sparkline.**
- [ ] Behind everything in the hero: a low-opacity (0.06) jaali pattern in teal-500. Implement as a `Canvas` modifier that draws the `p-jaali` repeat (see `patterns.jsx`). It is a 32×32 tile of overlapping circles. Subtle — if it looks like a texture, you've gone too far.
- [ ] Bottom of hero: a 1px `line-1` divider then a 3-column row: **Liquid · Invested · Card o/s**. Each column: eyebrow label (10sp uppercase 0.12em letterspacing) then Plus Jakarta 16sp 500 amount in compact form (₹2.11 L). Liquid is `teal-300`, Invested is `ochre-soft`, Card o/s is `expense`.
- [ ] **FlowStrip** below hero: 2-up grid, 10dp gap. Each tile is a `surface-2` card with 1dp `line-1` border, 14dp pad. Inside: a 22dp circle filled `income-soft`/`expense-soft` with arrow icon in `income`/`expense`, then the eyebrow ("INCOME"/"SPENDING"), then a 28sp Instrument Serif number, then a mono 11sp footer ("May" / "53% of cap"). Spending tile has a 3px bottom progress bar showing budget util.
- [ ] **AI Quick Entry card.** 16dp radius, gradient `linear-gradient(135deg, surface-2 0%, teal-950 100%)`, 1dp `line-teal` border. 40dp teal-700 rounded-square icon container with sparkles, then "Quick add with Gemini" (13sp 600), then `"Auto to MG Road ₹180" — type, dictate, snap a receipt` (11.5sp `text-2`). Right side: arrow-right in teal-300. **Tappable surface opens the AI sheet.**
- [ ] **Accounts row.** Horizontal scroll. Each card 158×116dp, 16dp radius, gradient (account-color → 65%-mixed-with-black), bandhani SVG overlay at 0.18 opacity. Inside: icon + uppercase type label (top), account name + INR amount + `•7421` last-4 (bottom). Last card is a 96×116 dashed-outline "Add account" tile.
- [ ] **Recent activity list.** Day-grouped (TODAY, YESTERDAY, then short date). Each day's transactions are inside one rounded-card-flush container with 1px hairline dividers between rows starting at 56dp from the left (after the icon). Row layout: 36dp icon container (income txns are filled income-soft, others surface-4), 12dp gap, then 2-line text (description 14sp 500, mono 11sp `account • time`), then right-aligned 15sp amount (income green, expense text-1) with 10sp category label below.

### 3.2 Ledger (Transactions) — `screen-transactions.jsx`

- [ ] DashHeader at the top.
- [ ] Page title: eyebrow "THE LEDGER" + Instrument Serif 26sp "This month". 40dp `surface-2` filter button on the right.
- [ ] In/Out/Net strip: single `surface-2` card with 3-column grid (1fr/1fr/1.1fr), each column has a 1px `line-1` left border (except first). Each shows colored dot + eyebrow + Instrument Serif 20sp value.
- [ ] Search input: 44dp tall, `surface-2`, 1dp `line-1`, 10dp radius, leading search icon in `text-3`, placeholder "Find merchant, note, amount…".
- [ ] Type chip group: `All · Expense · Income · Transfer · Investment`. Active chip uses `chip.active` (teal-900 fill + teal-500 border + teal-300 text).
- [ ] Filter chip row below: 4 segmented dropdowns — `📅 This month`, `🏦 All accounts`, `📁 All categories`, `🏷 Tags`. Each is a chip with leading icon + label + trailing chevronDown.
- [ ] Day-grouped transaction list, same row component as dashboard. Day header shows day name + the net total for that day on the right (mono, signed).

### 3.3 Accounts — `screen-accounts.jsx`

- [ ] DashHeader.
- [ ] Eyebrow "WHERE YOUR MONEY SITS" + Instrument Serif 26sp "Accounts".
- [ ] Total liquid hero card (18dp radius, surface-2, faint bandhani at 0.04 opacity): eyebrow "TOTAL LIQUID", Instrument Serif 40sp value, mono 12sp "across N accounts".
- [ ] Account row: 44dp gradient swatch with icon + bandhani overlay, 15sp 600 name, 11.5sp mono type and last-4, right-aligned 20sp Instrument Serif balance.
- [ ] Dashed "New account" button at end.

### 3.4 Cards — `screen-accounts.jsx`

- [ ] DashHeader + eyebrow "PLASTIC ON FILE" + Instrument Serif "Cards".
- [ ] Utilization summary card with eyebrow + "TOTAL OUTSTANDING" + 32sp Instrument Serif + utilization bar (teal if <30%, ochre if higher).
- [ ] **Credit card tile** — 190dp tall, 20dp radius, gradient (card-color → 55%-mixed-with-black), jaali overlay at 0.15, **chhatri silhouette in top-right at 0.10 opacity (120dp size, offset −10dp/−10dp)**. Inside, two layers:
  - Top: NETWORK eyebrow (white 0.8 opacity), card name 17sp 600. Top-right: `DUE IN {n}d` pill (rgba 0.14 white background, 10sp 700 letterspacing 0.05em).
  - Bottom: "OUTSTANDING" eyebrow, 28sp Instrument Serif amount, last-4 mono 11sp. Below that: 4px white-on-white-018 utilization bar, then a 10sp mono footer with `{pct}% of {limit}` and `Limit available {x}`.

### 3.5 More — `screens-more-1.jsx`

- [ ] DashHeader + eyebrow "EVERYTHING ELSE" + Instrument Serif 26sp "More".
- [ ] 5 grouped sections (Money / Recurring / People & rules / Look-ups / App). Each section: eyebrow header + a card-flush container with rows.
- [ ] Each row: 36dp `surface-4` rounded-square with a `teal-300` icon, then 14.5sp 500 label, 11.5sp `text-3` sub, trailing chevronRight in `text-3`.

### 3.6 Investments — `screens-more-1.jsx`

- [ ] Hero with eyebrow "PORTFOLIO VALUE", 48sp Instrument Serif, gain badge (`income-soft` background pill containing arrowUp + +₹X · +Y.Z%) and invested mono footer. Faint jaali at 0.05 ochre.
- [ ] Allocation: 8dp tall stacked bar across all holdings (each segment colored by holding), with a legend chip row beneath.
- [ ] Type filter chips: All · Mutual Funds · Deposits · Gold · Tax-saving.
- [ ] Holding rows: 4dp left color bar matching holding accent + name (14sp 600) + mono 11sp type/AMC + right-aligned Instrument Serif current value + mono pct (income or expense).
- [ ] Dashed "Add investment".

### 3.7 Insurance, Budgets, Goals, Subscriptions, Recurring, People, Rules, Reports, Categories, Tags, Settings, About

Each has its own file (`screens-more-1.jsx` or `screens-more-2.jsx`). Re-read them and lift exact values. Highlights:

- **Goals card** has a faint chhatri silhouette at 0.06 opacity in the bottom-right corner — keep it.
- **Budget rows** show progress bar with stripe-pattern overflow when `spent > cap`. Don't simplify this — when overspent, the bar is full + a diagonally striped extension shows by how much.
- **Recurring** has an ochre info-banner at the top about Phase 5 WorkManager. Carry the copy verbatim from the source.
- **Rules** rows render `WHEN` / `THEN` clause badges (10sp uppercase, mono, surface-3 background for WHEN, teal-900 background for THEN) — implement as a custom row, not a default list item.
- **Reports** has 4 sub-sections — implement each as its own composable: `CategoryBars`, `AppBars` (stacked + legend), `TopMerchants`, `TaxSections`.
- **About** has the `अ` brand mark at 88dp with 22dp radius, "अर्थ · artha · is one of the four puruṣārthas…" copy. The Devanagari word is in `Tiro Devanagari` 18sp `teal-300`.

### 3.8 Add Transaction sheet — `sheets.jsx` (`AddTransactionSheet`)

- [ ] Segmented control at top (Expense/Income/Transfer) — pill row, 4dp inset, active segment is `surface-4` filled.
- [ ] **Big amount entry** — centered, transparent input, `₹` prefix in 32sp Instrument Serif text-3, then the amount in 64sp Instrument Serif weight 300 tracking −0.02em. Color of the amount changes by tab: text-1 / income / indigo. **No outlined TextField for the amount.**
- [ ] Date/time chips below the amount.
- [ ] Field rows are: eyebrow label, optional "· optional" sub-label in `text-3`, then the control.
- [ ] All chip groups use the `chip` / `chip.active` pattern from §1.1.
- [ ] Category picker rows are 38dp chips with a 22dp rounded-square colored icon on the left.
- [ ] Save button uses tab-tinted color (teal-700 for expense, income for income, indigo-deep for transfer). Label includes the amount: `Save expense · ₹420`.

### 3.9 AI Quick Entry — `sheets.jsx` (`AIQuickEntry`)

- [ ] Header: 48dp gradient teal icon container with sparkles + "Quick add" 17sp 700 + "Gemini parses what happened. You confirm." 12sp text-3.
- [ ] Multiline textarea inside a `surface-2` rounded 18dp card. Border switches from `line-1` to `line-teal` when input is non-empty. Below the textarea: mic + image circle buttons on the left, send button on the right (teal-700 when there's text).
- [ ] Below the input, **when empty**, an "Or try one of these" eyebrow and a chip row of example sentences with leading sparkles icon.
- [ ] When the user types > 6 chars, simulate a parse delay (~700ms). Show a `surface-2` "Gemini is reading your sentence…" card with a pulsing dot (12dp circle, teal-500, box-shadow glow, animate scale 0.8↔1.2 over 1.2s).
- [ ] When parsed, replace it with a parsed card: header "PARSED · REVIEW & CONFIRM" eyebrow in teal-300 + edit icon. Body shows description (15sp 600), right-aligned 26sp Instrument Serif amount, then a chip row of parsed tokens (category, account icon-chip, payment app, @people, date). Footer in `surface-3` with Cancel/Save buttons.

### 3.10 Add sheets (Account/Card/Investment/Insurance/Goal/Budget/Subscription/Recurring/Person/Rule/Category/Tag)

- [ ] Use a shared `FieldRow` composable: eyebrow label + optional "optional" suffix + body slot.
- [ ] **Account chips are pills with `chip.active` for selection.**
- [ ] **Account/Card/Investment Add** show a live **Preview** card at the bottom (uses the same `AccountChip`/`CreditCardTile` as the list screen) — implement this preview.
- [ ] **Color picker** uses 30dp circles, selected has a 2px `text-1` outline with 2px offset.
- [ ] **Icon picker** uses 40dp rounded-square buttons — selected has teal-900 fill + teal-500 border + teal-300 icon.
- [ ] **Add Rule** has a custom condition/action builder — preserve the WHEN/THEN coloured-badge layout shown in the source.

---

## 4. Patterns to draw (jaali / bandhani / block-print)

In Compose, draw these in a `Canvas { … }` modifier on the card root. Each is a **tile** repeated with `drawIntoCanvas { … }` + matrix translate. Keep opacity low — the patterns are *texture*, not *decoration*.

```kotlin
fun DrawScope.drawJaali(tint: Color) {
  val tile = 32.dp.toPx()
  for (x in 0..(size.width / tile).toInt() + 1) {
    for (y in 0..(size.height / tile).toInt() + 1) {
      val cx = x * tile; val cy = y * tile
      drawCircle(tint, radius = 14.dp.toPx(),
                 center = Offset(cx, cy),
                 style = Stroke(width = 0.5.dp.toPx()),
                 alpha = 0.5f)
    }
  }
}
```

Three tiles to implement:
- **Jaali** — 32×32, 5 overlapping circles (centre + 4 corners), 14dp radius, 0.5dp stroke.
- **Bandhani** — 20×20, centre dot 1.2dp + 4 cardinal dots 0.5dp.
- **Block-print** — 12×12, centre dot 0.7dp + 4 corner dots 0.4dp.

Apply at these opacities:
- Hero card: jaali at **0.06** in teal.
- Account chip: bandhani at **0.18** in white.
- Credit card tile: jaali at **0.15** in white **+** a chhatri SVG at **0.10**.
- Investment hero: jaali at **0.05** in ochre.
- Total liquid card: bandhani at **0.04** in teal.
- Net worth report card: block-print at **0.05** in teal.

Don't skip these. The textures are what make the surfaces feel Indian without being kitsch.

---

## 5. Components that must exist as Composables

Build these once and reuse — the prototype's consistency comes from this:

- `ArthaCard` — `surface-2` + 1dp `line-1` border + 14dp radius (default).
- `Eyebrow(text)` — 10sp 600, 0.18em letterspacing, uppercase, `text-3`.
- `Hairline()` — 1dp `line-1` divider.
- `ChipFilter(label, active, leadingIcon, trailingIcon)` — replicates the chip pill spec from §1.1.
- `SectionHeader(title, action)` — 14dp teal-500 horizontal line + eyebrow + optional right-aligned action.
- `Amount(value, style)` — wraps `Text` with `fmtINR` + tabular feature settings.
- `BrandMark(size, bg, color)` — the अ avatar.
- `PatternedCard(pattern, opacity, color, content)` — card with one of the 3 tiled patterns behind content.
- `Sparkline(data, color, height)` — polyline + gradient fill + end dot.
- `Sheet(title, tall, content)` — bottom modal with the 36×4 handle, 24/24/0/0 radius, `surface-3` background.
- `AccountChip(account)`, `CreditCardTile(card)`, `TransactionRow(t)`, `AccountRow(a)`, `BudgetRow(b)`, `GoalCard(g)`, `InvestRow(i)`, `SubscriptionRow(s)` — direct ports of the JSX components.

---

## 6. Things that are EASY to miss — re-check these explicitly

These are the things every junior implementation gets wrong. Walk this list before declaring done.

1. **The text colour is `#F0EAD6`, NOT `#FFFFFF`.** Pure white burns out against the warm-dark background. If your text looks crisp-cold, you used white. Fix it.
2. **The expense colour is warm coral `#E58B6F`, NOT a true red.** Same for `--income` — it's sage `#56BD8C`, not Material green.
3. **Cards have 1dp hairline borders. They do NOT have elevation shadows.** Compose's default `Card` uses `Modifier.shadow` — drop it and use `Modifier.border(1.dp, line1, RoundedCornerShape(14.dp))`.
4. **The hero card border is `line-teal` (#14B8A6 at 32% alpha), not the default `outlineVariant`.** Specifically this border.
5. **The negative sign is an en-dash `–`, not a hyphen `-`.** It looks worse with a hyphen because the rest of the typography is editorial.
6. **Indian grouping.** `1,23,456` not `1,23,456` (no, look harder — your default Intl probably gives `123,456`). Always go through `fmtINR()`.
7. **The अ glyph uses Tiro Devanagari Hindi**, not whatever Devanagari your phone falls back to. Bundle the font.
8. **Tabular numerals.** `tnum` + `lnum`. If you scroll the recent transactions list and the `₹` symbols don't form a straight vertical line, you forgot.
9. **Bottom tab labels.** Label is `teal-300` when active, `text-2` otherwise. The active icon sits inside a 56×28dp rounded pill (`teal-900` fill), label sits below. The default M3 `NavigationBar` will fight you — override the `indicatorColor`.
10. **Patterns.** Every hero surface has a pattern. If your surfaces look flat, you skipped them. See §4.
11. **The `Namaste, Subramanya` greeting**, not "Hello … 👋". The current Android build uses a wave emoji — that's gone. Avatar (अ on teal-900) + small eyebrow date + "Namaste, {name}".
12. **No emoji.** Anywhere. Not in headers, not in empty states, not in chip labels. Icons only.
13. **Empty states** use a faint single-colour icon (24–32dp, `text-3`) above the message. Avoid the giant filled illustrations.
14. **The AI Quick Entry parsing dot pulses.** Don't make it a spinner — implement the 1.2s scale animation with `infiniteRepeatable`.
15. **Sheet handle width is 36dp.** Not the default M3 24dp drag pill. Don't accept the default.

---

## 7. Acceptance — when are you done?

You're done with a screen when you can put your build side-by-side with the prototype open on `Artha.html` and:

- Colours read identically (use the eyedropper).
- Type sizes are within ±1sp (use a screen ruler).
- Spacing is exact (16dp side gutters, 14dp card pad, etc).
- Every component from §5 is in place.
- Numbers are Indian-grouped and tabular.
- The 15 items in §6 all pass.

Do not declare a screen done until you've personally rendered the prototype, screenshotted both, and compared them. **Eyeball comparison, not adjective comparison.** "Looks similar" is not done.

---

## 8. Order of work (recommended)

1. Tokens + typography (§1).
2. `ArthaCard`, `Eyebrow`, `Hairline`, `ChipFilter`, `SectionHeader`, `Amount`, `BrandMark`, `Sparkline`, `PatternedCard`, `Sheet` (§5).
3. App shell: `NavigationBar` + `DashHeader` + `Scaffold` (§2).
4. **Dashboard** in full (§3.1) — including patterns. Don't move on until this is pixel-faithful.
5. Ledger, Accounts, Cards (§3.2–3.4).
6. More + secondary screens (§3.5–3.7).
7. Add Transaction + AI Quick Entry sheets (§3.8–3.9).
8. All other Add sheets (§3.10).
9. Walk §6 with the apk in your hand.

If you skip step 4 to "make progress on other screens," you will end up rebuilding everything when you finally fix the hero. The hero defines every other surface.
