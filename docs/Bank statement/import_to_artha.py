"""
One-shot bank-statement importer for Artha.

Reads canonical statement files in this folder, deduplicates, categorises by
keyword, and writes the result into Artha's Room SQLite database.

Two delivery modes:

  1. --apply: device path. App must be installed (debug build) and connected
     via adb. Force-stops the app, pulls the DB, inserts everything, pushes
     back. Requires USB or wireless adb.

  2. --export-json: bundles the import as a JSON asset shipped inside the
     APK. Writes to `app/src/main/assets/seed/bank_import.json`. The app's
     Settings -> "Import bundled bank data" tile reads it. No adb required.
     This is what we use when adb isn't available (e.g. remote machine).

USAGE:

  # Dry-run (default) — analyses, categorises, prints summary, writes nothing.
  python "docs/Bank statement/import_to_artha.py"

  # Apply via adb (when a device is reachable).
  python "docs/Bank statement/import_to_artha.py" --apply

  # Apply via APK bundle (no adb required).
  python "docs/Bank statement/import_to_artha.py" --export-json

  # Print the full categorised list (instead of just samples).
  python "docs/Bank statement/import_to_artha.py" --verbose

DECISIONS:

* New accounts are always created (FEDERAL_ACCOUNT_NAME / ICICI_ACCOUNT_NAME).
  If the user already has accounts with those exact names, the importer reuses
  them and skips creation; otherwise it creates fresh ones with the opening
  balance taken from the earliest available statement of that bank.
* Default txn type is EXPENSE (for withdrawals) / INCOME (for deposits) with
  keyword overrides (salary / interest / refund / cashback).
* Transfers between own accounts and CARD_PAYMENT are NOT auto-detected. They
  remain plain EXPENSE/INCOME — re-categorise in-app.
* Dedup key across files: (bank, date, withdrawal_paise, deposit_paise,
  normalised_description).
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
import re
import shutil
import sqlite3
import subprocess
import sys
import tempfile
import unicodedata
import uuid
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import datetime, date, timezone
from pathlib import Path

HERE = Path(__file__).parent
PROJECT_ROOT = HERE.parent.parent
PACKAGE = "com.subramanya.artha"
LOCAL_DB_DIR = PROJECT_ROOT / "build" / "import-temp"
LOCAL_DB_DIR.mkdir(parents=True, exist_ok=True)
LOCAL_DB = LOCAL_DB_DIR / "artha.db"

FEDERAL_ACCOUNT_NAME = "Federal Bank (Jupiter)"
ICICI_ACCOUNT_NAME = "ICICI Bank"

# Which files to actually use. Skipping byte-identical dupes and the
# overlap-with-FY file (the FY-aligned file is canonical).
FEDERAL_FILES = [
    # (filename, label)
    ("AccountStatement_01-APR-2022_to_31-MAR-2023.xlsx", "Federal FY22-23"),
    ("AccountStatement_01-APR-2023_to_31-MAR-2024.xlsx", "Federal FY23-24"),
    ("AccountStatement_01-APR-2024_to_31-MAR-2025.xlsx", "Federal FY24-25"),
    ("AccountStatement_01-APR-2025_to_31-MAR-2026.xlsx", "Federal FY25-26"),
    ("AccountStatement_01-APR-2026_to_21-MAY-2026.xlsx", "Federal Apr-May 2026"),
    # Skipped: AccountStatement_25-MAR-2022_to_01-JAN-2023.xlsx  (overlaps FY22-23)
    # Skipped: AccountStatement_01-APR-2026_to_21-MAY-2026(1).xlsx (byte-identical dup)
    # Skipped: AccountStatement_01-MAY-2026_to_21-MAY-2026.xlsx  (empty period)
]
ICICI_FILES = [
    ("OpTransactionHistory20-05-2026.xls-13-28-12.xls", "ICICI Aug-Dec 2022"),
    ("OpTransactionHistory20-05-2026.xls-13-27-37.xls", "ICICI 2023"),
    ("OpTransactionHistory20-05-2026.xls-13-26-52.xls", "ICICI 2024"),
    ("OpTransactionHistory20-05-2026.xls-13-26-05.xls", "ICICI 2025"),
    ("OpTransactionHistory20-05-2026.xls-13-23-54.xls", "ICICI Jan-May 2026"),
    # PDF skipped (redundant with the Jan-May 2026 xls)
]

# -----------------------------------------------------------------------------
# Parsing
# -----------------------------------------------------------------------------

@dataclass(frozen=True)
class RawTxn:
    bank: str  # 'federal' or 'icici'
    source_file: str
    date: date
    description: str
    withdrawal: float | None  # money out
    deposit: float | None     # money in
    balance: float | None

def _parse_date(v) -> date | None:
    if v in (None, ""):
        return None
    if isinstance(v, datetime):
        return v.date()
    if isinstance(v, date):
        return v
    s = str(v).strip()
    for fmt in ("%d/%m/%Y", "%d-%m-%Y", "%Y-%m-%d", "%d.%m.%Y", "%d %b %Y", "%d-%b-%Y", "%d/%b/%Y"):
        try:
            return datetime.strptime(s, fmt).date()
        except ValueError:
            continue
    return None


def _parse_money(v) -> float | None:
    if v in (None, ""):
        return None
    if isinstance(v, (int, float)):
        return float(v)
    s = re.sub(r"[,\s]", "", str(v))
    if not s or s == "-":
        return None
    try:
        return float(s)
    except ValueError:
        return None


def _normalize_description(s: str) -> str:
    """Strip prefixes, collapse whitespace, lowercase. Used for dedup + matching."""
    s = unicodedata.normalize("NFKD", s).lower()
    s = re.sub(r"[\r\n\t]+", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


def _load_xlsx(p: Path) -> list[list]:
    import openpyxl
    wb = openpyxl.load_workbook(p, read_only=True, data_only=True)
    return list(wb.active.iter_rows(values_only=True))


def _load_xls(p: Path) -> list[list]:
    import xlrd
    book = xlrd.open_workbook(p)
    sh = book.sheets()[0]
    return [sh.row_values(r) for r in range(sh.nrows)]


def _find_header_row(rows: list, hints: set[str]) -> int | None:
    for i, r in enumerate(rows):
        cells = [str(c or "").strip().lower() for c in r]
        joined = " ".join(cells)
        hits = sum(1 for h in hints if h in joined)
        if hits >= 3:
            return i
    return None


def parse_federal(path: Path) -> tuple[list[RawTxn], float | None]:
    """Returns (transactions, opening_balance)."""
    rows = _load_xlsx(path)
    header_idx = _find_header_row(rows, {"date", "particulars", "withdrawals", "deposits", "balance"})
    if header_idx is None:
        return [], None
    header = [str(c or "").strip().lower() for c in rows[header_idx]]
    col_date = header.index("date")
    col_particulars = header.index("particulars")
    col_wd = header.index("withdrawals")
    col_dep = header.index("deposits")
    col_bal = header.index("balance")

    opening = None
    txns: list[RawTxn] = []
    for r in rows[header_idx + 1:]:
        if not r or all(c in (None, "") for c in r):
            continue
        desc = str(r[col_particulars] or "").strip()
        wd = _parse_money(r[col_wd])
        dep = _parse_money(r[col_dep])
        bal = _parse_money(r[col_bal])

        # Opening balance row
        if opening is None and "opening balance" in desc.lower():
            opening = bal
            continue
        # Closing/totals/grand-total rows
        if any(k in desc.lower() for k in ("closing balance", "grand total", "****end of statement****")):
            continue
        d = _parse_date(r[col_date])
        if d is None and wd is None and dep is None:
            continue
        if d is None:
            continue
        txns.append(RawTxn(
            bank="federal",
            source_file=path.name,
            date=d,
            description=desc,
            withdrawal=wd,
            deposit=dep,
            balance=bal,
        ))
    return txns, opening


def parse_icici(path: Path) -> tuple[list[RawTxn], float | None]:
    rows = _load_xls(path)
    header_idx = _find_header_row(rows, {"transaction date", "transaction remarks", "withdrawal", "deposit", "balance"})
    if header_idx is None:
        return [], None
    header = [str(c or "").strip().lower() for c in rows[header_idx]]
    # ICICI columns: '', 's no.', 'value date', 'transaction date', 'cheque number', 'transaction remarks', 'withdrawal amount(inr)', 'deposit amount(inr)', 'balance(inr)'
    def idx(*needles):
        for n in needles:
            for i, h in enumerate(header):
                if n in h:
                    return i
        return None
    col_date = idx("transaction date")
    col_remarks = idx("transaction remarks")
    col_wd = idx("withdrawal")
    col_dep = idx("deposit")
    col_bal = idx("balance")

    opening = None
    txns: list[RawTxn] = []
    for r in rows[header_idx + 1:]:
        if not r or all(c in (None, "") for c in r):
            continue
        desc = str(r[col_remarks] or "").strip() if col_remarks is not None and col_remarks < len(r) else ""
        wd = _parse_money(r[col_wd]) if col_wd is not None and col_wd < len(r) else None
        dep = _parse_money(r[col_dep]) if col_dep is not None and col_dep < len(r) else None
        bal = _parse_money(r[col_bal]) if col_bal is not None and col_bal < len(r) else None
        d = _parse_date(r[col_date]) if col_date is not None and col_date < len(r) else None

        # ICICI doesn't print an explicit opening balance — capture first balance value
        # before the first txn so we can infer.
        if opening is None and bal is not None and wd is None and dep is None:
            opening = bal
            continue
        if d is None and wd is None and dep is None:
            continue
        if d is None:
            continue
        # Skip rows that look like footers / legends
        lower = desc.lower()
        if any(k in lower for k in ("legend", "narration code", "this is a system generated", "computer generated")):
            continue
        txns.append(RawTxn(
            bank="icici",
            source_file=path.name,
            date=d,
            description=desc,
            withdrawal=wd,
            deposit=dep,
            balance=bal,
        ))
    # If we never saw an opening row, compute it as: first txn's balance ± its delta.
    if opening is None and txns:
        first = txns[0]
        if first.balance is not None:
            opening = first.balance + (first.withdrawal or 0) - (first.deposit or 0)
    return txns, opening


# -----------------------------------------------------------------------------
# Dedup
# -----------------------------------------------------------------------------

def dedup(txns: list[RawTxn]) -> list[RawTxn]:
    seen: set[tuple] = set()
    out: list[RawTxn] = []
    for t in txns:
        key = (
            t.bank,
            t.date.isoformat(),
            int(round((t.withdrawal or 0) * 100)),
            int(round((t.deposit or 0) * 100)),
            _normalize_description(t.description)[:60],
        )
        if key in seen:
            continue
        seen.add(key)
        out.append(t)
    return out


# -----------------------------------------------------------------------------
# Categorisation — keyword → (parent_id, sub_id or None)
# -----------------------------------------------------------------------------

# Self handles — confirmed by user. Any txn touching these is a transfer
# between own accounts → TRANSFER type, no source-side category.
SELF_HANDLE_RE = re.compile(
    "|".join([
        r"9008059668@\w+",                                  # Any 9008059668@... — user's own
        r"subramanyagbellary-?\d*@\w+",                     # All -1/-2/-3/-4 variants
        r"subramanya\.g2@\w+",
        r"7892389809@\w+",                                  # user-confirmed: own
        r"\b917892389809@\w+",                              # international-prefixed variant
        r"sgbppay\d?@\w+",                                  # SGB Pay aliases
        r"sbellary@\w+",                                    # S Bellary handle (Federal etc.)
    ]),
    re.IGNORECASE,
)

# Counterparty handles → (regex, category_id, sub_id or None, type_override or None)
# type_override forces a non-default TransactionType (EXPENSE for outflows, INCOME for inflows).
COUNTERPARTY_RULES: list[tuple[str, str, str | None, str | None]] = [
    # Dad's accounts — Gopal N Bellary. User-confirmed.
    (r"gopalbellary@yb|gopalnbellary@i|gopal\s*n?\s*bellary", "cat_family", "cat_family_money_to_parents", None),
    # Wife — Sai Santhoshini (7287958875). User-confirmed.
    (r"7287958875@(?:ibl|axl|ybl|paytm|axis|axisbank)|sai\.santhoshini@\w+", "cat_family", "cat_family_money_to_spouse", None),
    # Sister — Vaishnavi Gopal Bellary (also pays via 7026192215). User-confirmed.
    (r"vaishnavi\s*gopal\s*b[ae]llary|vaishnavigopalballary@\w+|7026192215@\w+", "cat_family", None, None),
    # Friends — Anjana Talawar, Rahul (7483641376 + rahulsavukar), Kiran sir (9900200002),
    # Abhilash (9035105189). All college/friend circle → Friends > Outings.
    (r"anjanatalawar\d*@\w+|anjana\s*talawar", "cat_friends", "cat_friends_outings", None),
    (r"7483641376@\w+|rahulsavukar@\w+", "cat_friends", "cat_friends_outings", None),
    (r"9900200002@\w+", "cat_friends", "cat_friends_outings", None),  # Kiran sir (college)
    (r"9035105189@\w+", "cat_friends", "cat_friends_outings", None),  # Abhilash (college)
    (r"9740095334@\w+", "cat_friends", "cat_friends_outings", None),  # Vishwanath
    (r"7406906704@\w+", "cat_friends", "cat_friends_outings", None),  # Rajeev
    (r"8762201257@\w+", "cat_friends", "cat_friends_outings", None),  # Sachin
    (r"9901947929@\w+", "cat_friends", "cat_friends_outings", None),  # Minaz
    # Old PG rent — three variants of the "crib" handle (Axis / Cashfree / ICICI routing)
    # plus the landlord's phone (8884342225). User-confirmed.
    (r"crib\.cf@\w+|cf\.crib@\w+|cfcrib@\w+|\bcrib\b|8884342225@\w+", "cat_home", "cat_home_rent", None),
    # Simpl pay-later (any handle variant) → user wants Bills > Mobile.
    (r"\bsimpl(?:online)?@\w+|\bsimpl\b", "cat_bills_utilities", "cat_bills_utilities_mobile", None),
    # CRED — outgoing = credit-card payment, incoming = CRED Coins/cashback.
    # Broadened to match cred.club / cred.telecom / cred.voucher / cred.foo@anything.
    (r"cred\.\w+@\w+|credclub@\w+|\bcredcash@\w+", "cat_loan_emi", "cat_loan_emi_credit_card_late", None),
    # Snapmint BNPL EMI loan app — user-confirmed.
    (r"snapmint\.payu@\w+|\bsnapmint\b", "cat_loan_emi", "cat_loan_emi_personal_loan", None),
    # Mutual-fund SIPs via BSE Star MF.
    (r"bsestarmfrzp@\w+|bse\s*star\s*mf", "cat_sip_contribution", None, None),
    # Bangalore Metro recharges + BMTC (city bus) recharges routed via Airtel/Razorpay.
    (r"english\.?bmrc\.?payu@\w+|bmtc\.rzp@\w+|\bbmtc\b", "cat_transport", "cat_transport_public_transit", None),
    # Eatclub — corporate-meal delivery service.
    (r"eatclub@\w+|\beatclub\b", "cat_food_drink", "cat_food_drink_food_delivery", None),
    # Deutsche Bank refund routing — descriptions actually contain "RefundRef".
    (r"deut\d+@\w+|\bdeutsche\s*bank\b", "cat_refunds", "cat_refunds_purchase_refund", "REFUND"),
    # BHIM cashback (always incoming).
    (r"bhimcashback@\w+", "cat_cashback_rewards", "cat_cashback_rewards_upi_rewards", "CASHBACK"),
    # Employer — Deevia Software → Salary > Base.
    (r"deevia\s*software|\bdeevia\b", "cat_salary", "cat_salary_base", "INCOME"),
    # FD / RD funding from savings — "Dr. Tran for funding A/c <num>/SUBRAMANYA"
    # These are own-account capital transfers into a fixed deposit.
    (r"dr\.\s*tran\s*for\s*funding\s*a/c|funding\s*a/c\s*\d+", "cat_between_my_accounts", None, "TRANSFER"),
    # FD closure proceeds back into savings → also TRANSFER (own → own).
    (r"fd-?\d+\s*closure\s*proceeds", "cat_between_my_accounts", None, "TRANSFER"),
]

# Order matters: first match wins. Highest-confidence patterns first.
CATEGORY_RULES: list[tuple[str, str, str | None]] = [
    # ---- INCOME / non-expense overrides ----
    (r"\bsalary\b|sal\s*cr|salary\s*credit", "cat_salary", "cat_salary_base"),
    (r"\binterest\b|\bint\s*cr|sb\s*int", "cat_interest", "cat_interest_savings_acct"),
    (r"\brefund\b|rfnd|refund\s*from", "cat_refunds", "cat_refunds_purchase_refund"),
    (r"cash\s*back|cashback|cback", "cat_cashback_rewards", "cat_cashback_rewards_card_cashback"),
    (r"upi\s*rewards|jupiter\s*rewards|jupiter\s*cb", "cat_cashback_rewards", "cat_cashback_rewards_upi_rewards"),
    (r"\bdividend\b|div\s*cr", "cat_dividends", None),

    # ---- TRANSPORT ----
    (r"ola\s*money|olacabs|uber|rapido|namma\s*yatri|inDrive|in\s*drive", "cat_transport", "cat_transport_ride_hailing"),
    (r"\bfuel\b|petrol|diesel|indian\s*oil|iocl|hpcl|bpcl|hp\s*petrol|shell|bharat\s*petroleum|reliance\s*petroleum", "cat_transport", "cat_transport_fuel"),
    (r"bmrcl|namma\s*metro|metro\s*card|delhi\s*metro|chennai\s*metro", "cat_transport", "cat_transport_public_transit"),
    (r"fastag|paytm\s*fastag|hdfc\s*fastag|icici\s*fastag|toll\s*plaza", "cat_transport", "cat_transport_tolls"),
    (r"parking|park\s*plus|getparkin|paytm\s*parking", "cat_transport", "cat_transport_parking"),
    (r"\bauto\b|\btaxi\b", "cat_transport", "cat_transport_auto_taxi"),

    # ---- FOOD & DRINK ----
    (r"swiggy|zomato|food\s*panda|ubereats|uber\s*eats|eatsure|faasos", "cat_food_drink", "cat_food_drink_food_delivery"),
    (r"bigbasket|grofers|blinkit|zepto|dunzo|amazon\s*fresh|reliance\s*fresh|spencer|nature[\s'-]*basket", "cat_food_drink", "cat_food_drink_groceries"),
    (r"mother\s*dairy|md\s*store|nandini|amul", "cat_food_drink", "cat_food_drink_groceries"),
    (r"dominos|pizza\s*hut|kfc|mcdonalds|mcd|subway|burger\s*king|wow\s*momo|haldiram|barbeque\s*nation|biryani|behrouz|jubilant", "cat_food_drink", "cat_food_drink_restaurants"),
    (r"starbucks|barista|cafe\s*coffee|ccd|chai\s*point|chaayos|third\s*wave|blue\s*tokai", "cat_food_drink", "cat_food_drink_cafes_coffee"),
    (r"office\s*lunch|office\s*meal|canteen|cafeteria", "cat_food_drink", "cat_food_drink_office_lunch"),

    # ---- SHOPPING ----
    (r"amazon\s*pay|amzn|amazon\s*in|amazon\.in|amazonupi@\w+", "cat_shopping", "cat_shopping_electronics"),
    (r"flipkart|fk\s*pay|fkrt|ekart@\w+|\bekart\b", "cat_shopping", "cat_shopping_electronics"),
    (r"samsung\d*\.payu@\w+|\bsamsung\s*pay", "cat_shopping", "cat_shopping_electronics"),
    (r"myntra|ajio|nykaa|meesho|tata\s*cliq|reliance\s*digital|croma|vijay\s*sales", "cat_shopping", "cat_shopping_clothing"),
    (r"decathlon|sportswear|adidas|nike|puma", "cat_shopping", "cat_shopping_clothing"),
    (r"ikea|pepperfry|urban\s*ladder|home\s*centre", "cat_shopping", "cat_shopping_home_goods"),
    (r"\bbookstore\b|crossword|kindle\s*store|stationery", "cat_shopping", "cat_shopping_books_stationery"),

    # ---- BILLS & UTILITIES ----
    (r"airtel\s*postpaid|airtel\s*prepaid|airtel\s*recharge|airtel\s*mobile|jio\s*recharge|jio\s*prepaid|jio\s*postpaid|vi\s*postpaid|vodafone|idea\s*cellular|bsnl\s*mobile", "cat_bills_utilities", "cat_bills_utilities_mobile"),
    (r"jio\s*fiber|jiofiber|act\s*broadband|act\s*fibernet|hathway\s*broadband|airtel\s*xstream|broadband", "cat_bills_utilities", "cat_bills_utilities_internet"),
    (r"bescom|bses|tata\s*power|adani\s*electricity|electricity\s*bill|electricity\s*board", "cat_bills_utilities", "cat_bills_utilities_electricity"),
    (r"\bgas\s*bill|indane|hp\s*gas|gas\s*cylinder|gas\s*refill|igl\s*gas|mahanagar\s*gas", "cat_bills_utilities", "cat_bills_utilities_gas"),
    (r"water\s*bill|bwssb|jal\s*board", "cat_bills_utilities", "cat_bills_utilities_water"),
    (r"tata\s*sky|d2h|airtel\s*dth|dish\s*tv|sun\s*direct|cable\s*tv", "cat_bills_utilities", "cat_bills_utilities_dth_cable"),
    (r"society\s*maint|apartment\s*maint|mygate|nobroker|adda\b", "cat_bills_utilities", "cat_bills_utilities_maintenance_society"),

    # ---- ENTERTAINMENT ----
    (r"netflix|prime\s*video|hotstar|disney\+|disney\s*plus|sonyliv|sony\s*liv|zee5|jiocinema|jio\s*cinema|youtube\s*premium|appletv|apple\s*tv|spotify|amazon\s*music|wynk|gaana", "cat_entertainment", "cat_entertainment_streaming"),
    (r"bookmyshow|pvr|inox|cinepolis|imax", "cat_entertainment", "cat_entertainment_movies"),
    (r"steam|epic\s*games|playstation|psn|xbox\s*live|game\s*pass", "cat_entertainment", "cat_entertainment_games"),

    # ---- TRAVEL ----
    (r"makemytrip|mmt|goibibo|cleartrip|easemytrip|yatra|ixigo|booking\.com|bookingcom|agoda|airbnb|oyo|treebo", "cat_travel", "cat_travel_hotels"),
    (r"irctc|indian\s*railways|train\s*ticket", "cat_travel", "cat_travel_trains"),
    (r"indigo|spicejet|airindia|air\s*india|vistara|akasa|emirates|lufthansa|qatar\s*airways", "cat_travel", "cat_travel_flights"),
    (r"redbus|abhi\s*bus|abhibus|vrlonline@\w+|\bvrl\s*travel|\bvrl\s*logistics", "cat_travel", "cat_travel_buses"),

    # ---- HEALTH ----
    (r"apollo\s*pharmacy|pharmeasy|netmeds|1mg|tata\s*1mg|medplus|medlife", "cat_health", "cat_health_pharmacy"),
    (r"apollo\s*hospital|fortis|manipal\s*hospital|narayana|aster|max\s*hospital|practo|tata\s*croma", "cat_health", "cat_health_hospital"),
    (r"healthifyme|cult\.?fit|cultfit|cure\.?fit|gym|fitness|fitpass", "cat_health", "cat_health_gym_fitness"),
    (r"dental|dentist", "cat_health", "cat_health_dental"),
    (r"lenskart|titan\s*eye|optical|eye\s*care", "cat_health", "cat_health_eye_care"),

    # ---- HOME ----
    (r"\brent\b|house\s*rent|monthly\s*rent", "cat_home", "cat_home_rent"),
    (r"urban\s*company|urbanclap|maid|cook|housekeep", "cat_home", "cat_home_domestic_help"),

    # ---- RELIGIOUS & SPIRITUAL ----
    (r"temple|hundi|seva|abhisheka|kalyanotsavam|archana|prasadam|matha|mutt|trust|pilgrimage|tirupati|tirumala|isckon|iskcon", "cat_religious_spiritual", "cat_religious_spiritual_temple_donations"),

    # ---- FAMILY / FRIENDS / SPOUSE ----
    (r"\bwife\b|\bspouse\b|to\s*wife|to\s*spouse", "cat_family", "cat_family_money_to_spouse"),
    (r"mom|mother|dad|father|appa|amma|naanu|parents|to\s*parents", "cat_family", "cat_family_money_to_parents"),

    # ---- LOAN EMI ----
    (r"home\s*loan\s*emi|hl\s*emi", "cat_loan_emi", "cat_loan_emi_home_loan"),
    (r"personal\s*loan|pl\s*emi", "cat_loan_emi", "cat_loan_emi_personal_loan"),
    (r"car\s*loan|vehicle\s*loan", "cat_loan_emi", "cat_loan_emi_vehicle_loan"),

    # ---- INVESTMENT / SIP (Phase 2 categories — fall back to investment parent) ----
    (r"\bsip\b|systematic\s*investment|hdfc\s*amc|icici\s*pru\s*amc|nippon\s*amc|sbi\s*mutual|axis\s*mutual|kotak\s*mutual", "cat_sip_contribution", None),
    (r"zerodha|groww|upstox|kuvera|coin", "cat_stock_purchase", None),

    # ---- INSURANCE ----
    (r"lic\b|hdfc\s*life|sbi\s*life|max\s*life|tata\s*aia", "cat_insurance_premium", "cat_insurance_premium_term_life_premium"),
    (r"acko|policybazaar|policy\s*bazaar|bajaj\s*allianz|hdfc\s*ergo|icici\s*lombard|reliance\s*general", "cat_insurance_premium", "cat_insurance_premium_vehicle_premium"),

    # ---- FEES ----
    (r"\bsms\s*charge|service\s*charge|bank\s*charge|cc\s*charge|atm\s*charge", "cat_fees_charges", "cat_fees_charges_bank_fees"),
    (r"late\s*payment|late\s*fee|penal\s*interest", "cat_fees_charges", "cat_fees_charges_late_payment"),
    (r"\bgst\b|service\s*tax\b", "cat_fees_charges", "cat_fees_charges_bank_fees"),

    # ---- CASH / ATM ----
    (r"atm\s*wdl|atm\s*withdrawal|cash\s*wdl|\bto\s*atm\b|^to\s*atm[/_-]", "cat_cash_withdrawal", None),

    # ---- Mobile bills via Jio yesbank handle, BBPS aggregator ----
    (r"jio@yesbank|jio\s*postpaid|jio\s*prepaid|jio20br|jio\s*recharge", "cat_bills_utilities", "cat_bills_utilities_mobile"),
    (r"\bbbpsbp@|\bbbps\b|bharat\s*billpay", "cat_bills_utilities", "cat_bills_utilities_mobile"),

    # ---- Add-money into Jupiter from other accounts (TRANSFER-ish but kept as
    #      generic miscellaneous because we don't know the source — user fixes per-row).
    (r"add\s*money|addmoney|axijup", "cat_between_my_accounts", None),
]


def is_self_transfer(description: str) -> bool:
    return bool(SELF_HANDLE_RE.search(description))


def categorise(description: str) -> tuple[str | None, str | None, str | None]:
    """Returns (category_id, sub_category_id_or_none, type_override_or_none).
    The type_override forces TRANSFER / INCOME / CASHBACK / etc.; None means
    the caller's default (EXPENSE for outflows, INCOME for inflows) wins.
    Returns (None, None, None) when no rule matches → caller uses Miscellaneous.
    """
    norm = _normalize_description(description)
    # 1. Self-transfer wins above everything else.
    if SELF_HANDLE_RE.search(norm):
        return "cat_between_my_accounts", None, "TRANSFER"
    # 2. Counterparty handles (wife, dad, CRED, BSE-MF, Deevia, etc.).
    for pattern, parent, sub, type_override in COUNTERPARTY_RULES:
        if re.search(pattern, norm):
            return parent, sub, type_override
    # 3. Keyword rules (merchant/category text).
    for pattern, parent, sub in CATEGORY_RULES:
        if re.search(pattern, norm):
            return parent, sub, None
    return None, None, None


# -----------------------------------------------------------------------------
# Payment-app inference
# -----------------------------------------------------------------------------

def infer_payment_app(description: str) -> str:
    """Returns the PaymentApp enum name string."""
    d = _normalize_description(description)
    if re.search(r"\bupi\b", d):
        if "phonepe" in d or "ppz" in d:
            return "PHONEPE"
        if "gpay" in d or "google\\s*pay" in d or "/gpay/" in d:
            return "GPAY"
        if "paytm" in d:
            return "PAYTM"
        if "cred\\b" in d:
            return "CRED"
        if "bhim" in d:
            return "BHIM"
        return "GPAY"  # most common Indian default
    if re.search(r"\bpos\b|\becom\b|\bswipe\b|visa\s*dr|master\s*dr", d):
        return "CARD_SWIPE"
    if re.search(r"\bneft\b|\bimps\b|\brtgs\b|\bft\b", d):
        return "BANK_APP"
    if re.search(r"\batm\b|cash\s*wdl", d):
        return "CASH"
    if re.search(r"net\s*banking|netbanking|inb\b", d):
        return "NETBANKING"
    return "OTHER"


# -----------------------------------------------------------------------------
# Type override
# -----------------------------------------------------------------------------

def infer_type(raw: RawTxn, type_override: str | None = None) -> str:
    """Returns the TransactionType enum name string. [type_override] from the
    categoriser wins over keyword/direction inference."""
    if type_override:
        return type_override
    if raw.deposit and raw.deposit > 0:
        norm = _normalize_description(raw.description)
        if re.search(r"\binterest\b|sb\s*int", norm):
            return "INTEREST"
        if re.search(r"\brefund\b|rfnd", norm):
            return "REFUND"
        if re.search(r"cash\s*back|cashback", norm):
            return "CASHBACK"
        return "INCOME"
    return "EXPENSE"


# -----------------------------------------------------------------------------
# Description cleaning
# -----------------------------------------------------------------------------

def clean_description(raw: str) -> str:
    """Trim noisy prefixes; cap at ~80 chars. Keep the descriptive bit visible."""
    s = re.sub(r"\s+", " ", raw).strip()
    # Drop common transactional prefixes that carry no info
    s = re.sub(r"^(UPI\s*/|UPI-|NEFT\s*/|NEFT-|IMPS\s*/|IMPS-|RTGS\s*/|POS\s*/|POS-|FT\s*/|ECOM\s*/)", "", s, flags=re.I)
    # Drop bare ref numbers
    s = re.sub(r"^\d{6,}\s*[-/]\s*", "", s)
    if len(s) > 80:
        s = s[:77] + "..."
    return s or raw


# -----------------------------------------------------------------------------
# Build ArthaTxn dicts
# -----------------------------------------------------------------------------

EPOCH = datetime(1970, 1, 1, tzinfo=timezone.utc)

def to_artha(raw: RawTxn, account_id: str) -> dict:
    category_id, sub_id, type_override = categorise(raw.description)
    if category_id is None:
        category_id = "cat_miscellaneous"
        sub_id = "cat_miscellaneous_other"
    type_ = infer_type(raw, type_override)
    payment_app = infer_payment_app(raw.description)
    amount = raw.deposit if (raw.deposit and raw.deposit > 0) else (raw.withdrawal or 0.0)
    # Compose the Transaction row
    midnight_local = datetime(raw.date.year, raw.date.month, raw.date.day, 12, 0, tzinfo=timezone.utc)
    epoch_ms = int(midnight_local.timestamp() * 1000)
    now_ms = int(datetime.now(tz=timezone.utc).timestamp() * 1000)
    return {
        "id": str(uuid.uuid4()),
        "type": type_,
        "amount": amount,
        "currency": "INR",
        "date": epoch_ms,
        "description": clean_description(raw.description),
        "category_id": category_id,
        "sub_category_id": sub_id,
        "source_type": "ACCOUNT",
        "source_id": account_id,
        "destination_type": None,
        "destination_id": None,
        "payment_app": payment_app,
        "place": None,
        "latitude": None,
        "longitude": None,
        "receipt_uri": None,
        "notes": f"Imported from {raw.source_file}",
        "tax_section": None,
        "recurring_rule_id": None,
        "is_split": 0,
        "split_group_id": None,
        "source": "MANUAL",
        "created_at": now_ms,
        "updated_at": now_ms,
    }


# -----------------------------------------------------------------------------
# adb helpers
# -----------------------------------------------------------------------------

def adb(*args: str, check=True) -> subprocess.CompletedProcess:
    return subprocess.run(["adb", *args], capture_output=True, text=True, check=check)


def adb_check_device() -> None:
    out = adb("devices").stdout
    if "\tdevice" not in out:
        raise SystemExit("No connected adb device. Connect your phone and try again.")


def pull_db() -> None:
    LOCAL_DB.unlink(missing_ok=True)
    print(f"Stopping {PACKAGE} so SQLite locks release...")
    adb("shell", "am", "force-stop", PACKAGE, check=False)
    print("Pulling Room DB via run-as (debug build only)...")
    # Use the explicit cat-to-stdout path to avoid permission issues
    proc = subprocess.run(
        ["adb", "exec-out", "run-as", PACKAGE, "cat", "databases/artha.db"],
        capture_output=True, check=True,
    )
    if not proc.stdout:
        raise SystemExit("Got an empty DB blob from adb. Is the app installed and run-as enabled?")
    LOCAL_DB.write_bytes(proc.stdout)
    print(f"  -> wrote {LOCAL_DB.stat().st_size:,} bytes to {LOCAL_DB}")


def push_db() -> None:
    # Need to copy through /sdcard since run-as can't read /sdcard input redirection cleanly.
    remote_tmp = "/sdcard/artha_import.db"
    print(f"Pushing modified DB ({LOCAL_DB.stat().st_size:,} bytes) -> device...")
    adb("push", str(LOCAL_DB), remote_tmp)
    # Move into the package's private storage
    cmd = f"cp {remote_tmp} databases/artha.db && rm {remote_tmp}"
    # The cp needs to be run AS the app (private dir) — run-as gives us that.
    # But run-as can't access /sdcard directly; instead we use Android's `cat` pipe via shell.
    adb("shell", "sh", "-c", f"run-as {PACKAGE} sh -c 'cat /sdcard/artha_import.db > databases/artha.db' && rm /sdcard/artha_import.db")
    print("DB written back into private storage.")


def relaunch() -> None:
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity", check=False)


# -----------------------------------------------------------------------------
# SQLite writer
# -----------------------------------------------------------------------------

def ensure_account(conn: sqlite3.Connection, name: str, opening_balance: float, color_argb: int) -> str:
    cur = conn.execute("SELECT id FROM accounts WHERE name = ? LIMIT 1", (name,))
    row = cur.fetchone()
    if row:
        print(f"  account exists: {name} (id={row[0]})")
        return row[0]
    aid = str(uuid.uuid4())
    now_ms = int(datetime.now(tz=timezone.utc).timestamp() * 1000)
    conn.execute(
        """INSERT INTO accounts
           (id, name, type, institution, account_number_last4, opening_balance, currency,
            icon, color, is_archived, display_order, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        (
            aid, name, "SAVINGS",
            name.split("(")[0].strip(),  # institution = best-guess from name
            None,
            opening_balance,
            "INR",
            "account_balance",
            color_argb,
            0,
            int(datetime.now().timestamp()),
            now_ms,
        ),
    )
    print(f"  CREATED account: {name} (opening Rs.{opening_balance:,.2f})")
    return aid


def insert_transactions(conn: sqlite3.Connection, rows: list[dict]) -> int:
    sql = """INSERT INTO transactions
        (id, type, amount, currency, date, description, category_id, sub_category_id,
         source_type, source_id, destination_type, destination_id, payment_app,
         place, latitude, longitude, receipt_uri, notes, tax_section,
         recurring_rule_id, is_split, split_group_id, source, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
    payload = [(
        r["id"], r["type"], r["amount"], r["currency"], r["date"], r["description"],
        r["category_id"], r["sub_category_id"], r["source_type"], r["source_id"],
        r["destination_type"], r["destination_id"], r["payment_app"],
        r["place"], r["latitude"], r["longitude"], r["receipt_uri"], r["notes"],
        r["tax_section"], r["recurring_rule_id"], r["is_split"], r["split_group_id"],
        r["source"], r["created_at"], r["updated_at"],
    ) for r in rows]
    conn.executemany(sql, payload)
    return len(payload)


# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------

def parse_all() -> tuple[list[RawTxn], dict[str, float | None]]:
    all_txns: list[RawTxn] = []
    openings: dict[str, float | None] = {"federal": None, "icici": None}
    for fname, label in FEDERAL_FILES:
        p = HERE / fname
        if not p.exists():
            print(f"  ! missing: {fname}")
            continue
        txns, opening = parse_federal(p)
        if openings["federal"] is None and opening is not None:
            openings["federal"] = opening
        all_txns.extend(txns)
        print(f"  {label}: {len(txns)} raw txns (opening Rs.{opening or 0:.2f})")
    for fname, label in ICICI_FILES:
        p = HERE / fname
        if not p.exists():
            print(f"  ! missing: {fname}")
            continue
        txns, opening = parse_icici(p)
        if openings["icici"] is None and opening is not None:
            openings["icici"] = opening
        all_txns.extend(txns)
        print(f"  {label}: {len(txns)} raw txns (opening Rs.{opening or 0:.2f})")
    return all_txns, openings


def preview(txns: list[RawTxn], verbose: bool) -> None:
    by_bank = defaultdict(list)
    for t in txns:
        by_bank[t.bank].append(t)
    cat_counts: Counter[str] = Counter()
    type_counts: Counter[str] = Counter()
    app_counts: Counter[str] = Counter()
    uncategorised: list[RawTxn] = []
    for t in txns:
        cat, sub, type_override = categorise(t.description)
        if cat is None:
            cat_counts["cat_miscellaneous"] += 1
            uncategorised.append(t)
        else:
            cat_counts[cat] += 1
        type_counts[infer_type(t, type_override)] += 1
        app_counts[infer_payment_app(t.description)] += 1

    print("\n" + "=" * 80)
    print("SUMMARY")
    print(f"  Unique transactions after dedup: {len(txns)}")
    for bank, ts in by_bank.items():
        ws = sum(t.withdrawal or 0 for t in ts)
        ds = sum(t.deposit or 0 for t in ts)
        print(f"  {bank}: {len(ts)} txns  withdrawals Rs.{ws:,.2f}  deposits Rs.{ds:,.2f}")
    print(f"\nType distribution: {dict(type_counts)}")
    print(f"Payment-app distribution: {dict(app_counts.most_common())}")
    print(f"\nTop 15 categories:")
    for cid, n in cat_counts.most_common(15):
        print(f"  {n:5d}  {cid}")
    if uncategorised:
        print(f"\nUncategorised: {len(uncategorised)} (will land in Miscellaneous > Other)")
        sample = uncategorised[: 25 if not verbose else len(uncategorised)]
        print(f"  Sample {len(sample)} of {len(uncategorised)}:")
        for t in sample:
            amt = t.withdrawal or t.deposit or 0
            print(f"    {t.date}  Rs.{amt:>10,.2f}  {clean_description(t.description)[:80]}")


def apply_to_db(txns: list[RawTxn], openings: dict[str, float | None]) -> None:
    adb_check_device()
    pull_db()
    print(f"\nOpening local DB: {LOCAL_DB}")
    conn = sqlite3.connect(LOCAL_DB)
    try:
        conn.execute("PRAGMA foreign_keys = ON")
        federal_id = ensure_account(conn, FEDERAL_ACCOUNT_NAME, openings.get("federal") or 0.0, color_argb=int(0xFF0F766E - 0x100000000))
        icici_id = ensure_account(conn, ICICI_ACCOUNT_NAME, openings.get("icici") or 0.0, color_argb=int(0xFF4338CA - 0x100000000))

        # Build dicts and bulk-insert
        rows = []
        for t in txns:
            account_id = federal_id if t.bank == "federal" else icici_id
            rows.append(to_artha(t, account_id))
        n = insert_transactions(conn, rows)
        conn.commit()
        print(f"\nInserted {n} transactions across both banks.")
    finally:
        conn.close()

    push_db()
    print("\nRelaunching app...")
    relaunch()
    print("Done. Open the app — you should see the new accounts on Dashboard with full history.")


def export_json(txns: list[RawTxn], openings: dict[str, float | None]) -> Path:
    """Write a JSON payload that the in-app importer reads from APK assets.

    Account IDs are NOT generated here — the app picks a UUID at install time
    (or reuses an existing account matched by name). Transactions reference
    accounts by *name* so the JSON stays portable across installs.

    Each transaction gets a deterministic ID derived from
    (bank, date, amount, normalised description). This means re-running the
    in-app importer is idempotent — the same JSON re-imported is a no-op
    rather than 3,913 duplicates, because we INSERT OR IGNORE by primary key.
    """
    def stable_id(t: RawTxn) -> str:
        seed = "|".join([
            t.bank,
            t.date.isoformat(),
            str(int(round((t.withdrawal or 0) * 100))),
            str(int(round((t.deposit or 0) * 100))),
            _normalize_description(t.description)[:60],
        ])
        # UUID5 in the URL namespace gives a deterministic 36-char id.
        return str(uuid.uuid5(uuid.NAMESPACE_URL, seed))

    accounts = [
        {
            "name": FEDERAL_ACCOUNT_NAME,
            "type": "SAVINGS",
            "institution": "Federal Bank",
            "opening_balance": openings.get("federal") or 0.0,
            "icon": "account_balance",
            # signed-int form matching what AccountEntity.color (Long) stores
            "color": int(0xFF0F766E - 0x100000000),
        },
        {
            "name": ICICI_ACCOUNT_NAME,
            "type": "SAVINGS",
            "institution": "ICICI Bank",
            "opening_balance": openings.get("icici") or 0.0,
            "icon": "account_balance",
            "color": int(0xFF4338CA - 0x100000000),
        },
    ]

    rows = []
    for t in txns:
        cat_id, sub_id, type_override = categorise(t.description)
        if cat_id is None:
            cat_id = "cat_miscellaneous"
            sub_id = "cat_miscellaneous_other"
        type_ = infer_type(t, type_override)
        amount = t.deposit if (t.deposit and t.deposit > 0) else (t.withdrawal or 0.0)
        midnight = datetime(t.date.year, t.date.month, t.date.day, 12, 0, tzinfo=timezone.utc)
        epoch_ms = int(midnight.timestamp() * 1000)
        rows.append({
            "id": stable_id(t),
            "type": type_,
            "amount": amount,
            "currency": "INR",
            "date_ms": epoch_ms,
            "description": clean_description(t.description),
            "category_id": cat_id,
            "sub_category_id": sub_id,
            "source_account_name": FEDERAL_ACCOUNT_NAME if t.bank == "federal" else ICICI_ACCOUNT_NAME,
            "destination_account_name": None,
            "payment_app": infer_payment_app(t.description),
            "notes": f"Imported from {t.source_file}",
        })

    payload = {
        "version": 1,
        "generated_at_ms": int(datetime.now(tz=timezone.utc).timestamp() * 1000),
        "transaction_count": len(rows),
        "accounts": accounts,
        "transactions": rows,
    }

    assets_dir = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "seed"
    assets_dir.mkdir(parents=True, exist_ok=True)
    out = assets_dir / "bank_import.json"
    # Compact JSON — saves ~30% APK size vs pretty-printed for 3.9k rows.
    out.write_text(json.dumps(payload, separators=(",", ":")), encoding="utf-8")
    return out


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--apply", action="store_true", help="Actually write to the device DB via adb.")
    ap.add_argument("--export-json", action="store_true", help="Write bank_import.json into app/src/main/assets/seed/ for in-app import.")
    ap.add_argument("--verbose", action="store_true", help="Print all uncategorised rows, not just samples.")
    args = ap.parse_args()

    print("Parsing bank statements...\n")
    raw_txns, openings = parse_all()
    print(f"\nTotal raw rows: {len(raw_txns)}")
    deduped = dedup(raw_txns)
    print(f"After dedup: {len(deduped)}")

    preview(deduped, verbose=args.verbose)

    print("\n" + "=" * 80)
    if args.apply:
        print("--apply set — writing to device via adb.")
        apply_to_db(deduped, openings)
    elif args.export_json:
        out = export_json(deduped, openings)
        size_kb = out.stat().st_size / 1024
        print(f"--export-json set — wrote {len(deduped)} transactions to:")
        print(f"  {out}")
        print(f"  ({size_kb:.1f} KB)")
        print("\nRebuild the APK to bundle the new asset:")
        print("  ./gradlew assembleDebug")
        print("Then install the APK and tap Settings -> 'Import bundled bank data'.")
    else:
        print("Dry run (no writes). Pass --apply (adb) or --export-json (APK asset).")


if __name__ == "__main__":
    main()
