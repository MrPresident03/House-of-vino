# Implementation checklist

Legend: `[ ]` not started · `[~]` in progress · `[x]` done and tested · `(T)` = automated test exists

A box is only ticked when the feature works end-to-end on real data with tests. A UI
placeholder, sample data or a button without real behaviour stays unticked (spec §9).

## Status (2026-10-17)

CI is green on Linux and macOS:
- **jvm-android**: 129 tests (core, data incl. sync engine, import/export and the 5,000-bottle
  search test, composeApp design tokens and end-to-end UI flows on the desktop JVM); the
  Android debug APK builds.
- **ios**: the same core and data tests on the iOS simulator with the system SQLite (120), and
  the Xcode app builds for the simulator.
- **server-sql**: Supabase schema, RLS, households and the sync RPC on PostgreSQL 16.

`[x]` means covered by those automated tests. Platform features that need a camera, file
pickers, notifications or background execution stay `[~]` until the manual device pass at
the end of this file has been done on real phones.

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
- [x] Supabase key classification; privileged keys refused (T)

## M3 Persistence (`data`)
- [x] SQLDelight schema v1 + migration test (T)
- [x] Repositories: wine/vintage, lot, location, movement (transactional), tasting, wishlist, tag, valuation, photo (T)
- [x] stock_level triggers; ledger rebuild check (T)
- [x] Search: FTS4 index, normalised docs, structured filters, sort, paging, typo fallback, scopes (T)
- [x] Saved searches, recent queries (T)
- [x] 5,000-bottle performance test (search p95 and list paging) (T)

## M4 Backend and sync
- [x] Supabase migrations: tables, rev triggers, advisory lock, RLS, household roles, invites (T: SQL)
- [x] `sync_push` RPC: idempotent op_ids, optimistic concurrency, movement immutability (T: SQL)
- [~] `delete-account` Edge Function + SQL cleanup (SQL part tested; the Deno function is untested)
- [x] Kotlin sync engine: outbox, coalescing, retries/backoff, pull cursors, tombstones, conflicts (T)
- [x] Multi-device and concurrent-stock integration tests against an in-memory fake server (T)
- [~] Supabase remote implementation (auth, PostgREST pull, RPC push, storage upload)

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
- [~] Settings: account, household, currency & rates, preferences, privacy, notifications, backup, delete account

## M7 Scanning and platform
- [~] Android: CameraX + ML Kit barcode and text recognition; permission states
- [~] iOS: AVFoundation barcode + Vision text recognition; permission states
- [x] Open Food Facts barcode lookup (behind interface) + manual fallback everywhere (T)
- [~] Background sync: WorkManager (Android), BGTaskScheduler (iOS)
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
- [ ] Two devices on one account drink the last bottle while offline: stock conflict is flagged
- [ ] Household invite accepted on a second account; viewer can't edit
- [ ] Drinking-window reminder permission prompt and delivery
- [ ] Background sync after leaving the app (WorkManager / BGTaskScheduler)
- [ ] VoiceOver / TalkBack through add wine, drink a bottle and search; Dynamic Type at the largest size
- [ ] Dark and light themes; reduced motion turns off the quantity roll
