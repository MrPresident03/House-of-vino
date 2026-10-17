# ios: failure

- commit: 85d1736e2412cb8c37e124b8235b809b5ada5641
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39014857766
- tests: 120 run, 0 failed, 0 skipped

## Compiler errors (3)
```
error: Unknown iOS simulator arch: 'x86_64'
error: Unknown iOS simulator arch: 'x86_64'
error: Unknown iOS simulator arch: 'x86_64'
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':composeApp:syncComposeResourcesForIos' (registered by plugin 'org.jetbrains.compose').
> Unknown iOS simulator arch: 'x86_64'

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
---
* What went wrong:
Execution failed for task ':composeApp:validateArchitecturesForEmbedAndSignAppleFrameworkForXcode' (registered in build file 'composeApp/build.gradle.kts').
> ⛔ Xcode Requested Architecture Not Configured in Gradle
  Xcode requested target architectures that are not configured in your Gradle build: ios_x64.
  The framework 'composeApp' cannot be built for these target architectures.
  Solutions:
   * Add the missing Kotlin/Native target(s) to your kotlin {} block in build.gradle.kts
   * Or exclude unsupported architectures in your Xcode project's Build Settings by setting EXCLUDED_ARCHS
```

