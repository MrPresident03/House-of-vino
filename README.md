# House of Vino

A cellar book for serious wine collections, for iOS and Android: inventory with purchase
batches and locations, an append-only stock ledger, search that stays fast at thousands of
bottles, a wishlist, a tasting journal, insights, CSV import/export and on-device label and
barcode scanning. It works fully offline. An optional Supabase backend adds sync, backup and
household sharing.

Built with Kotlin Multiplatform and Compose Multiplatform, SQLDelight (SQLite) on the device,
and Supabase (PostgreSQL, Auth, Storage) on the server.

Design decisions, with every choice that affects **privacy, recurring cost, data licensing or
irreversible data changes** marked ⚑, are in [`docs/ADR.md`](docs/ADR.md). Also in `docs/`:
the database schema, navigation map, design system and the implementation checklist, which
records what is done and tested.

## Repository layout

| Path | What it is |
|---|---|
| `core/` | Pure Kotlin domain logic, no dependencies: money (integer minor units, HALF_EVEN FX), vintages and formats, the stock ledger, text normalisation and fuzzy matching, the CSV reader/writer, import mapping and duplicate detection, insights maths, scan candidate matching, API-key classification |
| `data/` | SQLDelight schema and migrations, repositories, FTS search engine, import/export, outbox-based sync engine, Supabase client, Open Food Facts lookup |
| `composeApp/` | Shared Compose UI (all screens), design system, platform contract, camera scanners (CameraX + ML Kit on Android, AVFoundation + Vision on iOS), iOS platform services |
| `androidApp/` | Android application: activity, platform services, WorkManager sync and reminders, manifest, FileProvider |
| `iosApp/` | iOS host app: XcodeGen spec, SwiftUI entry point, Info.plist settings, asset catalog |
| `supabase/` | Postgres migrations (tables, RLS, revision triggers, sync RPC, households), SQL contract tests, the `delete-account` Edge Function |
| `.github/` | CI workflow and the script that publishes results to the `ci-results` branch |

## Prerequisites

- JDK 21 (17 or newer works). Gradle is provided by the wrapper (`./gradlew`, Gradle 9.7.1).
- Android: Android Studio with SDK platform 36. Minimum Android version is 11 (API 30).
- iOS: a Mac with Xcode 16 or newer and [XcodeGen](https://github.com/yonaskolb/XcodeGen)
  (`brew install xcodegen`). Minimum iOS version is 16.
- Backend (optional): the [Supabase CLI](https://supabase.com/docs/guides/cli) and a Supabase
  project. For running the server tests locally, PostgreSQL 16 and `psql`.

## Configuration

The app runs without any configuration in **local-only mode**: everything is stored on the
device and sync/account screens explain that no server is configured. To enable sync, supply
two values.

| Variable | Used by | Where to set it |
|---|---|---|
| `SUPABASE_URL` | Android, iOS | Android: environment variable or `local.properties`. iOS: `iosApp/Configuration/Secrets.xcconfig` (write `https:/$()/…`, because `//` starts a comment in xcconfig). CI: repository secret |
| `SUPABASE_ANON_KEY` | Android, iOS | Same places. **Publishable (anon) key only** |
| `HV_DEVELOPMENT_TEAM` | iOS device builds | `Secrets.xcconfig` (your Apple team ID) |
| `SUPABASE_SERVICE_ROLE_KEY` | `delete-account` Edge Function only | Provided automatically by Supabase to Edge Functions. Never put it anywhere else |

Templates: `local.properties.example` and `iosApp/Configuration/Secrets.xcconfig.example`.
Both real files are git-ignored.

**Privileged keys are refused.** `core.security.KeyGuard` recognises Supabase's
`sb_publishable_…` / `sb_secret_…` keys and decodes legacy JWT keys to read their role. The
Android Gradle build, the Xcode pre-build script (`iosApp/scripts/check-supabase-key.sh`) and
the app itself all reject a secret or service-role key, so one can't end up in a build by
mistake.

## Backend setup (Supabase)

1. Create a project at [supabase.com](https://supabase.com) and note its URL and publishable
   (anon) key from *Project Settings → API*.
2. Link and apply the migrations:
   ```sh
   supabase login
   supabase link --project-ref <your-project-ref>
   supabase db push            # applies supabase/migrations/*.sql in order
   ```
   The migrations create every table with row-level security, the revision triggers, the
   `sync_push` RPC, household/invite functions and the private `photos` storage bucket.
3. Deploy the account-deletion function:
   ```sh
   supabase functions deploy delete-account
   ```
4. In *Authentication*, keep the Email provider enabled. The app signs in with a password or
   with a one-time code typed into the app, so edit the *Magic Link* email template to include
   the code (`{{ .Token }}`); the default template only contains a link. The app doesn't
   handle deep links in this release, so set the *Site URL* to a simple web page of yours
   ("Email confirmed, return to the app") for sign-up confirmation links to land on.
5. Put the URL and publishable key into your Android/iOS configuration (above).

New migrations go in `supabase/migrations/` with a timestamp prefix, and never edit one that
has been applied. Local database migrations are SQLDelight `.sqm` files in
`data/src/commonMain/sqldelight/migrations/` (`1.sqm` is the initial schema; add `2.sqm` for
the next change). `SchemaTest` checks that the sync registry matches the real schema.

For a fully local stack, `supabase start` runs everything in Docker. The Android emulator
reaches it at `http://10.0.2.2:54321`, which is the only plain-HTTP address the app accepts.

## Running tests

```sh
./gradlew :core:jvmTest                 # domain logic
./gradlew :data:jvmTest                 # SQLite schema, repositories, search (incl. 5,000-bottle performance), sync, import/export
./gradlew :composeApp:jvmTest           # design-token contrast + end-to-end UI flows on the desktop JVM
./gradlew :core:iosSimulatorArm64Test :data:iosSimulatorArm64Test   # on a Mac: same tests on Kotlin/Native + system SQLite

supabase/tests/run.sh -h localhost -p 5432 -U postgres   # server: RLS, households, sync RPC (needs PostgreSQL 16)
```

The server tests apply the migrations to a scratch database with a minimal stand-in for
Supabase's `auth` and `storage` schemas, then check that users can't read or write each
other's collections, viewers can't write, movements can't be updated or deleted, pushes are
idempotent and stale writes are reported as conflicts.

Camera scanning, file pickers, the share sheet, notifications and background sync need a
real device. The manual release checklist is at the end of `docs/CHECKLIST.md`.

## Building

**Android**

```sh
./gradlew :androidApp:assembleDebug     # androidApp/build/outputs/apk/debug/
./gradlew :androidApp:installDebug      # to a connected device or emulator
```

Release builds (`:androidApp:bundleRelease`) are minified and need your own signing
configuration in `androidApp/build.gradle.kts`, with the keystore kept out of the repository.

**iOS**

```sh
cd iosApp
cp Configuration/Secrets.xcconfig.example Configuration/Secrets.xcconfig   # then edit, or leave empty for local-only
xcodegen generate
open HouseOfVino.xcodeproj
```

Run the `HouseOfVino` scheme. The first build compiles the Kotlin framework through Gradle,
which takes several minutes.

## Continuous integration

`.github/workflows/ci.yml` runs on every push to `main` and on pull requests:

- **server-sql**: the Supabase migrations and contract tests on PostgreSQL 16.
- **jvm-android**: `core`, `data` and `composeApp` JVM tests and the Android debug build. The
  APK is kept as a workflow artifact for 14 days.
- **ios** (opt-in): Kotlin/Native tests on the iOS simulator, then XcodeGen and an
  `xcodebuild` simulator build. It runs only when the commit message contains `[ios]` or the
  workflow is started manually with *ios* ticked, because macOS runner minutes are billed at
  a much higher rate.

Each job also commits a summary (failed tests with messages, compiler errors, the log tail
and the full log, gzipped) to the `ci-results` branch under `runs/<run>-<sha>/<job>/`, with
the latest result per job in `latest-<job>.md`.

## External services, costs and credentials

Prices are as published at the time of writing (October 2026). Check the providers' pages
before relying on them.

| Service | Used for | Cost | Credentials | Notes |
|---|---|---|---|---|
| Supabase | Sync, accounts, photo storage, households | Free plan $0 (500 MB database, 1 GB files; **projects pause after a week of inactivity**). Pro plan from $25/month per project ⚑ | Project URL + publishable key in the app; service-role key only inside the Edge Function | Optional: the app works without it |
| Open Food Facts | Barcode lookup and the *Discover* search scope | Free | None. Requests send a `User-Agent`; set a real contact address in `OpenFoodFactsBarcodeLookup.DEFAULT_USER_AGENT` before release | Data under the **ODbL** (attribution shown with results; share-alike applies to any database built from it) ⚑. Limits: 15 product reads and 10 searches per minute per user. The app stays below them and only searches when asked |
| Google ML Kit (Android) | On-device barcode and text recognition | Free | None | Models are bundled. Google may receive diagnostics (no images or text) ⚑; disclose in the Play *Data safety* form |
| Apple Vision / AVFoundation (iOS) | On-device barcode and text recognition | Free | None | |
| GitHub Actions | CI | Included minutes on your plan for private repositories, then per-minute billing; macOS minutes cost several times Linux minutes ⚑ | Built-in `GITHUB_TOKEN`; optional `SUPABASE_URL` / `SUPABASE_ANON_KEY` secrets | The iOS job is opt-in for that reason |
| Apple Developer Program | Installing on devices beyond personal testing, TestFlight, App Store | US$99/year | Apple ID, team ID | Needed for distribution only |
| Google Play Console | Play Store distribution | US$25 one-time | Google account | Needed for distribution only |

There is no wine-catalogue or price API in this release. No licence-clean, free source
exists, and the app never invents prices, wine metadata, ratings, drinking windows or search
results. Values and drinking windows come from the user, and a value is always shown with its
source and date. `WineCatalogService` and `PriceService` are interfaces ready for a licensed provider.

## Decisions to be aware of

The full reasoning is in `docs/ADR.md`; the short version:

- ⚑ **Privacy**: data stays on the device until the user signs in. On the server every table
  has row-level security, and a collection is visible only to its household members. Android
  device backups exclude the cellar (ADR-11), so backup is sync or CSV export. Label OCR runs
  on the device.
- ⚑ **Recurring cost**: Supabase Pro for production; GitHub Actions minutes for CI.
- ⚑ **Licensing**: Open Food Facts data is ODbL. Fonts (Bodoni Moda, IBM Plex Sans) are under
  the SIL Open Font License; the licence files are in `composeApp/licenses/`.
- ⚑ **Irreversible**: deleting an account removes every collection the user owns, including
  shared households, their photos and history (ADR-08). The app offers an export first and
  requires typing a confirmation. Vintages are never merged without explicit confirmation, and
  stock history is never edited: corrections are new ledger entries.

## Known limitations

See the "Status" section of `docs/CHECKLIST.md` for what each milestone has verified so far.
Things that are by design or deferred:

- No catalogue or market-price integration (above).
- Conflict resolution is per record (keep mine / keep theirs), not per field.
- Household roles are owner / editor / viewer per collection; there is no per-bottle sharing.
- Photos sync after their record; a photo taken offline uploads at the next sync.
