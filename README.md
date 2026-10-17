# House of Vino

A cellar book for serious wine collections, for iOS and Android: inventory with purchase
batches and locations, an append-only stock ledger, search that stays fast at thousands of
bottles, a wishlist, a tasting journal, insights, CSV import/export and on-device label and
barcode scanning. It works fully offline, and partners can share one cellar between their
phones without an account or a server, so it costs nothing to run.

Built with Kotlin Multiplatform and Compose Multiplatform, with SQLDelight (SQLite) on the
device. Household sharing is a replicated change journal exchanged as encrypted files
([`docs/SYNC.md`](docs/SYNC.md)).

Design decisions, with every choice that affects **privacy, recurring cost, data licensing or
irreversible data changes** marked ⚑, are in [`docs/ADR.md`](docs/ADR.md). Also in `docs/`:
the database schema, navigation map, design system and the implementation checklist, which
records what is done and tested.

## Repository layout

| Path | What it is |
|---|---|
| `core/` | Pure Kotlin domain logic, no dependencies: money (integer minor units, HALF_EVEN FX), vintages and formats, the stock ledger, text normalisation and fuzzy matching, the CSV reader/writer, import mapping and duplicate detection, insights maths, scan candidate matching, sync merge rules and hybrid logical clocks, XChaCha20-Poly1305 / SHA-256 / Base64, a QR code encoder |
| `data/` | SQLDelight schema and migrations, repositories, FTS search engine, import/export, the change journal, households and invites, encrypted sync files, conflict review, Open Food Facts lookup |
| `composeApp/` | Shared Compose UI (all screens), design system, platform contract, camera scanners (CameraX + ML Kit on Android, AVFoundation + Vision on iOS), iOS platform services |
| `androidApp/` | Android application: activity, platform services, WorkManager reminders, manifest, FileProvider |
| `iosApp/` | iOS host app: XcodeGen spec, SwiftUI entry point, Info.plist settings, asset catalog |
| `.github/` | CI workflow and the script that publishes results to the `ci-results` branch |

## Prerequisites

- JDK 21 (17 or newer works). Gradle is provided by the wrapper (`./gradlew`, Gradle 9.7.1).
- Android: Android Studio with SDK platform 36. Minimum Android version is 11 (API 30).
- iOS: a Mac with Xcode 16 or newer and [XcodeGen](https://github.com/yonaskolb/XcodeGen)
  (`brew install xcodegen`). Minimum iOS version is 16.

There is no backend to set up.

## Configuration

None. The app has no server, no API keys and no accounts. The only build setting is your
Apple team ID for installing on a real iPhone: copy
`iosApp/Configuration/Secrets.xcconfig.example` to `Secrets.xcconfig` (git-ignored) and fill
in `HV_DEVELOPMENT_TEAM`. For Android, `local.properties` only needs `sdk.dir`
(see `local.properties.example`).

## How partners share a cellar

1. One phone: **More › Sharing › Share this cellar**. It creates a random household key and
   shows an invite QR code.
2. The other phone: **Sharing › Join a partner's cellar**, then scans the code (or pastes the
   invite text). Its own bottles are merged into the shared cellar. Both phones then show the
   same safety code.
3. From then on, **Send changes** makes an encrypted `.hovsync` file of everything the other
   phone hasn't seen, to pass on with any app (Messages, WhatsApp, email, AirDrop). The other
   phone opens it with **Receive changes**.

Edits to different details merge automatically. If both phones change the same detail before
swapping files, the later edit wins on both and the other is listed under **Changed on both
phones**. Stock is never overwritten: both phones' bottle movements are kept, and stock that
goes below zero is flagged for a recount. Details and the file format are in `docs/SYNC.md`.

Local database migrations are SQLDelight `.sqm` files in
`data/src/commonMain/sqldelight/com/houseofvino/data/db/` (`1.sqm` is the initial schema,
`2.sqm` adds the change journal). Never edit one that has shipped; add the next number.
`SchemaTest` checks that the sync table registry matches the real schema.

## Running tests

```sh
./gradlew :core:jvmTest                 # domain logic
./gradlew :data:jvmTest                 # SQLite schema, repositories, search (incl. 5,000-bottle performance), household sync, import/export
./gradlew :composeApp:jvmTest           # design-token contrast + end-to-end UI flows (incl. joining and swapping a sync file) on the desktop JVM
./gradlew :core:iosSimulatorArm64Test :data:iosSimulatorArm64Test   # on a Mac: same tests on Kotlin/Native + system SQLite
```

The household sync tests run two and three simulated phones against real databases: partners
converge after random edits with files lost, delayed or delivered twice; edits to the same
field are kept and reported; both drinking the last bottle is flagged; damaged, foreign and
removed partners' files are refused. The crypto and QR code tests check published vectors and
reference implementations.

Camera scanning, file pickers, the share sheet and notifications need a real device. The manual release checklist is at the end of `docs/CHECKLIST.md`.

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
cp Configuration/Secrets.xcconfig.example Configuration/Secrets.xcconfig   # your team ID, for real devices
xcodegen generate
open HouseOfVino.xcodeproj
```

Run the `HouseOfVino` scheme. The first build compiles the Kotlin framework through Gradle,
which takes several minutes.

## Continuous integration

`.github/workflows/ci.yml` runs on every push to `main` (and `work/**` branches) and on pull requests:

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

Running the app costs nothing: there is no server. Sync files travel through apps the users
already have. Prices below are as published at the time of writing (October 2026); check the
providers' pages before relying on them.

| Service | Used for | Cost | Credentials | Notes |
|---|---|---|---|---|
| Open Food Facts | Barcode lookup and the *Discover* search scope | Free | None. Requests send a `User-Agent`; set a real contact address in `OpenFoodFactsBarcodeLookup.DEFAULT_USER_AGENT` before release | Data under the **ODbL** (attribution shown with results; share-alike applies to any database built from it) ⚑. Limits: 15 product reads and 10 searches per minute per user. The app stays below them and only searches when asked |
| Google ML Kit (Android) | On-device barcode and text recognition | Free | None | Models are bundled. Google may receive diagnostics (no images or text) ⚑; disclose in the Play *Data safety* form |
| Apple Vision / AVFoundation (iOS) | On-device barcode and text recognition | Free | None | |
| GitHub Actions | CI | Included minutes on your plan for private repositories, then per-minute billing; macOS minutes cost several times Linux minutes ⚑ | Built-in `GITHUB_TOKEN` | The iOS job is opt-in for that reason |
| Apple Developer Program | Installing on devices beyond personal testing, TestFlight, App Store | US$99/year | Apple ID, team ID | Needed for distribution only |
| Google Play Console | Play Store distribution | US$25 one-time | Google account | Needed for distribution only |

There is no wine-catalogue or price API in this release. No licence-clean, free source
exists, and the app never invents prices, wine metadata, ratings, drinking windows or search
results. Values and drinking windows come from the user, and a value is always shown with its
source and date. `WineCatalogService` and `PriceService` are interfaces ready for a licensed provider.

## Decisions to be aware of

The full reasoning is in `docs/ADR.md`; the short version:

- ⚑ **Privacy**: data never leaves the phone except in sync files the user sends, which are
  encrypted with a key only the paired phones hold (ADR-13). The invite QR code *is* that key,
  so it's meant to be shown in person. Android device backups exclude the cellar (ADR-11), so
  backup is a partner's phone or CSV export. Label OCR runs on the device.
- ⚑ **Recurring cost**: none to run the app; GitHub Actions minutes for CI.
- ⚑ **Licensing**: Open Food Facts data is ODbL. Fonts (Bodoni Moda, IBM Plex Sans) are under
  the SIL Open Font License; the licence files are in `composeApp/licenses/`.
- ⚑ **Irreversible**: joining a household merges the joining phone's cellar into it, which
  can't be undone automatically. Removing a partner can't recall what their phone already
  has. "Erase this phone" deletes the local copy after an export offer and a typed
  confirmation. Vintages are never merged without explicit confirmation, and stock history is
  never edited: corrections are new ledger entries.

## Known limitations

See the "Status" section of `docs/CHECKLIST.md` for what each milestone has verified so far.
Things that are by design or deferred:

- No catalogue or market-price integration (above).
- Sharing is by file in this release: someone taps Send changes. Phone-to-phone sync over home
  Wi-Fi and a shared Google Drive folder are the next transports (ADR-13).
- Every partner can edit everything; there are no view-only partners.
- Opening a `.hovsync` file straight from a messaging app isn't wired up yet; use Receive
  changes inside the app.
- The journal keeps every change forever; compaction is planned before it matters (tens of
  thousands of edits).
