# jvm-android: failure

- commit: c7b621ce0788c2fd79cf80a90d44c16503ef0f34
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/43376509041
- tests: 197 run, 1 failed, 0 skipped

## Gradle failures
```
* What went wrong:
Execution failed for task ':composeApp:jvmTest'.
> There were failing tests. See the report at: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/build/reports/tests/jvmTest/index.html

* Try:
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

* Exception is:
```

## Failed tests (1)

### com.houseofvino.app.CriticalFlowsTest.firstRunAddWineWithBottlesThenDrinkOne[jvm]
```
androidx.compose.ui.test.ComposeTimeoutException: Condition still not satisfied after 10000 ms
androidx.compose.ui.test.ComposeTimeoutException: Condition still not satisfied after 10000 ms
	at androidx.compose.ui.test.SkikoComposeUiTest.waitUntil(ComposeUiTest.skiko.kt:468)
	at androidx.compose.ui.test.ComposeUiTest.waitUntil$default(ComposeUiTest.kt:228)
	at com.houseofvino.app.CriticalFlowsTest.waitFor(CriticalFlowsTest.kt:42)
	at com.houseofvino.app.CriticalFlowsTest.waitForText(CriticalFlowsTest.kt:44)
	at com.houseofvino.app.CriticalFlowsTest.access$waitForText(CriticalFlowsTest.kt:37)
	at com.houseofvino.app.CriticalFlowsTest$firstRunAddWineWithBottlesThenDrinkOne$1.invokeSuspend(CriticalFlowsTest.kt:71)
	at com.houseofvino.app.CriticalFlowsTest$firstRunAddWineWithBottlesThenDrinkOne$1.invoke(CriticalFlowsTest.kt)
	at com.houseofvino.app.CriticalFlowsTest$firstRunAddWineWithBottlesThenDrinkOne$1.invoke(CriticalFlowsTest.kt)
	at androidx.compose.ui.test.ComposeUiTest_skikoKt$runComposeUiTest$1.invokeSuspend(ComposeUiTest.skiko.kt:95)
	at androidx.compose.ui.test.ComposeUiTest_skikoKt$runComposeUiTest$1.invoke(ComposeUiTest.skiko.kt)
	at androidx.compose.ui.test.ComposeUiTest_skikoKt$runComposeUiTest$1.invoke(ComposeUiTest.skiko.kt)
	at androidx.compose.ui.test.SkikoComposeUiTest$runTest$1$1$1$1$1.invokeSuspend(ComposeUiTest.skiko.kt:276)
	at androidx.compose.ui.test.SkikoComposeUiTest$runTest$1$1$1$1$1.invoke(ComposeUiTest.skiko.kt)
	at androidx.compose.ui.test.SkikoComposeUiTest$runTest$1$1$1$1$1.invoke(ComposeUiTest.skiko.kt)
	at androidx.compose.ui.platform.FrameRecomposer$withMonotonicFrameClock$2.invokeSuspend(FrameRecomposer.skiko.kt:182)
	at androidx.compose.ui.platform.FrameRecomposer$withMonotonicFrameClock$2.invoke(FrameRecomposer.skiko.kt)
	at androidx.compose.ui.platform.FrameRecomposer$withMonotonicFrameClock$2.invoke(FrameRecomposer.skiko.kt)
	at kotlinx.coroutines.intrinsics.UndispatchedKt.startUndispatched(Undispatched.kt:66)
	at kotlinx.coroutines.intrinsics.UndispatchedKt.startUndispatchedOrReturn(Undispatched.kt:43)
	at kotlinx.coroutines.BuildersKt__Builders_commonKt.withContext(Builders.common.kt:497)
	at kotlinx.coroutines.BuildersKt.withContext(Unknown Source)
	at androidx.compose.ui.platform.FrameRecomposer.withMonotonicFrameClock(FrameRecomposer.skiko.kt:181)
	at androidx.compose.ui.test.SkikoComposeUiTest$runTest$1.invokeSuspend(ComposeUiTest.skiko.kt:275)
	at androidx.compose.ui.test.SkikoComposeUiTest$runTest$1.invoke(ComposeUiTest.skiko.kt)
```

## Tests per class
- Base64Test: 4
- CandidateMatcherTest: 4
- CatalogueTest: 8
- CellarRepositoryTest: 11
- CriticalFlowsTest: 8
- CryptoTest: 10
- CsvTest: 4
- DesignTokensTest: 7
- DuplicateDetectorTest: 2
- ExchangeRateTest: 8
- FieldMergeTest: 7
- FuzzyTest: 3
- GrapeVarietiesTest: 7
- HlcTest: 6
- HouseholdRejoinTest: 1
- HouseholdSyncTest: 17
- ImportExportTest: 7
- ImportTest: 12
- InsightsTest: 3
- IsoDateTest: 5
- LabelTextParserTest: 4
- LedgerTest: 10
- ModelTest: 7
- MoneyTest: 11
- OpenFoodFactsTest: 6
- QrCodeTest: 3
- QueryParserTest: 4
- SchemaTest: 5
- SearchEngineTest: 9
- SharingFlowTest: 2
- TextNormalizerTest: 2
