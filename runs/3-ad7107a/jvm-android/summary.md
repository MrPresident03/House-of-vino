# jvm-android: failure

- commit: ad7107afe589359335cc7f375bf95646f6478cf6
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39014063512
- tests: 120 run, 3 failed, 0 skipped

## Compiler errors (2)
```
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/jvmTest/kotlin/com/houseofvino/app/CriticalFlowsTest.kt:38:19 Argument type mismatch: actual type is 'Long', but 'String?' was expected.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/jvmTest/kotlin/com/houseofvino/app/CriticalFlowsTest.kt:77:19 Argument type mismatch: actual type is 'Long', but 'String?' was expected.
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':data:jvmTest'.
> There were failing tests. See the report at: file:///home/runner/work/House-of-vino/House-of-vino/data/build/reports/tests/jvmTest/index.html

* Try:
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

* Exception is:
---
* What went wrong:
Execution failed for task ':composeApp:compileTestKotlinJvm' (registered in build file 'composeApp/build.gradle.kts').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: ARGUMENT_TYPE_MISMATCH
       Kotlin compiler error
         Argument type mismatch: actual type is 'Long', but 'String?' was expected.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/jvmTest/kotlin/com/houseofvino/app/CriticalFlowsTest.kt line 38

```

## Failed tests (3)

### com.houseofvino.data.SearchEngineTest.indexFollowsEditsAndDeletes[jvm]
```
org.sqlite.SQLiteException: [SQLITE_CORRUPT_VTAB] Content in the virtual table is corrupt (database disk image is malformed)
org.sqlite.SQLiteException: [SQLITE_CORRUPT_VTAB] Content in the virtual table is corrupt (database disk image is malformed)
	at org.sqlite.core.DB.newSQLException(DB.java:1182)
	at org.sqlite.core.DB.newSQLException(DB.java:1193)
	at org.sqlite.core.DB.execute(DB.java:991)
	at org.sqlite.jdbc3.JDBC3PreparedStatement.lambda$executeQuery$1(JDBC3PreparedStatement.java:93)
	at org.sqlite.jdbc3.JDBC3Statement.withConnectionTimeout(JDBC3Statement.java:464)
	at org.sqlite.jdbc3.JDBC3PreparedStatement.executeQuery(JDBC3PreparedStatement.java:88)
	at app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement.executeQuery(JdbcDriver.kt:288)
	at app.cash.sqldelight.driver.jdbc.JdbcDriver.executeQuery(JdbcDriver.kt:161)
	at com.houseofvino.data.search.SearchEngine.vocabularyNear(SearchEngine.kt:119)
	at com.houseofvino.data.search.SearchEngine.runWithTypoFallback(SearchEngine.kt:101)
	at com.houseofvino.data.search.SearchEngine.searchCellar(SearchEngine.kt:83)
	at com.houseofvino.data.SearchEngineTest.cellar(SearchEngineTest.kt:46)
	at com.houseofvino.data.SearchEngineTest.cellar$default(SearchEngineTest.kt:45)
	at com.houseofvino.data.SearchEngineTest.indexFollowsEditsAndDeletes(SearchEngineTest.kt:140)
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
```

### com.houseofvino.data.SearchEngineTest.matchesAccentInsensitivePrefixesAcrossFields[jvm]
```
org.sqlite.SQLiteException: [SQLITE_CORRUPT_VTAB] Content in the virtual table is corrupt (database disk image is malformed)
org.sqlite.SQLiteException: [SQLITE_CORRUPT_VTAB] Content in the virtual table is corrupt (database disk image is malformed)
	at org.sqlite.core.DB.newSQLException(DB.java:1182)
	at org.sqlite.core.DB.newSQLException(DB.java:1193)
	at org.sqlite.core.DB.execute(DB.java:991)
	at org.sqlite.jdbc3.JDBC3PreparedStatement.lambda$executeQuery$1(JDBC3PreparedStatement.java:93)
	at org.sqlite.jdbc3.JDBC3Statement.withConnectionTimeout(JDBC3Statement.java:464)
	at org.sqlite.jdbc3.JDBC3PreparedStatement.executeQuery(JDBC3PreparedStatement.java:88)
	at app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement.executeQuery(JdbcDriver.kt:288)
	at app.cash.sqldelight.driver.jdbc.JdbcDriver.executeQuery(JdbcDriver.kt:161)
	at com.houseofvino.data.search.SearchEngine.vocabularyNear(SearchEngine.kt:119)
	at com.houseofvino.data.search.SearchEngine.runWithTypoFallback(SearchEngine.kt:101)
	at com.houseofvino.data.search.SearchEngine.searchCellar(SearchEngine.kt:83)
	at com.houseofvino.data.SearchEngineTest.cellar(SearchEngineTest.kt:46)
	at com.houseofvino.data.SearchEngineTest.cellar$default(SearchEngineTest.kt:45)
	at com.houseofvino.data.SearchEngineTest.matchesAccentInsensitivePrefixesAcrossFields(SearchEngineTest.kt:61)
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
```

### com.houseofvino.data.SearchEngineTest.correctsTyposWhenNothingMatches[jvm]
```
org.sqlite.SQLiteException: [SQLITE_CORRUPT_VTAB] Content in the virtual table is corrupt (database disk image is malformed)
org.sqlite.SQLiteException: [SQLITE_CORRUPT_VTAB] Content in the virtual table is corrupt (database disk image is malformed)
	at org.sqlite.core.DB.newSQLException(DB.java:1182)
	at org.sqlite.core.DB.newSQLException(DB.java:1193)
	at org.sqlite.core.DB.execute(DB.java:991)
	at org.sqlite.jdbc3.JDBC3PreparedStatement.lambda$executeQuery$1(JDBC3PreparedStatement.java:93)
	at org.sqlite.jdbc3.JDBC3Statement.withConnectionTimeout(JDBC3Statement.java:464)
	at org.sqlite.jdbc3.JDBC3PreparedStatement.executeQuery(JDBC3PreparedStatement.java:88)
	at app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement.executeQuery(JdbcDriver.kt:288)
	at app.cash.sqldelight.driver.jdbc.JdbcDriver.executeQuery(JdbcDriver.kt:161)
	at com.houseofvino.data.search.SearchEngine.vocabularyNear(SearchEngine.kt:119)
	at com.houseofvino.data.search.SearchEngine.runWithTypoFallback(SearchEngine.kt:101)
	at com.houseofvino.data.search.SearchEngine.searchCellar(SearchEngine.kt:83)
	at com.houseofvino.data.SearchEngineTest.cellar(SearchEngineTest.kt:46)
	at com.houseofvino.data.SearchEngineTest.cellar$default(SearchEngineTest.kt:45)
	at com.houseofvino.data.SearchEngineTest.correctsTyposWhenNothingMatches(SearchEngineTest.kt:68)
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
```
