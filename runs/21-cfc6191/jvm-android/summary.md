# jvm-android: failure

- commit: cfc6191def6293b731056c737d1b6efe3d262383
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39066598662
- tests: 159 run, 2 failed, 0 skipped

## Compiler errors (4)
```
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/scan/ScanScreen.kt:256:54 Unresolved reference 'name' on receiver of type 'Suggested<WineColor>'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/scan/ScanScreen.kt:257:53 Unresolved reference 'name' on receiver of type 'Suggested<WineStyle>'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/scan/ScanScreen.kt:256:54 Unresolved reference 'name' on receiver of type 'Suggested<WineColor>'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/scan/ScanScreen.kt:257:53 Unresolved reference 'name' on receiver of type 'Suggested<WineStyle>'.
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':composeApp:compileKotlinJvm' (registered in build file 'composeApp/build.gradle.kts').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_REFERENCE
       Kotlin compiler error
         Unresolved reference 'name' on receiver of type 'Suggested<WineColor>'.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/scan/ScanScreen.kt line 256

---
* What went wrong:
Execution failed for task ':composeApp:compileAndroidMain' (registered by plugin 'com.android.internal.kotlin.multiplatform.library').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_REFERENCE
       Kotlin compiler error
         Unresolved reference 'name' on receiver of type 'Suggested<WineColor>'.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/scan/ScanScreen.kt line 256

---
* What went wrong:
Execution failed for task ':data:jvmTest'.
> There were failing tests. See the report at: file:///home/runner/work/House-of-vino/House-of-vino/data/build/reports/tests/jvmTest/index.html

* Try:
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

* Exception is:
---
* What went wrong:
Execution failed for task ':data:buildCataloguePack' (registered in build file 'data/build.gradle.kts').
> Process 'command '/usr/lib/jvm/temurin-21-jdk-amd64/bin/java'' finished with non-zero exit value 1

* Try:
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.
```

## Failed tests (2)

### com.houseofvino.data.catalogue.CatalogueTest.styles_map_from_lwin_types[jvm]
```
org.junit.ComparisonFailure: expected:<..., Premier Cru Classe[]> but was:<..., Premier Cru Classe[, Margaux]>
org.junit.ComparisonFailure: expected:<..., Premier Cru Classe[]> but was:<..., Premier Cru Classe[, Margaux]>
	at org.junit.Assert.assertEquals(Assert.java:117)
	at kotlin.test.junit.JUnitAsserter.assertEquals(JUnitSupport.kt:32)
	at kotlin.test.AssertionsKt__AssertionsKt.assertEquals(Assertions.kt:83)
	at kotlin.test.AssertionsKt.assertEquals(Unknown Source)
	at kotlin.test.AssertionsKt__AssertionsKt.assertEquals$default(Assertions.kt:82)
	at kotlin.test.AssertionsKt.assertEquals$default(Unknown Source)
	at com.houseofvino.data.catalogue.CatalogueTest.styles_map_from_lwin_types(CatalogueTest.kt:84)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
```

### com.houseofvino.data.catalogue.CatalogueTest.real_lwin_pack[jvm]
```
org.junit.ComparisonFailure: FELTON ROAD
BLOCK 3
PINOT NOIR
BANNOCKBURN
CENTRAL OTAGO
2021 expected:<B[lock 3] Pinot Noir> but was:<B[annockburn] Pinot Noir>
org.junit.ComparisonFailure: FELTON ROAD
BLOCK 3
PINOT NOIR
BANNOCKBURN
CENTRAL OTAGO
2021 expected:<B[lock 3] Pinot Noir> but was:<B[annockburn] Pinot Noir>
	at org.junit.Assert.assertEquals(Assert.java:117)
	at kotlin.test.junit.JUnitAsserter.assertEquals(JUnitSupport.kt:32)
	at kotlin.test.AssertionsKt__AssertionsKt.assertEquals(Assertions.kt:83)
	at kotlin.test.AssertionsKt.assertEquals(Unknown Source)
	at com.houseofvino.data.catalogue.CatalogueTest.real_lwin_pack(CatalogueTest.kt:113)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
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
