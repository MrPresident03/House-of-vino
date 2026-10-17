# ios: failure

- commit: aac1e564736aa2aeefc814e7335939d1617481da
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39015182712
- tests: 120 run, 0 failed, 0 skipped

## Compiler errors (10)
```
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/iosMain/kotlin/com/houseofvino/app/platform/BackgroundSync.kt:33:55: error: This declaration needs opt-in. Its usage must be marked with '@kotlinx.cinterop.ExperimentalForeignApi' or '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/iosMain/kotlin/com/houseofvino/app/platform/CameraScanner.ios.kt:46:30: error: Unresolved reference 'defaultDeviceWithMediaType'.
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/iosMain/kotlin/com/houseofvino/app/platform/IosPlatform.kt:12:30: error: Unresolved reference 'defaultDeviceWithMediaType'.
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/iosMain/kotlin/com/houseofvino/app/platform/IosPlatform.kt:96:70: error: Unresolved reference 'secondsFromGMT' on receiver of type 'NSTimeZone'.
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/iosMain/kotlin/com/houseofvino/app/platform/IosPlatform.kt:129:20: error: Unresolved reference 'popoverPresentationController' on receiver of type 'UIActivityViewController'.
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/iosMain/kotlin/com/houseofvino/app/platform/IosPlatform.kt:194:17: error: 'val' cannot be reassigned.
/Users/runner/work/House-of-vino/House-of-vino/composeApp/src/iosMain/kotlin/com/houseofvino/app/platform/IosPlatform.kt:196:17: error: 'val' cannot be reassigned.
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

