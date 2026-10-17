# Implementation checklist

Legend: `[ ]` not started · `[~]` in progress · `[x]` done and tested · `(T)` = automated test exists

A box is only ticked when the feature works end-to-end on real data with tests. A UI
placeholder, sample data or a button without real behaviour stays unticked (spec §9).

## Status (2026-10-18)

CI is green on Linux and macOS:
- **jvm-android**: 162 tests: core (crypto vectors, QR encoder, merge rules), data (including
  15 household sync scenarios with two and three phones and the 5,000-bottle search test) and
  composeApp (design tokens, critical flows, sharing flows) on the desktop JVM; the Android
  debug APK builds.
- **ios**: the core and data tests on the iOS simulator with the system SQLite, and the Xcode
  app builds for the simulator.

`[x]` means covered by those automated tests. Platform features that need a camera, file
pickers, the share sheet or notifications stay `[~]` until the manual device pass at the end
of this file has been done on real phones.

## M1 Project and CI
- [x] Gradle multi-module KMP build (core, data, composeApp, androidApp) + version catalog
- [x] iOS host app (XcodeGen) consuming the `ComposeApp` framework
- [x] CI: Linux JVM tests + assembleDebug; macOS iOS simulator tests + xcodebuild (opt-in); Postgres server tests; results branch

## M2 Domain core (`core`)
- [x] Money: parse, format, arithmetic, allocation, FX with HALF_EVEN (T)
- [x] Vintage year model (year / NV / unknown), bottle formats (T)
- [x] Movement sign rules, stock projection, transfer pairing, reversal, negative-stock detection (T)
- [x] Text normalisation, tokenisation, abbreviation expansion, Damerau–Levenshtein (T)
- [x] CSV read/write (RFC 4180), column mapping, row validation, duplicate detection (T)
- [x] Drinking-window status, insights maths (T)
- [x] Barcode/label candidate validation (no vintage/format assumed) (T)
- [x] Sync merge rules: hybrid logical clock, field-level last-writer-wins, conflict detection, convergence property test (T)
- [x] XChaCha20-Poly1305, SHA-256, Base64 against published vectors and reference libraries (T)
- [x] QR encoder: bit-identical to a reference encoder, decoded by ZXing (T)

## M3 Persistence (`data`)
- [x] SQLDelight schema v1 + migration test (T)
- [x] Repositories: wine/vintage, lot, location, movement (transactional), tasting, wishlist, tag, valuation, photo (T)
- [x] stock_level triggers; ledger rebuild check (T)
- [x] Search: FTS4 index, normalised docs, structured filters, sort, paging, typo fallback, scopes (T)
- [x] Saved searches, recent queries (T)
- [x] 5,000-bottle performance test (search p95 and list paging) (T)

## M4 Household sync without a server (ADR-13)
- [x] Change journal recorded with every write; one-time journaling of pre-existing rows (T)
- [x] Ingest in clock order with causal gap detection, field merge, conflicts, negative-stock flags (T)
- [x] Encrypted, text-armoured sync files; household key, invites, safety code (T)
- [x] Join with merge of the joining phone's cellar (re-keying) (T)
- [x] Remove partner (key rotation), stop sharing, erase phone with new device identity (T)
- [x] Two- and three-phone convergence under random edits, lost and duplicated files (T)
- [ ] Phone-to-phone sync on home Wi-Fi
- [ ] Shared Google Drive folder transport
- [ ] Journal compaction

> The Supabase backend built earlier (server schema, RLS, sync RPC, accounts) was removed on
> 2026-10-18 so the app costs nothing to run. Its tests passed; the code is in git history.

## M5 Core UI
- [x] Design system tokens, fonts, components, contrast test (T)
- [~] Cellar list/grid with paging, sort, quick filters, all UI states
- [~] Vintage detail; wine/vintage editor with validation
- [~] Add purchase (lot); consume/sell/gift/loss/adjust; transfer; undo via reversal (T: UI) — logic tested; screen only partly covered by UI tests
- [~] Locations tree, grid slots
- [~] Offline banner, sync status

## M6 Feature screens
- [~] Search UI: scopes, filters sheet, saved searches, recent queries, grid/list (T: UI) — logic tested; screen only partly covered by UI tests
- [~] Wishlist: CRUD, merchant links, status, convert to inventory (T) — logic tested; screen only partly covered by UI tests
- [~] Tasting journal: structured + free notes, rating, photos, occasion, pairing (T) — logic tested; screen only partly covered by UI tests
- [~] Insights: totals, cost, value with provenance, windows, consumption, breakdowns (T) — logic tested; screen only partly covered by UI tests
- [~] CSV import flow (pick, map, validate, duplicates, commit, report) and exports (T) — logic tested; screen only partly covered by UI tests
- [~] Settings: sharing, currency & rates, preferences, privacy, notifications, backup, erase this phone
- [~] Sharing screens: share, invite QR, join by scan or paste, send/receive file, phones, conflicts (T: UI)

## M7 Scanning and platform
- [~] Android: CameraX + ML Kit barcode and text recognition; permission states
- [~] iOS: AVFoundation barcode + Vision text recognition; permission states
- [x] Open Food Facts barcode lookup (behind interface) + manual fallback everywhere (T)
- [~] Drinking-window notifications (local)

## Release gates (spec §10)
- [ ] Add/edit wines manually
- [ ] Quantities, purchases, batches, locations
- [ ] Consumption and transfers without corruption
- [ ] Search/filter large collections
- [ ] Wishlist and tasting history
- [ ] Import/export
- [ ] Offline + reliable sync
- [ ] Private collections + household permissions
- [ ] Runs on iOS and Android, including real-device pass (manual)

## Manual device pass (before each release, on a real iPhone and a real Android phone)
- [ ] Camera permission: first request, denial, "open settings" path, re-grant
- [ ] Barcode scan of a real wine bottle; unknown barcode falls back to manual entry
- [ ] Label capture and OCR on a real label; every suggested field editable before saving
- [ ] CSV import from the Files app / document picker; export opens the share sheet
- [ ] Photo from the library attached to a tasting note; shows after app restart
- [ ] Airplane mode: add, drink and move bottles; reconnect; changes sync once, no duplicates
- [ ] Pair two real phones (iPhone ↔ Android) by QR; both show the same safety code
- [ ] Send a .hovsync file through Messages/WhatsApp/email and receive it on the other phone
- [ ] Both phones drink the last bottle before swapping files: stock conflict is flagged
- [ ] Drinking-window reminder permission prompt and delivery
- [ ] VoiceOver / TalkBack through add wine, drink a bottle and search; Dynamic Type at the largest size
- [ ] Dark and light themes; reduced motion turns off the quantity roll
