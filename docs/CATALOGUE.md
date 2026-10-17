# Wine catalogue (LWIN)

The app ships a copy of the LWIN database (Liv-ex, CC BY 4.0) so a scanned label can fill in
the wine's details on the phone, with no network and no cost. Decision and trade-offs: ADR-14.

## Pipeline

```
LWINdatabase.xlsx ──tools/lwin/extract.py──▶ tools/lwin/lwin.tsv.gz   (committed, ~3.5 MB)
                   ──:data:buildCataloguePack──▶ data/build/catalogue/lwin.db
                   ──▶ Android assets / iOS app bundle
```

1. `extract.py` keeps `STATUS = Live` rows whose `TYPE` is Wine or Fortified Wine, turns
   Liv-ex's `NA` into empty, and writes a sorted, gzipped TSV. It needs `openpyxl`.
2. `:data:buildCataloguePack` (JVM) builds the SQLite pack with the app's own
   `TextNormalizer`, so indexing and matching can never drift apart. Android's `preBuild` and
   the Xcode pre-build step both run it.
3. On first use each phone copies the pack out of the app (SQLite can't read inside an APK
   or the read-only iOS bundle) and opens it read-only in spirit: nothing is ever written.

## Pack layout (`PRAGMA user_version = 1`)

| Table | Contents |
|---|---|
| `wine` | One row per LWIN code: producer title and name, wine, country, region, sub-region, site, parcel, colour, type, sub-type, designation, classification, first and final vintage. |
| `wine_fts` | Contentless FTS4 index (`docid` = LWIN) over normalised producer, wine and place text. |
| `meta` | Source and row count. |

## Matching

`LwinCatalogue.match(text, year)`:

1. Distinctive words of the scanned text (not "chateau", "domaine", "de", colours…) are
   looked up in the producer index.
2. A wine is a candidate only if **every** distinctive word of its producer is in the text.
3. Candidates are ranked by how much of the cuvée name, then site and classification, also
   appear. Long words tolerate one OCR slip.
4. A vintage outside LWIN's first/final vintage lowers the score and shows a warning.
5. The top match is MEDIUM confidence and applied automatically only if the cuvée fully
   matched and no other candidate is close. Otherwise matches are listed for the user to pick.

## Refreshing the data

1. Download the workbook from https://liv-ex.com/lwin/get-lwin-database and check the
   licence is still CC BY 4.0.
2. `pip install openpyxl && python3 tools/lwin/extract.py ~/Downloads/LWINdatabase.xlsx`
3. Commit `tools/lwin/lwin.tsv.gz`. The next build ships it.
