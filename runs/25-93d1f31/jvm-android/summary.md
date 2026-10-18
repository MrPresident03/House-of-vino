# jvm-android: failure

- commit: 93d1f311bb2059fc88d6bc9b1d9bb70b1ae5fdf1
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39102219523
- tests: 159 run, 0 failed, 0 skipped

## Compiler errors (2)
```
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/locations/LocationScreens.kt:250:61 Smart cast to 'String' is impossible, because 'photoPath' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/locations/LocationScreens.kt:250:61 Smart cast to 'String' is impossible, because 'photoPath' is a public API property declared in different module.
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':composeApp:compileKotlinJvm' (registered in build file 'composeApp/build.gradle.kts').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: SMARTCAST_IMPOSSIBLE
       Kotlin compiler error
         Smart cast to 'String' is impossible, because 'photoPath' is a public API property declared in different module.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/locations/LocationScreens.kt line 250

---
* What went wrong:
Execution failed for task ':composeApp:compileAndroidMain' (registered by plugin 'com.android.internal.kotlin.multiplatform.library').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: SMARTCAST_IMPOSSIBLE
       Kotlin compiler error
         Smart cast to 'String' is impossible, because 'photoPath' is a public API property declared in different module.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/locations/LocationScreens.kt line 250

```

## Tests per class
- Base64Test: 4
- CandidateMatcherTest: 4
- CatalogueTest: 8
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
