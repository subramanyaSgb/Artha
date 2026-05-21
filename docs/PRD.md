# Artha — Personal Finance Manager

*Product Requirements Document*

**Owner:** Subramanya GB
**Version:** 0.3 (Claude Code in VS Code as build tool)
**Target build tool:** Claude Code (VS Code extension) → Android (Kotlin + Jetpack Compose)
**Region:** India (INR, FY April–March, UPI-first)

---

## 1. Product Vision

A single, personal, India-first finance app that I actually trust as the source of truth for every rupee — across bank accounts, cash, credit cards, investments, insurance, lending, subscriptions, and goals — with a rules engine smart enough to know that paying my credit card bill is **not** an expense and that my LIC premium **is** an investment, not a bill.

The app must work the way *my* money actually moves, not the way a generic Western expense tracker assumes.

---

## 2. Target User

Single user (me). No multi-user, no household sharing in v1. Account data stays on-device. (Cloud sync may come later — see Phase 5.)

---

## 3. Problem Statement

Existing apps (Wallet, Paisa, spreadsheets, Walnut, Money Manager, etc.) each fail in at least one of these ways:

1. **No real rules engine.** Credit card repayments get double-counted. LIC premiums show as expenses. Money sent to spouse for her savings is logged as gone-forever.
2. **No India-native categories.** Temple donations, sevas, prasadam, pilgrimage, festival gifts have no home.
3. **Weak separation between asset classes.** Bank accounts, cards, investments, and insurance are dumped into one flat list.
4. **No receipt OCR.** Manual entry every time is friction. Gemini can fix this.
5. **Subscriptions, recurring bills, friend-lending, and goals require 3 different apps.**

This app consolidates all of it.

---

## 4. Core Design Principles

1. **Rules first, UI second.** The transaction model and rules engine are the heart of the app. Get them right or nothing else matters.
2. **Local-first storage.** Room DB on device. No cloud account in MVP. Reduces complexity, increases privacy, and keeps the dependency surface small.
3. **India-native.** INR symbol everywhere, FY April–March for reports, tax-section tags (80C/80D/etc.), UPI app metadata on every transaction.
4. **Source of truth, not just a tracker.** Account/card balances are computed from transactions, not typed in. You enter an opening balance once; every transaction adjusts it.
5. **Receipt-attached.** Any transaction can have a photo attached (now or later).
6. **Customizable.** Nav bar order, categories, rules, defaults — all user-editable.

---

## 5. Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Local DB:** Room
- **Architecture:** MVVM with ViewModel + StateFlow
- **Image capture:** CameraX
- **AI (Phase 3+):** Gemini API for receipt OCR + smart categorization
- **Charts:** Vico (Compose-native chart library)
- **Navigation:** Compose Navigation
- **Min SDK:** 26 (Android 8.0)
- **Theme:** Light + Dark, Material You dynamic color

---

## 6. Information Architecture

### Bottom Navigation (default order, reorderable in Settings)

```
Dashboard | Transactions | Accounts | Cards | Investments | Insurance | More
```

`More` opens a drawer/sheet for: Budgets, Goals, Subscriptions, Recurring, People (lending), Reports, Categories, Rules, Settings.

> `[DECISION]` 7 bottom-nav items is too many on small screens. Default to **5 visible + More**. User can pick any 5 to pin in Settings.

### Top App Bar (global)

- Left: greeting `Hello, <Name> 👋`
- Right: today's date (`Thu, 21 May`)
- Tapping the greeting opens Settings → Profile.

---

## 7. Screen Specifications

### 7.1 Splash Screen

- App logo (centered)
- App name **Artha** (अर्थ — "wealth & meaning"; one of the four *purusharthas*)
- Tagline: **"Your money. Your rules."**
- Auto-dismiss after DB init (~500ms minimum)

> **Naming rationale:** Artha captures both *wealth* and *purpose*, matching the dual nature of personal finance — managing rupees and being intentional about where they go.

### 7.2 First-Run Onboarding (one-time)

3 quick screens:

1. **Welcome** + privacy note ("Your data stays on this phone")
2. **Set your name + currency** (default INR)
3. **Add your accounts** — quick form to add 1+ bank accounts, cards, and cash with opening balances. Can skip and add later.

### 7.3 Dashboard (default landing)

Top to bottom:

1. **Greeting bar** (top app bar)
2. **Hero card — Total Net Position**
   - Large amount: sum of all bank balances + cash + investments − credit card outstanding
   - Subtitle: "across X accounts, Y cards, Z investments"
   - Tap → Reports/Net Worth view
3. **This Month strip** (2 small cards side-by-side)
   - Income: `+₹X` (green)
   - Expense: `−₹Y` (red)
   - Tap each → filtered Transactions view
4. **Accounts row** (horizontal scroll, reorderable via long-press)
   - Each card shows: account name, last 4 / institution, balance, icon
   - Tap → Account Detail
   - "+" tile at end → Add Account
5. **Cards row** (same pattern)
   - Each shows: card name, outstanding amount, due date if within 7 days (highlighted)
6. **Recent Transactions** (today only, default)
   - Header with "Today" + chip filters (Today / This Week / This Month)
   - List of 5–10 most recent
   - "View all" link → Transactions screen with the same filter pre-applied
7. **Quick Actions FAB**
   - **Tap `+`** → opens the manual Add Transaction sheet (Section 7.5)
   - **Long-press `+`** → opens **AI Quick Entry** (Section 11.1) — text / voice / photo, all in one sheet
   - Hidden long-press chosen over a second FAB to keep the dashboard uncluttered

### 7.4 Transactions Screen

- **Search bar** at top
- **Filter chips row:** Date range, Type (Expense/Income/Transfer/Investment/...), Account, Card, Category, Person, Tag, Amount range, Has receipt, Tax section
- **Sort:** Date desc (default), Date asc, Amount desc, Amount asc
- **Group by:** Day (default), Week, Month, Category, Account, None
- **List item shows:** Icon (category), title (description), subtitle (category › subcategory · account/card), amount (color-coded by type), date
- Tap → Transaction Detail
- Long-press → multi-select for bulk delete/edit
- **Empty state:** "No transactions match these filters"
- **Export button** (top right): Export filtered set to CSV

### 7.5 Add / Edit Transaction (modal bottom sheet, full-height)

**Top tabs:** `Expense` | `Income` | `Transfer` | `Investment` (Investment tab does Buy/Sell)

Fields shown for **Expense** (the most complex):

- **Amount** (large numeric input at top, with INR symbol)
- **Date & Time** (defaults to now)
- **From** — Account or Card (chip picker, recents first)
- **Category** → opens Category picker (search + tree)
- **Sub-category** (auto-shown if parent has children)
- **Description / Merchant** (text)
- **Payment app** (chip: GPay, PhonePe, Paytm, CRED, BHIM, Bank App, Card Swipe, Cash, Other) — auto-suggest from past transactions for same merchant
- **People** (chips, multi-select — e.g., "with Wife", "for Parents")
- **Place / Location** (free text + optional GPS pin via current location button)
- **Tags** (multi-select chips)
- **Tax section** (None / 80C / 80D / 80CCD / Other) — auto-set by rules where possible
- **Receipt** — Camera / Gallery / Skip
- **Notes** (multiline)
- **Save** button (primary) and **Save & Add another** (secondary)

For **Income**: similar, but `To` instead of `From`, source category (Salary, Interest, Cashback, Refund, etc.).

For **Transfer**: `From` (Account/Card/Cash) + `To` (Account/Card/Cash) + Amount + Note. No category. Special sub-type **Credit Card Payment** auto-detected if `To` is a credit card.

For **Investment**:
- Buy: From Account → To Investment, Amount, Units (optional), NAV/Price (optional)
- Sell: From Investment → To Account, Amount, Gains (auto-computed if units & buy price known)

### 7.5.1 Spouse-Prompt Dialog (interrupts save)

Fires whenever the user tries to save an EXPENSE with a person tagged as `relation = SPOUSE`, **unless** the user has set a permanent default in Settings → Behavior.

```
┌──────────────────────────────────────────┐
│  Sending ₹5,000 to Wife                   │
│  How should this be tracked?              │
├──────────────────────────────────────────┤
│                                          │
│  ○ Transfer (to her savings)             │
│    Won't count as a monthly expense       │
│                                          │
│  ○ Expense                               │
│    Counts toward monthly spending         │
│                                          │
│  ☐ Don't ask again — always use Transfer │
│  ☐ Don't ask again — always use Expense  │
│                                          │
│      [ Cancel ]      [ Save ]            │
└──────────────────────────────────────────┘
```

- Default radio selection: Transfer (since this matches your stated pattern — she saves it)
- If a "Don't ask again" box is ticked, persist `spouseTransactionDefault = TRANSFER | EXPENSE` in Settings; future saves skip the dialog
- Settings → Behavior has a "Reset spouse prompt" button to clear the default and resume asking
- Same dialog logic applies in reverse on **INCOME** when wife sends money back

### 7.6 Transaction Detail

- All fields visible, edit button top-right
- Receipt thumbnail (tap to view full / re-OCR)
- Audit info at bottom: created date, edited date, source (Manual / SMS / OCR)
- **Duplicate** action, **Delete** action

### 7.7 Accounts Screen

- List of all accounts (Bank Savings, Bank Current, Cash, Wallet)
- Each row: name, institution, last 4, current balance, icon
- Tap → Account Detail
- "+" FAB → Add Account
- Reorder via drag handle

### 7.8 Account Detail

- Header: account name, institution, last 4
- Hero: current balance, opening balance, total in / total out (this month + lifetime toggle)
- Mini chart: balance over time (last 30 days)
- Transactions list (filtered to this account)
- Actions: Edit, Archive, Reconcile (set actual balance — creates an adjustment transaction)

### 7.9 Cards Screen

- List of all cards (Credit / Debit / Prepaid)
- Each row: card name, network (Visa/MC/Rupay/Amex), last 4, outstanding (for credit), credit limit utilization bar, due date if within 10 days
- Tap → Card Detail
- "+" FAB → Add Card

### 7.10 Card Detail

- Header: card name, last 4, network, type
- For Credit: outstanding, credit limit, available limit, utilization %, statement date, due date, min due
- Mini chart: outstanding over time
- Transactions list (filtered to this card)
- **Pay Bill** action → opens Add Transaction pre-filled as Transfer to this card
- Actions: Edit, Archive, Set statement & due dates

### 7.11 Investments Screen

- Toggle: **By Type** (FD, RD, SIP, Mutual Funds, Stocks, Gold, Bonds, PPF, EPF, NPS, ULIP) vs **All List** view
- Hero: total invested, current value, absolute gain, % gain
- Each investment row: name, type, invested, current value, %change, icon
- Tap → Investment Detail (transactions, units, NAV history if entered)
- "+" FAB → Add Investment
- Filter: by tax-section (80C bucket view)

### 7.12 Insurance Screen

- List grouped by type: Health, Vehicle, Life (Term), Life (Endowment), Travel, Home
- Each row: policy name, provider, premium amount, **next due date** (highlighted if within 30 days), sum assured
- Tap → Insurance Detail (premium history, policy doc attachment, nominee, agent contact)
- "+" FAB → Add Insurance
- Notification: 7 days before premium due

> **Rule:** Endowment / ULIP / money-back policies also create a linked Investment entry (LIC = investment). Term-life is pure expense.

### 7.13 Budgets

- List of budgets (per category or overall monthly)
- Each: progress bar (spent / budgeted), days left in period
- Tap → drill into transactions
- "+" Add Budget: pick category (or "Overall"), amount, period (Monthly/Weekly/Yearly), alert threshold (default 80%)

### 7.14 Goals & Savings

- List of goals (e.g., "Emergency Fund ₹3L", "iPhone 17 ₹1.2L", "Vacation ₹50k")
- Each: target amount, current amount, progress bar, target date, monthly contribution needed
- "Linked accounts" — money in these accounts/investments counts toward this goal
- "+" Add Goal

### 7.15 Subscriptions

- List of recurring online subscriptions (Netflix, Spotify, iCloud, Claude Max, etc.)
- Each: logo, name, amount, frequency, next due date, payment method
- Total per month + per year (hero)
- Status: Active / Paused / Cancelled
- "+" Add Subscription — when added, optionally auto-create a recurring transaction rule

### 7.16 Recurring Transactions

- List of rules (Rent on 1st, Mobile Bill on 5th, SIP on 10th, Salary on last working day)
- Each: amount, frequency, next run date, last run date
- Tap → edit rule
- Auto-creates a transaction on schedule (with a "Confirm" notification, optional auto-confirm)

### 7.17 People (Lending / Split-wise lite)

- List of people
- Each: name, net balance ("Owes you ₹X" / "You owe ₹X" / "Settled")
- Tap → Person Detail with full transaction history with them, settle-up button
- "+" Add Person
- "+" Add Lending — quick form: amount, person, date, note, "expected return date" optional

> `[DECISION]` Full Splitwise-style group expenses (3+ people in a trip) is **deferred to Phase 4**. v1 supports only 1-to-1 lending.

### 7.18 Reports / Analytics

- Time period selector: This Month / Last Month / FY (Apr–Mar) / Custom
- Cards:
  - **Net Worth Trend** (line chart)
  - **Income vs Expense** (bar chart)
  - **Spending by Category** (donut)
  - **Spending by Payment Method** (donut: GPay vs PhonePe vs CRED vs Cash...)
  - **Top Merchants** (list)
  - **Tax-Section Summary** (80C used / limit, 80D used / limit) — important for FY-end planning
  - **Investment Performance** (per type)

### 7.19 Categories Management

- Tree view: Parent → Sub-categories
- Each: icon, color, type (Expense/Income/Transfer/Investment)
- Pre-seeded with the full list in section 9 below
- Add/Edit/Delete/Reorder
- Cannot delete a category in use (must reassign first)

### 7.20 Rules Engine

- List of active rules
- Each rule: name, conditions, actions, priority, on/off toggle
- "+" Add Rule — visual builder:
  - **IF** (any/all of):
    - Description contains "..."
    - Amount = / > / < ...
    - From account / card = ...
    - Payment app = ...
    - Time of day between ...
  - **THEN** (any of):
    - Set category to ...
    - Set sub-category to ...
    - Add tag ...
    - Add person ...
    - Mark as "Not an expense" (e.g., for card payments)
    - Mark as Investment
    - Set tax section to ...
- Pre-seeded rules (see section 10)

### 7.21 Settings

Sections:

- **Profile** — name, photo, currency (locked to INR for v1)
- **Appearance** — theme (System / Light / Dark), Material You toggle
- **Navigation** — pick 5 bottom-nav items + their order
- **Security** — biometric / PIN lock on app open, auto-lock timeout
- **Data**
  - Export all (JSON / CSV / Excel)
  - Backup to local file
  - Restore from backup
  - **Reset all data** (with confirmation)
- **Notifications** — bill reminders, budget alerts, premium due, large transaction alert (>₹X)
- **Categories** — opens Categories Management
- **Rules** — opens Rules Engine
- **About** — version, privacy policy, open-source licenses

---

## 8. Data Model

### 8.1 Entities

```
Account
  id, name, type [SAVINGS|CURRENT|CASH|WALLET], institution, accountNumberLast4,
  openingBalance, currentBalance (computed), currency, icon, color,
  isArchived, displayOrder, createdAt

Card
  id, name, type [CREDIT|DEBIT|PREPAID], issuer, network [VISA|MASTERCARD|RUPAY|AMEX|DINERS],
  cardNumberLast4, creditLimit (nullable), currentOutstanding (computed),
  statementDayOfMonth, dueDayOfMonth, linkedAccountId (nullable, for debit cards),
  icon, color, isArchived, displayOrder, createdAt

Investment
  id, name, type [FD|RD|SIP|MUTUAL_FUND|EQUITY|GOLD_PHYSICAL|GOLD_DIGITAL|BONDS|PPF|EPF|NPS|ULIP|OTHER],
  institution, investedAmount (computed), currentValue,
  units (nullable), nav (nullable), startDate, maturityDate (nullable),
  taxSection (nullable, e.g., 80C), icon, color, linkedInsuranceId (nullable, for endowment), createdAt

Insurance
  id, name, type [HEALTH|VEHICLE|LIFE_TERM|LIFE_ENDOWMENT|TRAVEL|HOME|OTHER],
  provider, policyNumber, sumAssured, premiumAmount,
  premiumFrequency [MONTHLY|QUARTERLY|HALF_YEARLY|YEARLY|SINGLE], nextPremiumDate,
  startDate, endDate, nominee, agentContact (nullable), policyDocUri (nullable),
  taxSection (nullable, e.g., 80D), createdAt

Category
  id, name, parentId (nullable, for sub-categories), type [EXPENSE|INCOME|TRANSFER|INVESTMENT],
  icon, color, isSystem (true = cannot delete), displayOrder

Tag
  id, name, color

Person
  id, name, relation [SPOUSE|PARENT|SIBLING|CHILD|FRIEND|COLLEAGUE|BUSINESS|OTHER],
  contact (nullable), avatarUri (nullable), createdAt

Transaction
  id, type [EXPENSE|INCOME|TRANSFER|INVESTMENT_BUY|INVESTMENT_SELL|CARD_PAYMENT|REFUND|CASHBACK|INTEREST|LOAN_GIVEN|LOAN_RECEIVED|GIFT_SENT|GIFT_RECEIVED|ADJUSTMENT],
  amount, currency, date,
  description, categoryId (nullable for transfers), subCategoryId (nullable),
  sourceType [ACCOUNT|CARD|CASH|INVESTMENT|EXTERNAL], sourceId,
  destinationType (nullable for expense/income), destinationId (nullable),
  paymentApp [GPAY|PHONEPE|PAYTM|CRED|BHIM|BANK_APP|CARD_SWIPE|CASH|NETBANKING|OTHER],
  place (nullable), latitude (nullable), longitude (nullable),
  peopleIds [list], tagIds [list],
  receiptUri (nullable), notes (nullable),
  taxSection (nullable),
  recurringRuleId (nullable), isSplit (bool), splitGroupId (nullable),
  source [MANUAL|OCR|SMS|RECURRING|RULE], createdAt, updatedAt

Budget
  id, name, scope [OVERALL|CATEGORY], categoryId (nullable),
  amount, period [WEEKLY|MONTHLY|YEARLY], startDate,
  alertThresholdPercent, isActive

Goal
  id, name, targetAmount, targetDate, linkedAccountIds [list], linkedInvestmentIds [list],
  icon, color, isAchieved, createdAt

Subscription
  id, name, provider, amount, frequency [MONTHLY|QUARTERLY|YEARLY],
  nextDueDate, lastPaidDate, categoryId, paymentMethodType, paymentMethodId,
  status [ACTIVE|PAUSED|CANCELLED], autoCharge, logoUri, createdAt

RecurringRule
  id, name, transactionTemplate (JSON snapshot), frequency, dayOfMonth (or dayOfWeek),
  nextRunDate, lastRunDate, autoConfirm (bool), isActive

TransactionRule
  id, name, conditions (JSON), actions (JSON), priority, isActive

Settings (singleton)
  userName, currency, fiscalYearStartMonth (default 4 = April),
  themeMode, useDynamicColor, biometricLockEnabled, autoLockTimeoutSec,
  bottomNavItems (ordered list), notificationPrefs (JSON)
```

### 8.2 Key Relationships

- A `Transaction` always has a `source` (where the money came from) and, for transfers/investments, a `destination`.
- `currentBalance` and `currentOutstanding` are **derived** from transactions, never stored independently (except as cached values that recompute on transaction change).
- An `Insurance` of type `LIFE_ENDOWMENT` / `ULIP` has a 1:1 link to an `Investment` (premium payments hit both).

---

## 9. Categories (Pre-Seeded)

**Expense categories** (with sub-categories):

- **Food & Drink** — Groceries, Restaurants, Cafés & Coffee, Food Delivery, Snacks, Office Lunch
- **Transport** — Fuel, Public Transit, Ride-hailing (Ola/Uber/Rapido), Auto/Taxi, Parking, Tolls (FASTag), Vehicle Maintenance
- **Bills & Utilities** — Mobile, Internet, Electricity, Water, Gas, DTH/Cable, Maintenance/Society
- **Shopping** — Clothing, Electronics, Home Goods, Personal Care, Books & Stationery, Gifts
- **Health** — Doctor, Pharmacy, Hospital, Diagnostics, Gym & Fitness, Dental, Eye Care
- **Entertainment** — Movies, Events & Concerts, Streaming (→ may be Subscription), Games, Hobbies
- **Travel** — Flights, Trains, Buses, Hotels, Local Transport, Vacation, Travel Insurance
- **Home** — Rent, Home Maintenance, Repairs, Furniture, Appliances, Domestic Help (Maid/Cook)
- **Family** — Money to Parents, Money to Spouse, Money to Children, Family Gifts, Family Events
- **Friends** — Outings, Gifts, Lending (tracked separately too)
- **Religious & Spiritual** ★ — **Temple Donations** (hundi), **Sevas & Pujas** (archana, abhisheka, kalyanotsavam), **Prasadam**, **Pilgrimage**, **Religious Books/Items**, **Charity to Religious Orgs**
- **Festivals** — Diwali, Holi, Ganesh Chaturthi, Eid, Christmas, Other Festivals, Festival Gifts, Sweets & Snacks
- **Education** — Courses, Books, Tuition, Certifications, Online Learning
- **Personal Care** — Salon/Barber, Spa, Cosmetics, Accessories
- **Charity & Donations** — NGO, Crowdfunding, Disaster Relief, Other
- **Fees & Charges** — Bank Fees, Card Fees, Late Payment, Government Fees, Legal/CA Fees
- **Taxes** — Income Tax, Advance Tax, Property Tax, GST (business)
- **Loan EMI** — Home Loan, Personal Loan, Vehicle Loan, Education Loan, Credit Card Late
- **Insurance Premium** — Health, Vehicle, Term Life, Travel, Home *(separate from Investment-flavored premiums)*
- **Pets** — Food, Vet, Grooming, Accessories
- **Miscellaneous** — Other

**Income categories:**

- **Salary** — Base, Bonus, Variable, Reimbursement
- **Freelance / Business**
- **Interest** — Savings Account, FD, RD, Bonds
- **Dividends**
- **Rental Income**
- **Capital Gains** — Mutual Funds, Stocks, Gold
- **Refunds** — Purchase Refund, Tax Refund
- **Cashback & Rewards** — Credit Card Cashback, UPI Rewards, CRED Coins
- **Gifts Received**
- **Money from Family** (e.g., received back from spouse/parent)
- **Other Income**

**Investment categories** (internal — tied to investment type, not user-facing as expense categories):

- SIP Contribution, FD Booking, RD Installment, MF Purchase, Stock Purchase, Gold Purchase, PPF Deposit, NPS Contribution, ULIP/Endowment Premium, Bond Purchase

**Transfer categories** (internal):

- Between My Accounts, Credit Card Payment, Cash Withdrawal, Cash Deposit

---

## 10. Rules Engine — Pre-Seeded Rules

These ship enabled by default. User can disable any.

| # | Name | Condition | Action |
|---|------|-----------|--------|
| 1 | Credit Card Payment | Transfer to a Card of type CREDIT | Mark as `CARD_PAYMENT`, exclude from "expense" totals |
| 2 | LIC Premium is Investment | Description contains "LIC" AND From = bank account | Set type to INVESTMENT_BUY, link to LIC investment, set tax section 80C |
| 3 | ELSS SIP is Investment | Description contains "ELSS" or merchant = "Groww" | Set type to INVESTMENT_BUY, set tax section 80C |
| 4 | Health Insurance is 80D | Description contains "health insurance" OR linked to Insurance of type HEALTH | Set tax section 80D |
| 5 | Salary Credit | Description contains "salary" or "SAL" AND To = ICICI account | Set type to INCOME, category Salary › Base |
| 6 | Money to Spouse — prompt user | Transaction has a Person with `relation = SPOUSE` AND type = EXPENSE | Show "Transfer or Expense?" dialog at save time (see Section 7.5.1). User can set a permanent default later in Settings → Behavior |
| 7 | Money to Parents = Family Expense | Person tag = "Parents" | Category Family › Money to Parents (kept as expense) |
| 8 | UPI Refund | Description contains "refund" OR "REVERSAL" | Type = REFUND (offsets original expense by category) |
| 9 | Cashback | Description contains "cashback" OR source = CRED | Type = CASHBACK, category Cashback & Rewards |
| 10 | Temple Donation | Description contains "temple" or "devasthanam" or "hundi" | Category Religious › Temple Donations |

> **Spouse prompt behavior:** Because you chose "ask each time," Rule #6 doesn't auto-apply a type — it interrupts at save time with a small dialog (see 7.5.1). Two checkboxes in that dialog let you stop being asked and lock in a default whenever you're ready. Same logic applies if she sends money back to you (type = INCOME).

---

## 11. AI Features

### 11.1 AI Quick Entry — Hero Feature (Phase 3)

The fastest way to log a transaction. Three input modes — **text, voice, photo** — all in a single sheet. Powered by Gemini.

#### Entry Point

- **Long-press the `+` FAB** on any screen → opens the AI Quick Entry bottom sheet
- Tap `+` (short press) still opens the manual Add Transaction sheet
- Long-press chosen over a second FAB to keep the UI clean
- Subtle one-time tooltip on first dashboard load: "💡 Long-press + for AI Quick Entry"

#### The Input Sheet (bottom sheet, ~70% height)

```
┌──────────────────────────────────────────┐
│  ✨ Tell AI what you spent           ✕   │
├──────────────────────────────────────────┤
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ Type here…                         │  │
│  │ e.g. "350 at CCD via GPay          │  │
│  │       yesterday"                   │  │
│  │                                    │  │
│  └────────────────────────────────────┘  │
│                                          │
│  [ 🎤 Voice ]  [ 📷 Camera ]  [ 🖼 Gallery ] │
│                                          │
│  AI will fill the details.               │
│  You confirm before saving.              │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │       Parse with AI  →             │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

- **Multiline text input** at top, 3 lines visible, grows to 6
- **Three icon buttons in one row** below the input: mic, camera, gallery
- All three icons can be used **alongside text** (e.g., add a photo + type "this was for office lunch with team" for extra hints)
- Primary CTA: "Parse with AI" at bottom (also triggered by Enter on text)

#### Flow 1 — Text Input

1. User types: e.g., `"Spent 1200 on groceries at DMart, paid via PhonePe"`
2. Tap "Parse with AI" → Gemini API call with structured-output schema
3. Loading state (~1–3s): *"AI is reading your transaction…"*
4. Success → Preview Sheet (below)
5. Failure → toast: *"Couldn't parse that. Try rephrasing or enter manually"* + button to switch to manual

#### Flow 2 — Voice Input

1. Tap 🎤 → permission prompt on first use (`RECORD_AUDIO`)
2. Android's `SpeechRecognizer` starts; mic icon pulses red; input shows *"Listening…"*
3. Live transcription appears in the text input as user speaks
4. Auto-stops on ~2s silence, or user taps mic again to stop
5. **User can edit the transcription** before parsing (critical — misheard amounts are the #1 voice failure mode)
6. Tap "Parse with AI" → same path as text flow

> `[DECISION]` Use Android's on-device `SpeechRecognizer` with `en-IN` locale (free, works offline on most modern Androids, handles Hinglish reasonably). Falls back to network model if device doesn't support on-device. Pure Kannada/Hindi support can be added later by switching locale.

#### Flow 3 — Photo Input (Receipt OCR)

1. Tap 📷 → permission prompt on first use (`CAMERA`)
2. CameraX opens with a rectangular receipt-mode guide overlay
3. Capture → preview with Retake / Use buttons
4. (Or via 🖼️ gallery: standard `ACTION_PICK` image picker)
5. Selected image thumbnail appears above the text input
6. User can add text hints alongside (e.g., "this was a team lunch reimbursement")
7. Tap "Parse with AI" → Gemini Vision call with image (+ optional text)
8. Loading: *"AI is reading your receipt…"*
9. Success → Preview Sheet, **image attached as receipt automatically**
10. Failure (`not_a_receipt` / `unreadable`) → toast with manual fallback

#### The Preview Sheet (after any of the three flows)

This is the **standard Add Transaction sheet (Section 7.5)**, pre-filled, with two additions:

- **Banner at top:** *"✨ Filled by AI — review and edit, then save"*
- **Confidence dot next to each field:**
  - 🟢 Green — high confidence (>0.8)
  - 🟡 Yellow — medium (0.5–0.8), verify before saving
  - 🔴 Red — low (<0.5) or ambiguous, **must** be set manually before save can proceed

User edits any field → tap Save normally.

#### The Gemini Parser — Prompt Template

Single template, dynamic context per user. Sent as one API call with `responseFormat: JSON_OBJECT` (or function-calling equivalent).

```
You are a transaction parser for an Indian personal finance app.
Today's date: {today}
User: {userName}
User's accounts: {accounts as JSON list with names, types}
User's cards: {cards as JSON list with names, issuer, last4}
User's people: {people as JSON list with names, relations}
Top categories: {category tree summary}
Recent merchants: {top 20 from past transactions}

Parse the input into a transaction. Input may be text, transcribed voice,
or a receipt image (multimodal call).

Input: "{userText}"

Return JSON matching this schema strictly:
{
  "type": "EXPENSE" | "INCOME" | "TRANSFER",
  "amount": number,
  "date": "YYYY-MM-DD",
  "description": string,
  "merchant": string | null,
  "category": string | null,
  "subCategory": string | null,
  "sourceType": "ACCOUNT" | "CARD" | "CASH" | null,
  "sourceName": string | null,        // app fuzzy-matches to actual ID
  "paymentApp": "GPAY"|"PHONEPE"|"PAYTM"|"CRED"|"BHIM"|"BANK_APP"|"CARD_SWIPE"|"CASH"|null,
  "people": [string],
  "place": string | null,
  "tags": [string],
  "notes": string | null,
  "confidence": {
    "amount": 0-1, "source": 0-1, "category": 0-1, "date": 0-1
  },
  "ambiguities": [string]
}

Rules:
- Dates: "yesterday", "last Friday", "3 days back" → resolve to YYYY-MM-DD
- Numbers: "5k" = 5000, "1.5k" = 1500, "two hundred" = 200; strip "₹", "rs", "rupees"
- Family terms: "wife", "papa", "mom", "amma", "appa", "anna", "akka" →
  match People list by relation if name not given
- Indian merchants: Swiggy, Zomato, BlinkIt, Zepto, BigBasket, DMart, Reliance Fresh,
  More, Spencer's, Nature's Basket, CCD, Starbucks → infer category
- UPI keywords: "scanned", "qr", "upi" → likely UPI; guess GPay if no other hint
- Hindi/Kannada words:
    "doodh"/"haalu" = milk (Groceries),
    "chai" = tea (Cafés),
    "auto" = auto-rickshaw (Transport),
    "petrol"/"diesel" = Fuel,
    "kirana" = Groceries,
    "darshan"/"seva" = Religious › Sevas & Pujas,
    "hundi" = Religious › Temple Donations
- Match source to nearest by name/issuer; if ambiguous (e.g., "card" with multiple
  cards), set sourceName=null and add to ambiguities
- If multiple items with different amounts mentioned in one input, sum into ONE
  transaction; list item breakdown in notes (no split in v1)
- Return ONLY valid JSON. No markdown fences. No commentary.
```

**For receipt images:** same prompt, image attached, plus an instruction at top:
*"Analyze this receipt image. If not a receipt or unreadable, return `{"error":"not_a_receipt"|"unreadable","message":"..."}`. Amount = grand total only (not subtotal). Extract paymentApp from receipt if visible (e.g., 'PAID VIA PHONEPE')."*

#### Post-Parse Processing in App

1. **Fuzzy-match** `sourceName` → `sourceId` against accounts/cards (Levenshtein + token overlap)
2. **Fuzzy-match** `category` → `categoryId` (case-insensitive, partial)
3. **Match `people`** → `personIds`. If a person name is unknown, prompt user inline: *"Add 'Kiran' as a new person?"* — `[DECISION]` no silent auto-creation in v1
4. **Set confidence dots** based on Gemini's response
5. **Apply existing TransactionRules** (Section 10) on top — user's hard rules override AI's category (e.g., Rule #1 still flags credit card payments)

#### Edge Cases & Failure Handling

| Case | Behavior |
|---|---|
| Voice misheard amount | User edits transcription before parsing |
| Photo of non-receipt | Toast + fallback to manual; image not attached |
| Blurry receipt, amount unclear | Amount field flagged 🔴, must enter manually |
| Multiple amounts in one text | Sum into one transaction, items listed in notes |
| Ambiguous account ("on my card") | sourceName=null, field flagged 🔴 |
| Gemini API failure / no internet | Toast + fallback to manual; user's input preserved |
| User cancels mid-flow | Original input preserved in case they reopen the sheet |
| Past transactions empty (first use) | Skip "Recent merchants" context, Gemini uses general knowledge |

#### Permissions Added by This Feature

- `RECORD_AUDIO` — runtime, first 🎤 tap
- `CAMERA` — runtime, first 📷 tap (likely already granted from receipt attachment)
- `INTERNET` — manifest, always (Gemini needs network)

#### API Key Handling

- **Phase 3 (Claude Code build):** Gemini API key stored in `local.properties` (already gitignored), exposed to Kotlin via `BuildConfig.GEMINI_API_KEY`. Add `local.properties` to `.gitignore` if not already there. Never commit the key.
- **Phase 5 hardening (Play Store):** offer **BYOK** in Settings → AI (user pastes their own Gemini key, stored in EncryptedSharedPreferences) OR backend proxy. Do not ship a hardcoded key in a public APK.

#### Cost & Rate Limit

- Each Quick Entry = 1 Gemini call. 30 transactions/day × 30 = 900 calls/month.
- Comfortably within Gemini free tier during v1 testing.
- Settings → AI shows: *"AI calls this month: 247 / 1500 free tier"* so user sees usage.

### 11.2 Smart Categorization (in manual entry)

When user types in the description field of manual Add Transaction, a debounced Gemini call (300ms after typing stops) suggests a category. Shown as a tappable chip below the field: *"Suggest: Food › Restaurants"* — tap to apply, ignore to dismiss. Phase 3+, optional toggle in Settings → AI.

### 11.3 Monthly Insights (Phase 4)

On the 1st of each month, generate a "month in review" summary using Gemini, shown on the Reports screen and as a notification:

> *"Your spending was 12% higher than last month, mostly in Food Delivery (+₹3,400). You saved ₹X by skipping ride-hailing on weekdays. Your Section 80C is 78% utilized for FY26 — consider topping up ELSS before March."*

One Gemini call per month, cached locally.

---

## 12. Security & Privacy

- **App lock:** biometric (fingerprint/face) + PIN fallback. Required on first open, after auto-lock timeout (default 60s).
- **Storage:** Room DB on internal storage (private to app).
- **No analytics, no telemetry** in v1.
- **Backup:** user-initiated only, exports encrypted file to user-chosen location.
- **Permissions requested:**
  - Camera (for receipts & AI Quick Entry photo) — runtime, on first use
  - Microphone (for AI Quick Entry voice) — runtime, on first use
  - Location (optional, for transaction place) — runtime, when toggled on
  - Notifications — runtime
  - SMS (Phase 5 only, if SMS parsing added) — runtime
- **Internet permission:** required for Gemini API (Phase 3+). MVP (Phases 1–2) works fully offline.

---

## 13. Phased Build Plan with Claude Code

Claude Code is agentic and iterative — unlike a single-prompt tool, it can carry context across many turns within a session, edit files in place, run builds, and review diffs. The "phase" framing below is for **milestone management** (what to ship before moving on), not for prompt compression. Each phase will typically take multiple Claude Code sessions.

**Persistent project context** lives in a `CLAUDE.md` file at the repo root — Claude Code reads it at the start of every session, so you don't re-explain the project. The PRD lives in `docs/PRD.md` and CLAUDE.md points to it for full specs.

### Phase 0 — Setup (one-time, done by you before Claude Code touches anything)

1. Install **JDK 17** (required by Android Gradle Plugin)
2. Install **Android Studio** (gives you the SDK, build tools, ADB, AVD emulator) — even though you'll edit in VS Code, you still need the Android SDK
3. Install **Node.js LTS** (Claude Code is an npm package)
4. Install **Claude Code**: `npm install -g @anthropic-ai/claude-code`
5. Install **Claude Code VS Code extension** (Spark icon in sidebar; extension bundles the CLI)
6. Create an empty Git repo for the project, open in VS Code
7. Add `CLAUDE.md` and `docs/PRD.md` (deliverables provided alongside this PRD)
8. Authenticate Claude Code (OAuth via Max plan or API key)
9. Optional: create an AVD (Pixel 7, API 34) in Android Studio so the emulator is ready

### Phase 1 — MVP Skeleton

**Scope:** Splash + Onboarding + Dashboard + Transactions list + Add Transaction (Expense/Income/Transfer) + Accounts + Account detail + Cards + Card detail + Settings (basic) + Categories management. Categories pre-seeded with the full list from Section 9 including the Religious & Spiritual tree. Spouse-prompt dialog (Section 7.5.1). Three hardcoded rules (Section 10, rules #1, #3, and the spouse trigger).

**Workflow with Claude Code:**

1. **Session 1 — Project bootstrap.** Ask Claude Code to initialize the Android project structure (Gradle, modules, Compose dependencies, Room, Vico, kotlinx-datetime). Verify `./gradlew assembleDebug` succeeds before going further.
2. **Session 2 — Data layer.** Build all Room entities (Section 8), DAOs, the Repository pattern, and the category seeder. Add instrumentation tests for balance computation.
3. **Session 3 — Theming & navigation.** Material 3 + Material You + Light/Dark theme + bottom nav scaffold with 5 items + More drawer.
4. **Session 4 — Splash + Onboarding.** End-to-end first-run flow.
5. **Session 5 — Add Transaction + spouse-prompt dialog.** The single hardest screen — give it full attention.
6. **Session 6 — Dashboard + Transactions list.**
7. **Session 7 — Accounts + Account detail.**
8. **Session 8 — Cards + Card detail.**
9. **Session 9 — Settings + Categories screen + More drawer.**
10. **Session 10 — Polish pass.** Empty states, Indian number formatting, pull-to-refresh, validation, animations. Run the 10 acceptance tests.

**Per-session discipline:**
- Start each session with: *"Read CLAUDE.md and docs/PRD.md sections X-Y, then…"*
- End each session with: *"Update CLAUDE.md with anything important we decided that wasn't there before."*
- Commit at the end of each session (`feat: ...`)
- Run `./gradlew assembleDebug && ./gradlew test` before commit

### Phase 2 — Investments, Insurance, Card Detail Polish, Rules Engine UI

**Scope:** Investments page + detail (FD, RD, SIP, MF, Gold, Bonds, PPF, EPF, NPS, ULIP, Equity); Insurance page + detail with premium-due reminders; Rules Engine UI to view/create/edit/toggle rules; pre-seeded rules from Section 10 fully wired with execution at save-time.

**Workflow:** 4–6 sessions. Insurance ↔ Investment linkage for endowment/ULIP needs careful thought.

### Phase 3 — AI Quick Entry (Hero Feature)

**Scope:** The full AI Quick Entry flow (Section 11.1) — long-press FAB → bottom sheet with text + voice + photo input in one bar → Gemini parser → confidence-tagged preview → confirm & save. Plus smart-categorization-while-typing in manual entry (11.2).

**Setup tasks specific to this phase:**

1. Get a Gemini API key from Google AI Studio (the *website*, ironic but separate from the Studio Build tool we previously planned). Store in `local.properties` → expose via `BuildConfig`.
2. Add Gemini Android SDK dependency.
3. Wire CameraX for the photo flow.
4. Wire Android `SpeechRecognizer` (en-IN) for the voice flow.

**Test cases to validate Phase 3:**

- *Text:* "350 at CCD via GPay yesterday" → Food › Cafés, GPay, yesterday's date, no source ambiguity
- *Text:* "Spent 1.5k on petrol" → Transport › Fuel, ₹1500, source flagged 🔴 (no payment method)
- *Voice:* "Paid 200 rupees for darshan at the temple this morning" → Religious › Sevas & Pujas
- *Photo:* DMart bill → Groceries, total grand amount, items in notes
- *Mixed:* Photo of Swiggy receipt + text "with office team" → Food › Office Lunch, people tagged appropriately

### Phase 4 — Budgets, Goals, Subscriptions, Recurring, People

**Scope:** All the "extras." Each is a small self-contained feature. Notifications via WorkManager. Each can be 1–2 sessions.

### Phase 5 — Hardening for Real Use / Play Store

**Scope:**
- SMS parsing (BroadcastReceiver for bank SMS → auto-create transactions for review)
- Biometric / PIN lock on app open
- EncryptedSharedPreferences for the Gemini key (if BYOK enabled) and any other secrets
- Encrypted backup/restore (room DB → encrypted zip → user's chosen location)
- Full Reports/Analytics screen with FY-aware charts
- ProGuard / R8 rules, signing config, version bumping discipline
- Crash reporting (Firebase Crashlytics or Sentry — opt-in)
- Optional cloud sync (Firebase Firestore + Auth) — only if you truly want multi-device. Adds significant complexity.

---

## 14. Open Decisions — ALL RESOLVED ✅

1. ~~**App name**~~ → **Artha** ✅
2. ~~**Bottom nav default**~~ → **5 visible (Dashboard, Transactions, Accounts, Cards, More) + drawer** ✅
3. ~~**Money-to-Spouse default**~~ → **Ask each time** via dialog at save (Section 7.5.1), with permanent-default escape hatch ✅
4. ~~**Storage**~~ → **Local-only (Room DB on device)** for v1. No cloud, no login. Backup via user-initiated file export. ✅
5. ~~**App icon**~~ → **Deferred** to post–Phase 1. Use a placeholder for the MVP build; design icon once we see the app come alive. ✅
6. ~~**Currency**~~ → **INR only** for v1. Multi-currency deferred (low value, high complexity). ✅

**Status:** Ready to start Phase 0 (setup) and Phase 1 (Claude Code sessions). Deliverables: this PRD + `CLAUDE.md` + Phase 1 task playbook.

---

## 15. Out of Scope (v1)

- Multi-user / family sharing
- Web or iOS clients
- Direct bank account linking (Account Aggregator / Plaid-equivalent)
- Stock portfolio live prices
- Tax filing (just tax-section *tracking*)
- Receipt storage in cloud
- Crypto holdings
- Group splitwise (3+ people) — deferred to Phase 4 if time permits

---

## 16. Success Criteria

- I stop using Wallet / Paisa / Excel within 2 weeks of MVP installation.
- 100% of my real transactions for one full month are captured in the app.
- The Section 80C bucket at FY end matches what my CA computes (within ₹1,000).
- Net worth on the dashboard matches my manual reconciliation across all accounts/cards/investments (within ₹500).

---

*End of PRD v0.1*
