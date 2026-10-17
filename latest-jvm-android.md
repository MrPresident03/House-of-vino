# jvm-android: failure

- commit: 383f203d522f1f8870c8322dfc9147ac60348f77
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39013903895
- tests: 78 run, 0 failed, 0 skipped

## Compiler errors (59)
```
e: file:///home/runner/work/House-of-vino/House-of-vino/data/src/commonTest/kotlin/com/houseofvino/data/CellarRepositoryTest.kt:83:9 Type inference failed. The value of the type parameter 'T' must be mentioned in input types (argument types, receiver type, or expected type). Try to specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:28:8 Unresolved reference 'io'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:20 Unresolved reference 'SupabaseClient'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:84 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:90 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:62:18 Unresolved reference 'SupabaseClient'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:49 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:51 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:52 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:54 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:65 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:72:21 Cannot access class 'io.github.jan.supabase.postgrest.result.PostgrestResult'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:30 No value passed for parameter 'writeLocal'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:76 Argument type mismatch: actual type is '(??? (Unknown lambda parameter type), ??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type)', but 'Long' was expected.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:78 Cannot infer type for value parameter 'id'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:82 Cannot infer type for value parameter 'bytes'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/cellar/VintageDetailScreen.kt:188:58 Smart cast to 'String' is impossible, because 'labelPhotoPath' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/navigation/AppNavHost.kt:175:72 Smart cast to 'String' is impossible, because 'vintageId' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:172:42 Cannot access class 'io.github.jan.supabase.auth.user.UserInfo'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:174:46 Cannot access class 'io.github.jan.supabase.auth.OtpVerifyResult'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:28:8 Unresolved reference 'io'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:20 Unresolved reference 'SupabaseClient'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:84 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:90 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:62:18 Unresolved reference 'SupabaseClient'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:49 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:51 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:52 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:54 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:65 Cannot access class 'io.github.jan.supabase.SupabaseClient'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:72:21 Cannot access class 'io.github.jan.supabase.postgrest.result.PostgrestResult'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:30 No value passed for parameter 'writeLocal'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:76 Argument type mismatch: actual type is '(??? (Unknown lambda parameter type), ??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type)', but 'Long' was expected.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:78 Cannot infer type for value parameter 'id'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:82 Cannot infer type for value parameter 'bytes'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/cellar/VintageDetailScreen.kt:188:58 Smart cast to 'String' is impossible, because 'labelPhotoPath' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/navigation/AppNavHost.kt:175:72 Smart cast to 'String' is impossible, because 'vintageId' is a public API property declared in different module.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:172:42 Cannot access class 'io.github.jan.supabase.auth.user.UserInfo'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:174:46 Cannot access class 'io.github.jan.supabase.auth.OtpVerifyResult'. Check your module classpath for missing or conflicting dependencies.
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':data:compileTestKotlinJvm' (registered in build file 'data/build.gradle.kts').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR
       Kotlin compiler error
         Type inference failed. The value of the type parameter 'T' must be mentioned in input types (argument types, receiver type, or expected type). Try to specify it explicitly.
         Location: /home/runner/work/House-of-vino/House-of-vino/data/src/commonTest/kotlin/com/houseofvino/data/CellarRepositoryTest.kt line 83

---
* What went wrong:
Execution failed for task ':composeApp:compileKotlinJvm' (registered in build file 'composeApp/build.gradle.kts').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_IMPORT
       Kotlin compiler error
         Unresolved reference 'io'.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt line 28

---
* What went wrong:
Execution failed for task ':composeApp:compileAndroidMain' (registered by plugin 'com.android.internal.kotlin.multiplatform.library').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_IMPORT
       Kotlin compiler error
         Unresolved reference 'io'.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt line 28

```

