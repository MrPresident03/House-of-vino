# jvm-android: failure

- commit: d3a73a596455c4f624a4905d7677d3e9173a1a93
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39060351699
- tests: 162 run, 1 failed, 0 skipped

## Gradle failures
```
* What went wrong:
Execution failed for task ':data:jvmTest'.
> There were failing tests. See the report at: file:///home/runner/work/House-of-vino/House-of-vino/data/build/reports/tests/jvmTest/index.html

* Try:
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

* Exception is:
```

## Failed tests (1)

### com.houseofvino.data.SearchEngineTest.fiveThousandBottleCollectionStaysResponsive[jvm]
```
java.lang.AssertionError: slowest text search 1177 ms (load=8551ms text=[391, 258, 512, 1177, 575, 1017, 370] sorted=[63, 66, 54, 44] filtered=333ms)
java.lang.AssertionError: slowest text search 1177 ms (load=8551ms text=[391, 258, 512, 1177, 575, 1017, 370] sorted=[63, 66, 54, 44] filtered=333ms)
	at org.junit.Assert.fail(Assert.java:89)
	at kotlin.test.junit.JUnitAsserter.fail(JUnitSupport.kt:56)
	at kotlin.test.Asserter.assertTrue(Assertions.kt:1545)
	at kotlin.test.junit.JUnitAsserter.assertTrue(JUnitSupport.kt:30)
	at kotlin.test.Asserter.assertTrue(Assertions.kt:1555)
	at kotlin.test.junit.JUnitAsserter.assertTrue(JUnitSupport.kt:30)
	at kotlin.test.AssertionsKt__AssertionsKt.assertTrue(Assertions.kt:44)
	at kotlin.test.AssertionsKt.assertTrue(Unknown Source)
	at com.houseofvino.data.SearchEngineTest.fiveThousandBottleCollectionStaysResponsive(SearchEngineTest.kt:185)
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
```

## Tests per class
- Base64Test: 4
- CandidateMatcherTest: 4
- CellarRepositoryTest: 8
- CriticalFlowsTest: 4
- CryptoTest: 10
- CsvTest: 4
- DesignTokensTest: 5
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
- SharingFlowTest: 2
- TextNormalizerTest: 2
