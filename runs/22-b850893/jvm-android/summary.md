# jvm-android: failure

- commit: b850893619051d3718f9a167029218c826cf1da7
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39067146811
- tests: 102 run, 0 failed, 0 skipped

## Compiler errors (2)
```
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/catalogue/CataloguePack.kt:57:21 Cannot access 'fun endTransaction(successful: Boolean): QueryResult<Unit>': it is protected in 'app.cash.sqldelight.Transacter.Transaction'.
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/catalogue/CataloguePack.kt:57:21 Cannot access 'fun endTransaction(successful: Boolean): QueryResult<Unit>': it is protected in 'app.cash.sqldelight.Transacter.Transaction'.
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':data:compileAndroidMain' (registered by plugin 'com.android.internal.kotlin.multiplatform.library').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: INVISIBLE_REFERENCE
       Kotlin compiler error
         Cannot access 'fun endTransaction(successful: Boolean): QueryResult<Unit>': it is protected in 'app.cash.sqldelight.Transacter.Transaction'.
         Location: /home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/catalogue/CataloguePack.kt line 57

---
* What went wrong:
Execution failed for task ':data:compileKotlinJvm' (registered in build file 'data/build.gradle.kts').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: INVISIBLE_REFERENCE
       Kotlin compiler error
         Cannot access 'fun endTransaction(successful: Boolean): QueryResult<Unit>': it is protected in 'app.cash.sqldelight.Transacter.Transaction'.
         Location: /home/runner/work/House-of-vino/House-of-vino/data/src/commonMain/kotlin/com/houseofvino/data/catalogue/CataloguePack.kt line 57

```

## Tests per class
- Base64Test: 4
- CandidateMatcherTest: 4
- CryptoTest: 10
- CsvTest: 4
- DuplicateDetectorTest: 2
- ExchangeRateTest: 7
- FieldMergeTest: 7
- FuzzyTest: 3
- HlcTest: 6
- ImportTest: 8
- InsightsTest: 3
- IsoDateTest: 5
- LabelTextParserTest: 4
- LedgerTest: 10
- ModelTest: 7
- MoneyTest: 10
- QrCodeTest: 3
- QueryParserTest: 3
- TextNormalizerTest: 2
