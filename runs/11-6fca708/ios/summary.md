# ios: failure

- commit: 6fca708ec4646372a5f96a013a1cb5af8e92c4a4
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39058791180
- tests: 151 run, 0 failed, 0 skipped

## Compiler errors (5)
```
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:271:83: error: Cannot access 'val IO: CoroutineDispatcher': it is internal in 'kotlinx.coroutines.Dispatchers'.
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/sharing/SharingScreens.kt:246:72: error: Smart cast to 'Long' is impossible, because 'lastHeardAt' is a public API property declared in different module.
error: Compilation finished with errors
error: Compilation finished with errors
error: Compilation finished with errors
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':composeApp:compileKotlinIosSimulatorArm64' (registered in build file 'composeApp/build.gradle.kts').
> Compilation finished with errors

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
```

## Tests per class
- Base64Test: 4
- CandidateMatcherTest: 4
- CellarRepositoryTest: 8
- CryptoTest: 10
- CsvTest: 4
- DuplicateDetectorTest: 2
- ExchangeRateTest: 7
- FieldMergeTest: 7
- FuzzyTest: 3
- HlcTest: 6
- HouseholdSyncTest: 15
- ImportExportTest: 7
- ImportTest: 8
- InsightsTest: 3
- IsoDateTest: 5
- LabelTextParserTest: 4
- LedgerTest: 10
- ModelTest: 7
- MoneyTest: 10
- OpenFoodFactsTest: 6
- QrCodeTest: 3
- QueryParserTest: 3
- SchemaTest: 5
- SearchEngineTest: 8
- TextNormalizerTest: 2
