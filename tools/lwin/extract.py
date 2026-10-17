#!/usr/bin/env python3
"""Converts the Liv-ex LWIN workbook into tools/lwin/lwin.tsv.gz, the source the app's
catalogue pack is built from (see docs/CATALOGUE.md).

    pip install openpyxl
    python3 tools/lwin/extract.py LWINdatabase.xlsx

Keeps live wines (still, sparkling and fortified); drops spirits, beer, cider, sake and
deleted or merged codes. Values are copied as given, except Liv-ex's "NA" which becomes empty.
"""
import gzip, sys, pathlib
import openpyxl

KEEP_TYPES = {"Wine", "Fortified Wine"}
COLUMNS = ["LWIN", "PRODUCER_TITLE", "PRODUCER_NAME", "WINE", "COUNTRY", "REGION", "SUB_REGION",
           "SITE", "PARCEL", "COLOUR", "TYPE", "SUB_TYPE", "DESIGNATION", "CLASSIFICATION",
           "VINTAGE_CONFIG", "FIRST_VINTAGE", "FINAL_VINTAGE"]


def clean(v):
    if v is None:
        return ""
    if isinstance(v, float) and v.is_integer():
        v = int(v)
    s = str(v).strip()
    if s == "NA":
        return ""
    return s.replace("\t", " ").replace("\r", " ").replace("\n", " ")


def main(src, out):
    wb = openpyxl.load_workbook(src, read_only=True)
    rows = wb.active.iter_rows(values_only=True)
    header = list(next(rows))
    idx = {h: header.index(h) for h in COLUMNS + ["STATUS"]}
    kept = []
    for r in rows:
        if r[idx["STATUS"]] != "Live" or r[idx["TYPE"]] not in KEEP_TYPES:
            continue
        kept.append([clean(r[idx[c]]) for c in COLUMNS])
    kept.sort(key=lambda row: int(row[0]))
    with gzip.open(out, "wt", encoding="utf-8", compresslevel=9) as f:
        f.write("\t".join(c.lower() for c in COLUMNS) + "\n")
        for row in kept:
            f.write("\t".join(row) + "\n")
    print(f"{len(kept)} wines -> {out}")


if __name__ == "__main__":
    here = pathlib.Path(__file__).resolve().parent
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else here / "lwin.tsv.gz")
