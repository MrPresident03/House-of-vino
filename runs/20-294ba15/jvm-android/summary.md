# jvm-android: failure

- commit: 294ba159998ff1956e661cb70aba1f5cc1c732fb
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39066517846
- tests: 0 run, 0 failed, 0 skipped

## Gradle failures
```
* What went wrong:
Error : You cannot add Provider instances to the Android SourceSet API.
It is not possible for Android Studio to determine if the Provider points
to a directory that contains generated (read-only) or static (read-write) files. 

Instead you should use the Sources interface in the Variant API, in particular
SourceDirectories.addGeneratedDirectory and SourceDirectories.addStaticDirectories.

```

