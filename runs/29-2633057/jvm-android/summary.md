# jvm-android: failure

- commit: 2633057d0cb817440f819b94c040273c81559f41
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/43373306337
- tests: 197 run, 2 failed, 0 skipped

## Gradle failures
```
* What went wrong:
Execution failed for task ':core:jvmTest'.
> There were failing tests. See the report at: file:///home/runner/work/House-of-vino/House-of-vino/core/build/reports/tests/jvmTest/index.html

* Try:
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

* Exception is:
---
* What went wrong:
Execution failed for task ':composeApp:jvmTest'.
> There were failing tests. See the report at: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/build/reports/tests/jvmTest/index.html

* Try:
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

* Exception is:
```

## Failed tests (2)

### com.houseofvino.app.CriticalFlowsTest.varietalPickerFindsAGrapeBySynonymAndFillsTheColour[jvm]
```
androidx.compose.ui.test.ComposeTimeoutException: Condition still not satisfied after 10000 ms
androidx.compose.ui.test.ComposeTimeoutException: Condition still not satisfied after 10000 ms
	at androidx.compose.ui.test.SkikoComposeUiTest.waitUntil(ComposeUiTest.skiko.kt:468)
	at androidx.compose.ui.test.ComposeUiTest.waitUntil$default(ComposeUiTest.kt:228)
	at com.houseofvino.app.CriticalFlowsTest$varietalPickerFindsAGrapeBySynonymAndFillsTheColour$1.invokeSuspend(CriticalFlowsTest.kt:153)
	at com.houseofvino.app.CriticalFlowsTest$varietalPickerFindsAGrapeBySynonymAndFillsTheColour$1.invoke(CriticalFlowsTest.kt)
	at com.houseofvino.app.CriticalFlowsTest$varietalPickerFindsAGrapeBySynonymAndFillsTheColour$1.invoke(CriticalFlowsTest.kt)
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
	at androidx.compose.ui.test.SkikoComposeUiTest$runTest$1.invoke(ComposeUiTest.skiko.kt)
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt$runTest$2$1$1.invokeSuspend(TestBuilders.kt:317)
	at _COROUTINE._BOUNDARY._(CoroutineDebugging.kt:42)
```

### com.houseofvino.core.money.MoneyTest.rejectsNumbersMixedWithOtherText[jvm]
```
java.lang.AssertionError: should reject "list 45" expected null, but was:<EUR 45.00>
java.lang.AssertionError: should reject "list 45" expected null, but was:<EUR 45.00>
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.failNotNull(Assert.java:756)
	at org.junit.Assert.assertNull(Assert.java:738)
	at kotlin.test.junit.JUnitAsserter.assertNull(JUnitSupport.kt:52)
	at kotlin.test.AssertionsKt__AssertionsKt.assertNull(Assertions.kt:310)
	at kotlin.test.AssertionsKt.assertNull(Unknown Source)
	at com.houseofvino.core.money.MoneyTest.rejectsNumbersMixedWithOtherText(MoneyTest.kt:58)
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
