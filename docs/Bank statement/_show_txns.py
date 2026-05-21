"""Show full uncategorised transactions for given handle substrings, so the
user can identify who they are."""
from __future__ import annotations
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from import_to_artha import (
    FEDERAL_FILES, ICICI_FILES, HERE, parse_federal, parse_icici, dedup,
)

QUERIES = ["udaykumardoni", "mayurgangadhre", "9108320737", "9740095334"]


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

    for q in QUERIES:
        hits = [t for t in txns if q.lower() in t.description.lower()]
        print(f"\n{'='*80}\n{q}  ({len(hits)} txns)")
        for t in hits:
            amt = t.withdrawal or t.deposit or 0
            dir_ = "OUT" if t.withdrawal else "IN "
            print(f"  {t.date} {dir_} Rs.{amt:>10,.0f}  {t.description}")


if __name__ == "__main__":
    main()
