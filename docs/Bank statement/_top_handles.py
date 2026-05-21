"""Surface the most-used UPI handles + the most-used merchant patterns
across all bank statements, so the user can tell us who/what each one is."""
from __future__ import annotations
import re
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from import_to_artha import (  # type: ignore
    FEDERAL_FILES, ICICI_FILES, HERE, parse_federal, parse_icici, dedup, _normalize_description,
)

UPI_HANDLE_RE = re.compile(r"\b[\w.\-]+@\w+\b")
NUMERIC_PHONE_RE = re.compile(r"\b[6-9]\d{9}@\w+\b")

def main() -> None:
    raw = []
    for fname, _ in FEDERAL_FILES:
        p = HERE / fname
        if p.exists():
            t, _ = parse_federal(p); raw.extend(t)
    for fname, _ in ICICI_FILES:
        p = HERE / fname
        if p.exists():
            t, _ = parse_icici(p); raw.extend(t)
    txns = dedup(raw)
    print(f"Examined {len(txns)} unique transactions\n")

    handle_counts: Counter[str] = Counter()
    handle_amounts: dict[str, float] = {}
    handle_directions: dict[str, list[int]] = {}  # [in_count, out_count]
    for t in txns:
        for m in UPI_HANDLE_RE.findall(t.description):
            h = m.lower()
            handle_counts[h] += 1
            amt = t.deposit or t.withdrawal or 0
            handle_amounts[h] = handle_amounts.get(h, 0) + amt
            d = handle_directions.setdefault(h, [0, 0])
            if t.deposit and t.deposit > 0:
                d[0] += 1
            else:
                d[1] += 1

    print("TOP 30 RECURRING UPI HANDLES (sorted by count):")
    print(f"{'count':>6}  {'in':>4}/{'out':<4}  {'sum':>14}  handle")
    for h, n in handle_counts.most_common(30):
        ind, outd = handle_directions[h]
        amt = handle_amounts[h]
        print(f"{n:>6}  {ind:>4}/{outd:<4}  Rs.{amt:>10,.0f}  {h}")

    # Also surface the top non-UPI keywords (caps in particulars after stripping)
    print("\nTOP 20 NON-UPI MERCHANT PATTERNS (CAPS words 4+ chars, excluding the obvious prefixes):")
    NOISE = {"UPI", "POS", "ECM", "ATM", "NEFT", "IMPS", "RTGS", "PA", "FT", "OUT", "INB", "CR", "DR", "PAYMENT", "DEBIT", "CREDIT", "TRANSACTION", "PAY", "AMOUNT", "FROM", "TRANSFER", "ECOM"}
    word_counts: Counter[str] = Counter()
    for t in txns:
        words = re.findall(r"[A-Z]{4,}", t.description)
        for w in words:
            if w not in NOISE:
                word_counts[w] += 1
    for w, n in word_counts.most_common(20):
        print(f"  {n:>5}  {w}")


if __name__ == "__main__":
    main()
