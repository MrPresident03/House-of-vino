# Architecture Decision Record — House of Vino

Status: accepted · Date: 2026-10-17 · Scope: initial production architecture

Each decision lists the choice, the reason, and the consequence. Decisions that
affect **privacy**, **recurring cost**, **data licensing** or **irreversible data
changes** are marked ⚑.

## ADR-01 Platform and toolchain

- **Kotlin Multiplatform + Compose Multiplatform** for shared logic and UI on Android and iOS.
- Versions (verified against upstream release tags on 2026-10-17):

| Component | Version | Note |
|---|---|---|
| Kotlin | 2.4.21 | latest stable |
| Compose Multiplatform | 1.12.2 | Material3 artifact `1.12.0-alpha03` is the version JetBrains pairs with 1.12.2 |
| Android Gradle Plugin | 9.3.1 | 9.4.0 exists, but Kotlin 2.4.21's documented AGP range ends at 9.3.1 |
| Gradle | 9.7.1 | Kotlin 2.4.21's documented Gradle range ends at 9.7.x |
| SQLDelight | 2.4.1 | |
| supabase-kt | 3.8.0 | built against Ktor 3.5.1, coroutines 1.11.0 |
| Ktor | 3.5.1 | matches supabase-kt |
| kotlinx-coroutines / serialization / datetime | 1.11.0 / 1.11.0 / 0.8.0 | |
| Navigation (JetBrains) | 2.9.2 | latest stable; 2.10 is still RC |
| Lifecycle (JetBrains) | 2.11.0 | |
| Coil | 3.6.3 | image loading |
| CameraX / ML Kit barcode / ML Kit text | 1.6.2 / 17.3.0 / 16.0.1 | Android only, on-device |
| WorkManager | 2.12.0 | Android background sync |

- **Android minSdk 30 (Android 11)**. Android 11's platform SQLite (3.28) is the first with
  UPSERT and window functions. Supporting API 26–29 would need a bundled SQLite build.
  Trade-off: devices older than Android 11 are excluded.
- **iOS deployment target 16.0** (needed for VisionKit `DataScannerViewController`).
- The iOS Xcode project is generated with **XcodeGen** from `iosApp/project.yml`. This keeps
  `project.pbxproj` out of review diffs. Developers need `brew install xcodegen`.

## ADR-02 Module structure

```
core/        pure Kotlin, stdlib only — domain model, money, inventory rules, text
             normalisation, fuzzy matching, CSV, search query model, insights maths
data/        SQLDelight database, migrations, repositories, search engine, sync engine,
             Supabase remote, photo storage
composeApp/  Compose UI: design system, screens, view models, navigation, platform
             service interfaces; produces the iOS framework `ComposeApp`
androidApp/  Android application shell, WorkManager, CameraX + ML Kit, FileProvider
iosApp/      SwiftUI host, BGTaskScheduler registration
supabase/    SQL migrations, RLS policies, RPC functions, Edge Function, SQL tests
```

- `core` has **no third-party dependencies**. Domain rules can be tested quickly on any
  JVM, and the domain layer can't grow framework coupling.
- Every KMP module also declares a `jvm()` target. Its only purpose is fast CI tests: DB
  tests on the JDBC SQLite driver, and Compose UI tests on desktop. The JVM target is not shipped.
- Dependency injection is a hand-written `AppGraph` (constructor injection). There is no DI
  framework, so dependencies stay explicit and test fakes are trivial.

## ADR-03 Data model: identity vs inventory vs acquisition

- `wine` = producer + cuvée identity (vintage-independent).
- `vintage` = one vintage of a wine. The year is a sealed type: `Year(n)`, `NonVintage` or `Unknown`.
- `lot` = one acquisition batch: vintage, bottle format, date, source, unit price, packaging.
- `movement` = **append-only inventory ledger**. Each row is a signed bottle delta for
  one lot at one location.
- `stock_level` = a cache of `SUM(delta)` per (lot, location), maintained by SQLite triggers
  inside the same transaction as the movement insert. It can always be rebuilt from the ledger.
- Tastings, valuations and wishlist items reference `vintage`, never `lot` or stock.
  Consuming or selling bottles therefore can't remove tasting history. Deletes are soft
  (tombstones), and foreign keys are `ON DELETE RESTRICT`.
- Vintages are never merged automatically. Duplicate detection only *proposes* matches.
  A merge needs explicit confirmation and is itself recorded.

## ADR-04 Money

- `Money(minor: Long, currency)` uses ISO-4217 minor units. Parsing and formatting work on
  strings and longs. **No floating point anywhere in money paths.**
- Exchange rates are scaled integers (10⁻⁸) with HALF_EVEN rounding. Converted totals are
  shown only when the user has supplied a rate, and the rate's source and date are shown with
  the total. The app never invents FX rates.
- Every valuation records `source`, `currency` and `as_of` date, all required.

## ADR-05 Local persistence and search

- SQLDelight with schema `.sq` files and numbered `.sqm` migrations. A migration test runs
  every migration from v1 and compares the result against a fresh schema.
- **Full-text search uses SQLite FTS4, not FTS5.** Android's platform SQLite does not
  reliably ship FTS5. FTS4 is available on Android, iOS and the JVM test driver.
- Text is **normalised in Kotlin** before indexing and querying: case and diacritics are
  folded, ligatures expanded, apostrophes and hyphens split, and "st"/"ste" expanded. This
  makes matching identical on every platform, independent of tokenizer builds.
- Each query token becomes a prefix match (`tok*`).
- **Typo tolerance**: if a token has no hits, candidate terms come from the `fts4aux`
  vocabulary and are ranked by Damerau–Levenshtein distance (≤1 for ≤5 chars, ≤2 otherwise).
  The query is retried and the UI shows "Showing results for …".
- The search index is **derived data**. It is maintained by `SearchIndexer` in the same
  transaction as each write, versioned, and rebuilt automatically when the version changes.
- Structured filters and sort are compiled to parameterised SQL by `SearchSqlBuilder` and run
  with `LIMIT`/`OFFSET` paging. The full collection is never loaded into memory.

## ADR-06 Sync ⚑ privacy

- Local-first. Every write commits locally with an outbox entry in the **same transaction**.
- Outbox entries carry a client `op_id` (UUID) and the row's `base_rev`. The server records
  applied `op_id`s, so retrying a push is idempotent.
- The server assigns a monotonically increasing `rev` with a trigger. The trigger first takes
  a transaction-scoped advisory lock per collection, so revs within one collection commit in
  order. Pull cursors are kept per (collection, table), which closes the "late commit, skipped
  row" hole.
- **Conflict detection**: mutable entities use optimistic concurrency (`base_rev` must equal
  the current `rev`). On a mismatch the server's row wins locally, the local version is kept
  in `sync_conflict`, and the user resolves it (keep mine / keep theirs).
- **Stock changes never conflict**: movements are insert-only, and the server forbids
  UPDATE/DELETE on them in RLS. If merged stock goes negative (two devices drank the last
  bottle), it is flagged as a stock conflict for the user to fix with an adjustment.
- Deletes are tombstones (`deleted_at`) and propagate like any other change.
- Retries use exponential backoff with jitter (cap 15 min). The queue survives app restarts
  because it is a table.

## ADR-07 Backend: Supabase ⚑ privacy ⚑ cost

- Postgres + Auth + Storage. A collection is owned by one user. A **household** is a
  collection with additional members (`owner` / `editor` / `viewer`).
- **Row-level security on every table.** Read: member of the row's collection. Write:
  owner/editor. Movements: insert-only. Membership changes: owner only. Invites are accepted
  through a `SECURITY DEFINER` RPC that checks the token hash, expiry and email.
- The app embeds only the Supabase URL and the **anon (publishable) key**. These are
  public by design. The service-role key exists only inside the `delete-account` Edge Function.
- Storage bucket `photos` is private. Object paths are `{collection_id}/{photo_id}.jpg`, and
  storage RLS uses the same membership check as the tables.
- ⚑ cost: the Supabase Free plan pauses after 1 week of inactivity and has DB/storage
  quotas. Production should use Pro (~US$25/month per project at time of writing; verify
  current pricing).

## ADR-08 Account deletion ⚑ irreversible

- Settings → Delete account requires typing a confirmation phrase. The client offers an
  export first.
- The server then deletes every collection the user owns (cascading all rows), removes their
  memberships, and deletes their storage objects and the auth user. This runs in the
  `delete-account` Edge Function with the service role.
- Households owned by the user are deleted, not transferred. The UI warns members of shared
  collections. Transferring ownership first is a separate, explicit action.
- Local data is wiped after the server confirms. If offline, deletion is refused rather than queued.

## ADR-09 External data ⚑ licensing ⚑ cost

All external data sits behind replaceable interfaces in `data/integrations`:
`BarcodeLookupService`, `WineCatalogService`, `PriceService`, `LabelTextRecognizer`.

- **Barcode lookup**: Open Food Facts (open data, **ODbL** — attribution required, and
  share-alike applies to derived databases). Free with fair-use rate limits. Results are
  treated as *candidates only*: a barcode is never assumed to identify a vintage or format,
  and producer, vintage and format are validated before matching.
- **OCR and barcode decoding**: on-device only (bundled ML Kit models on Android, Apple
  Vision/AVFoundation on iOS). Label photos don't leave the device for recognition. Free.
  ⚑ privacy: Google documents that ML Kit may contact Google for model/compatibility updates
  and sends API usage and performance metrics (no images or recognised text). The Privacy
  screen says so on Android, and it must be declared in the Play Data safety form.
- **Wine catalogue / prices**: there is no licence-clean free source. Vivino and Wine-Searcher
  have no open APIs, and scraping violates their terms. Initial release ships the interfaces
  plus `Unavailable` implementations that route to the manual workflow. The app never
  fabricates metadata, ratings, drinking windows or prices.
- "External discovery" search scope = Open Food Facts product search, labelled as such.

## ADR-10 Verification

- Local workspace: pure-Kotlin `core` tests run with the standalone compiler, and the
  Supabase SQL plus RLS tests run on PostgreSQL 16 with a stub `auth` schema.
- CI (GitHub Actions, private repo ⚑ cost: uses the account's Actions minutes; macOS minutes
  bill at a multiplier):
  - Linux: `./gradlew check` (core/data/composeApp JVM tests, Android unit tests, lint) +
    `assembleDebug`.
  - macOS: `iosSimulatorArm64Test` (runs the DB tests on the native SQLite driver) + XcodeGen +
    `xcodebuild` for the simulator.
  - A summary of every run is committed to the `ci-results` branch for review.
- Real-device testing (spec §8) needs physical hardware and is listed as a manual release step.

## ADR-11 Device backups ⚑ privacy

- Android `allowBackup=false` and data-extraction rules exclude the database and photos from
  Google cloud backup and device-to-device transfer. iOS keeps the database in Application
  Support, which iCloud device backups include by default (the user controls iCloud Backup).
- Backup is therefore an explicit choice: sign in (Supabase sync) or export CSV. A user who
  does neither and loses their Android phone loses the cellar. The first-run screen says an
  account is what backs up; Settings → "Backup and export" offers the CSV export, and on
  Android also says that phone backups don't include the cellar.
- Reason: the cellar holds purchase prices and values; copying it into a backup service the
  user never chose is a privacy decision we shouldn't make for them on Android, where backup
  is opt-out. Revisit if users ask for it.

## ADR-12 Secrets in builds ⚑ privacy

- Supabase URL and anon key come from environment variables (CI) or `local.properties` /
  `iosApp/Configuration/Secrets.xcconfig` (developers). Neither is committed.
- `core.security.KeyGuard` classifies keys (new `sb_publishable_`/`sb_secret_` keys and
  legacy JWTs, by decoding the JWT payload's `role`). The Android Gradle build, the Xcode
  pre-build script and `BackendConfig` all refuse anything that isn't publishable.
