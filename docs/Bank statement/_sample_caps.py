"""Print 3 sample uncategorised rows for each interesting CAPS token, so we can
see the actual description format and tighten the rules."""
from __future__ import annotations
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from import_to_artha import (
    FEDERAL_FILES, ICICI_FILES, HERE, parse_federal, parse_icici, dedup,
    categorise,
)

TARGETS = ["CRED", "AIRTEL", "SUBRAMANYA", "KARNATAKA", "DEUT", "ACDR",
           "SHRI", "RAGHA", "GAJENDRA", "EATCLUB"]


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
    unc = [t for t in txns if categorise(t.description)[0] is None]

    for tok in TARGETS:
        hits = [t for t in unc if tok in t.description.upper()]
        print(f"\n{'='*80}\n{tok}  ({len(hits)} uncategorised rows)")
        for t in hits[:3]:
            amt = t.withdrawal or t.deposit or 0
            dir_ = "OUT" if t.withdrawal else "IN "
            print(f"  {t.date} {dir_} Rs.{amt:>10,.0f}  {t.description[:120]}")


if __name__ == "__main__":
    main()
