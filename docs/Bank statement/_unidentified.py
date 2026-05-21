"""Surface the most-impactful uncategorised UPI handles + merchant patterns
remaining after the current rules. Goal: minimise the user's review work."""
from __future__ import annotations
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from import_to_artha import (  # type: ignore
    FEDERAL_FILES, ICICI_FILES, HERE, parse_federal, parse_icici, dedup,
    categorise, _normalize_description,
)

HANDLE_RE = re.compile(r"[\w.\-]+@\w+", re.IGNORECASE)


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

    # Only look at uncategorised txns
    unc = [t for t in txns if categorise(t.description)[0] is None]
    print(f"Uncategorised: {len(unc)} of {len(txns)} ({100*len(unc)/len(txns):.0f}%)\n")

    # Handle-level rollup
    handle_count: Counter[str] = Counter()
    handle_amount: defaultdict[str, float] = defaultdict(float)
    handle_dir: defaultdict[str, list[int]] = defaultdict(lambda: [0, 0])
    for t in unc:
        # Strip trailing whitespace/digits that crept into the handle text
        for h in HANDLE_RE.findall(t.description):
            hl = h.lower().strip()
            handle_count[hl] += 1
            handle_amount[hl] += (t.deposit or t.withdrawal or 0)
            if t.deposit and t.deposit > 0:
                handle_dir[hl][0] += 1
            else:
                handle_dir[hl][1] += 1

    # Drop one-off handles (count <= 2) to keep the list scannable
    significant = {h: c for h, c in handle_count.items() if c >= 3}

    print("=" * 80)
    print("TOP 25 UNCATEGORISED HANDLES (3+ occurrences), sorted by total rupee impact:")
    print(f"{'count':>5}  {'in':>3}/{'out':<3}  {'sum':>13}  handle")
    items = sorted(significant.items(), key=lambda kv: -handle_amount[kv[0]])
    for h, c in items[:25]:
        ind, outd = handle_dir[h]
        print(f"{c:>5}  {ind:>3}/{outd:<3}  Rs.{handle_amount[h]:>10,.0f}  {h}")

    print("\n" + "=" * 80)
    print("TOP 15 UNCATEGORISED HANDLES, sorted by COUNT:")
    print(f"{'count':>5}  {'sum':>13}  handle")
    for h, c in handle_count.most_common(15):
        if c < 3:
            break
        print(f"{c:>5}  Rs.{handle_amount[h]:>10,.0f}  {h}")

    # Also surface CAPS merchant patterns past the obvious noise
    print("\n" + "=" * 80)
    print("TOP 12 CAPS MERCHANT TOKENS in uncategorised rows (4+ chars):")
    NOISE = {"UPI", "POS", "ECM", "ATM", "NEFT", "IMPS", "RTGS", "PA", "FT", "OUT", "INB",
             "CR", "DR", "PAYMENT", "DEBIT", "CREDIT", "TRANSACTION", "PAY", "AMOUNT",
             "FROM", "TRANSFER", "ECOM", "BANK", "INDI", "INDIA", "ICIC", "ICICI",
             "AXIS", "HDFC", "FEDERAL", "CANARA", "LIMITE", "BHARATPE", "UPIOUT"}
    word_counts: Counter[str] = Counter()
    for t in unc:
        for w in re.findall(r"[A-Z]{4,}", t.description):
            if w not in NOISE:
                word_counts[w] += 1
    for w, n in word_counts.most_common(12):
        print(f"  {n:>5}  {w}")


if __name__ == "__main__":
    main()
