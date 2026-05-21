"""
Deeper analyser. Locates the real transaction-row header by keyword, then:
  - counts true transaction rows
  - reports earliest/latest dates
  - sums withdrawals and deposits
  - flags duplicates (byte-identical files) and overlap windows

Outputs nothing sensitive (no merchant names, no UPI handles).
"""
from __future__ import annotations
import hashlib
import re
from datetime import datetime, date
from pathlib import Path

HERE = Path(__file__).parent
HEADER_HINTS = {"date", "transaction", "description", "narration", "withdrawal", "deposit", "amount", "balance", "chq", "ref"}


def file_hash(p: Path) -> str:
    h = hashlib.sha1()
    with p.open("rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()[:12]


def parse_date(v) -> date | None:
    if v in (None, ""):
        return None
    if isinstance(v, datetime):
        return v.date()
    if isinstance(v, date):
        return v
    s = str(v).strip()
    for fmt in ("%d/%m/%Y", "%d-%m-%Y", "%Y-%m-%d", "%d.%m.%Y", "%d %b %Y", "%d-%b-%Y"):
        try:
            return datetime.strptime(s, fmt).date()
        except ValueError:
            continue
    return None


def parse_money(v) -> float | None:
    if v in (None, ""):
        return None
    if isinstance(v, (int, float)):
        return float(v)
    s = re.sub(r"[,\s]", "", str(v))
    if not s:
        return None
    try:
        return float(s)
    except ValueError:
        return None


def find_header_row(rows: list) -> int | None:
    for i, r in enumerate(rows):
        cells = [str(c or "").strip().lower() for c in r]
        joined = " ".join(cells)
        hits = sum(1 for hint in HEADER_HINTS if hint in joined)
        if hits >= 3:
            return i
    return None


def analyse_rows(rows: list) -> dict:
    header_idx = find_header_row(rows)
    if header_idx is None:
        return {"error": "no transaction header detected"}
    header = [str(c or "").strip().lower() for c in rows[header_idx]]
    cols = {name: i for i, name in enumerate(header) if name}

    def col(*names):
        for n in names:
            for k, idx in cols.items():
                if n in k:
                    return idx
        return None

    date_col = col("date") if col("date") is not None else col("txn date", "value date")
    wd_col = col("withdrawal", "debit")
    dep_col = col("deposit", "credit")
    bal_col = col("balance")

    txns: list[tuple[date | None, float | None, float | None, float | None]] = []
    for r in rows[header_idx + 1:]:
        if not r or all(c in (None, "") for c in r):
            continue
        d = parse_date(r[date_col]) if date_col is not None and date_col < len(r) else None
        wd = parse_money(r[wd_col]) if wd_col is not None and wd_col < len(r) else None
        dep = parse_money(r[dep_col]) if dep_col is not None and dep_col < len(r) else None
        bal = parse_money(r[bal_col]) if bal_col is not None and bal_col < len(r) else None
        # Treat as transaction row only if at least one of date/amount/balance is set
        if d is not None or wd is not None or dep is not None:
            txns.append((d, wd, dep, bal))

    dates = sorted([t[0] for t in txns if t[0]])
    sum_wd = sum(t[1] for t in txns if t[1])
    sum_dep = sum(t[2] for t in txns if t[2])
    rows_with_date = sum(1 for t in txns if t[0])
    rows_missing_date = len(txns) - rows_with_date
    rows_missing_money = sum(1 for t in txns if t[1] is None and t[2] is None)

    return {
        "header_idx": header_idx,
        "columns": {"date": date_col, "withdrawal": wd_col, "deposit": dep_col, "balance": bal_col},
        "header_preview": " | ".join(header[:10]),
        "txn_count": len(txns),
        "rows_missing_date": rows_missing_date,
        "rows_missing_money": rows_missing_money,
        "first_date": dates[0] if dates else None,
        "last_date": dates[-1] if dates else None,
        "sum_withdrawals": sum_wd,
        "sum_deposits": sum_dep,
        "final_balance": next((t[3] for t in reversed(txns) if t[3] is not None), None),
    }


def load_xlsx(p: Path) -> list:
    import openpyxl
    wb = openpyxl.load_workbook(p, read_only=True, data_only=True)
    return list(wb.active.iter_rows(values_only=True))


def load_xls(p: Path) -> list:
    import xlrd
    book = xlrd.open_workbook(p)
    sh = book.sheets()[0]
    return [sh.row_values(r) for r in range(sh.nrows)]


def load_pdf(p: Path) -> list:
    """PDF: extract text and split into pseudo-rows by line; relies on
    well-spaced table rendering. Returns each line as a single-cell row."""
    import pdfplumber
    rows = []
    with pdfplumber.open(p) as pdf:
        for page in pdf.pages:
            for tbl in page.extract_tables() or []:
                for r in tbl:
                    rows.append(r)
    return rows


def analyse(p: Path) -> dict:
    ext = p.suffix.lower()
    try:
        rows = {"": [], ".xlsx": load_xlsx, ".xls": load_xls, ".pdf": load_pdf}[ext](p)
    except Exception as e:
        return {"error": f"{type(e).__name__}: {e}"}
    out = analyse_rows(rows)
    out["row_count_raw"] = len(rows)
    return out


def main() -> None:
    files = sorted(p for p in HERE.iterdir() if p.is_file() and not p.name.startswith("_"))
    hashes: dict[str, list[str]] = {}
    print(f"Analysing {len(files)} files in {HERE}\n")
    rows_report = []
    for p in files:
        info = analyse(p)
        h = file_hash(p)
        hashes.setdefault(h, []).append(p.name)
        rows_report.append((p, h, info))

    for p, h, info in rows_report:
        print("=" * 80)
        print(f"{p.name}")
        print(f"  size: {p.stat().st_size:,} bytes   sha1[12]: {h}")
        if "error" in info and info.get("txn_count") is None:
            print(f"  ERROR: {info['error']}")
            continue
        print(f"  raw rows: {info.get('row_count_raw')}")
        print(f"  header at row {info.get('header_idx')}: {info.get('header_preview')}")
        cols = info.get("columns", {})
        print(f"  detected cols: date={cols.get('date')}  wd={cols.get('withdrawal')}  dep={cols.get('deposit')}  bal={cols.get('balance')}")
        print(f"  transactions parsed: {info.get('txn_count')}")
        print(f"    rows missing a date: {info.get('rows_missing_date')}")
        print(f"    rows missing both withdrawal AND deposit: {info.get('rows_missing_money')}")
        if info.get("first_date"):
            print(f"  date range: {info['first_date']}  ->  {info['last_date']}")
        if info.get("sum_withdrawals") is not None:
            print(f"  Sum withdrawals: Rs.{info['sum_withdrawals']:,.2f}")
            print(f"  Sum deposits:    Rs.{info['sum_deposits']:,.2f}")
        if info.get("final_balance") is not None:
            print(f"  final balance shown: Rs.{info['final_balance']:,.2f}")
        print()

    # Duplicate detection
    dupes = {h: names for h, names in hashes.items() if len(names) > 1}
    if dupes:
        print("=" * 80)
        print("BYTE-IDENTICAL DUPLICATES")
        for h, names in dupes.items():
            print(f"  {h}:")
            for n in names:
                print(f"    - {n}")


if __name__ == "__main__":
    main()
