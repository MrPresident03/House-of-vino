# jvm-android: failure

- commit: 05b9b9162394003a02371098e77c1a52814c653f
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39014295459
- tests: 129 run, 3 failed, 0 skipped

## Gradle failures
```
* What went wrong:
Execution failed for task ':composeApp:jvmTest'.
> There were failing tests. See the report at: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/build/reports/tests/jvmTest/index.html

* Try:
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

* Exception is:
```

## Failed tests (3)

### com.houseofvino.app.CriticalFlowsTest.firstRunAddWineWithBottlesThenDrinkOneWithANote[jvm]
```
java.lang.IllegalStateException: Method setCurrentState must be called on the main thread
java.lang.IllegalStateException: Method setCurrentState must be called on the main thread
	at androidx.lifecycle.LifecycleRegistry.enforceMainThreadIfNeeded(LifecycleRegistry.kt:382)
	at androidx.lifecycle.LifecycleRegistry.setCurrentState(LifecycleRegistry.kt:139)
	at androidx.navigation.internal.NavBackStackEntryImpl.updateState$navigation_common(NavBackStackEntryImpl.kt:109)
	at androidx.navigation.internal.NavBackStackEntryImpl.setMaxLifecycle$navigation_common(NavBackStackEntryImpl.kt:86)
	at androidx.navigation.NavBackStackEntry.setMaxLifecycle(NavBackStackEntry.nonAndroid.kt:130)
	at androidx.navigation.internal.NavControllerImpl.popEntryFromBackStack$navigation_runtime(NavControllerImpl.kt:612)
	at androidx.navigation.internal.NavControllerImpl.executePopOperations$lambda$0(NavControllerImpl.kt:488)
	at androidx.navigation.internal.NavControllerImpl.pop$navigation_runtime(NavControllerImpl.kt:247)
	at androidx.navigation.NavController$NavControllerNavigatorState.pop(NavController.nonAndroid.kt:102)
	at androidx.navigation.NavigatorState.popWithTransition(NavigatorState.kt:141)
	at androidx.navigation.NavController$NavControllerNavigatorState.popWithTransition(NavController.nonAndroid.kt:106)
	at androidx.navigation.compose.ComposeNavigator.popBackStack(ComposeNavigator.kt:66)
	at androidx.navigation.internal.NavControllerImpl.popBackStackInternal$navigation_runtime(NavControllerImpl.kt:202)
	at androidx.navigation.internal.NavControllerImpl.executePopOperations$navigation_runtime(NavControllerImpl.kt:485)
	at androidx.navigation.internal.NavControllerImpl.popBackStackInternal$navigation_runtime(NavControllerImpl.kt:409)
	at androidx.navigation.internal.NavControllerImpl.navigate$navigation_runtime(NavControllerImpl.kt:1174)
	at androidx.navigation.internal.NavControllerImpl.navigate$navigation_runtime(NavControllerImpl.kt:1552)
	at androidx.navigation.internal.NavControllerImpl.navigate$navigation_runtime(NavControllerImpl.kt:1570)
	at androidx.navigation.internal.NavControllerImpl.navigate$navigation_runtime$default(NavControllerImpl.kt:1565)
	at androidx.navigation.internal.NavControllerImpl.navigate$navigation_runtime(NavControllerImpl.kt:1562)
	at androidx.navigation.NavController.navigate(NavController.nonAndroid.kt:378)
	at com.houseofvino.app.ui.navigation.AppNavHostKt.AppNavHost$lambda$1$0$0$0$0(AppNavHost.kt:57)
	at com.houseofvino.app.ui.settings.SyncAndOnboardingKt.FirstRunScreen$finish(SyncAndOnboarding.kt:173)
	at com.houseofvino.app.ui.settings.SyncAndOnboardingKt.FirstRunScreen$lambda$6$3$0(SyncAndOnboarding.kt:182)
```

### com.houseofvino.app.CriticalFlowsTest.misspeltSearchStillFindsTheWine[jvm]
```
java.lang.IllegalStateException: State must be at least 'CREATED' to be moved to 'DESTROYED' in component NavBackStackEntry(d1a98b2d-751c-4294-a2dc-9d0500c94292) destination=Destination route=com.houseofvino.app.ui.navigation.SearchRoute
java.lang.IllegalStateException: State must be at least 'CREATED' to be moved to 'DESTROYED' in component NavBackStackEntry(d1a98b2d-751c-4294-a2dc-9d0500c94292) destination=Destination route=com.houseofvino.app.ui.navigation.SearchRoute
	at androidx.lifecycle.LifecycleRegistryKt.checkLifecycleStateTransition(LifecycleRegistry.kt:435)
	at androidx.lifecycle.LifecycleRegistryKt.access$checkLifecycleStateTransition(LifecycleRegistry.kt:1)
	at androidx.lifecycle.LifecycleRegistry.moveToState(LifecycleRegistry.kt:170)
	at androidx.lifecycle.LifecycleRegistry.setCurrentState(LifecycleRegistry.kt:140)
	at androidx.navigation.internal.NavBackStackEntryImpl.updateState$navigation_common(NavBackStackEntryImpl.kt:107)
	at androidx.navigation.internal.NavBackStackEntryImpl.handleLifecycleEvent$navigation_common(NavBackStackEntryImpl.kt:92)
	at androidx.navigation.NavBackStackEntry.handleLifecycleEvent(NavBackStackEntry.nonAndroid.kt:135)
	at androidx.navigation.internal.NavControllerImpl.lifecycleObserver$lambda$0(NavControllerImpl.kt:153)
	at androidx.lifecycle.LifecycleRegistry$ObserverWithState.dispatchEvent(LifecycleRegistry.kt:394)
	at androidx.lifecycle.LifecycleRegistry.backwardPass$lambda$0(LifecycleRegistry.kt:329)
	at androidx.lifecycle.FastSafeIterableMap.forEachReversed(FastSafeIterableMap.kt:159)
	at androidx.lifecycle.LifecycleRegistry.backwardPass(LifecycleRegistry.kt:322)
	at androidx.lifecycle.LifecycleRegistry.sync(LifecycleRegistry.kt:363)
	at androidx.lifecycle.LifecycleRegistry.moveToState(LifecycleRegistry.kt:180)
	at androidx.lifecycle.LifecycleRegistry.handleLifecycleEvent(LifecycleRegistry.kt:157)
	at androidx.compose.ui.test.SkikoComposeUiTest.closeScene(ComposeUiTest.skiko.kt:345)
	at androidx.compose.ui.test.SkikoComposeUiTest.access$closeScene(ComposeUiTest.skiko.kt:139)
	at androidx.compose.ui.test.SkikoComposeUiTest$withScene$2.invoke(ComposeUiTest.skiko.kt:289)
	at androidx.compose.ui.test.SkikoComposeUiTest$withScene$2.invoke(ComposeUiTest.skiko.kt:289)
	at androidx.compose.ui.test.DesktopSynchronization_desktopKt.runOnUiThread$lambda$0(DesktopSynchronization.desktop.kt:32)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)
	at java.desktop/java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:308)
	at java.desktop/java.awt.EventQueue.dispatchEventImpl(EventQueue.java:773)
	at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:720)
```

### com.houseofvino.app.CriticalFlowsTest.searchFindsPartialWordsAndToleratesTypos[jvm]
```
java.lang.IllegalStateException: State must be at least 'CREATED' to be moved to 'DESTROYED' in component NavBackStackEntry(68f0e7c0-f553-4883-9c16-3b1037e75b72) destination=Destination route=com.houseofvino.app.ui.navigation.SearchRoute
java.lang.IllegalStateException: State must be at least 'CREATED' to be moved to 'DESTROYED' in component NavBackStackEntry(68f0e7c0-f553-4883-9c16-3b1037e75b72) destination=Destination route=com.houseofvino.app.ui.navigation.SearchRoute
	at androidx.lifecycle.LifecycleRegistryKt.checkLifecycleStateTransition(LifecycleRegistry.kt:435)
	at androidx.lifecycle.LifecycleRegistryKt.access$checkLifecycleStateTransition(LifecycleRegistry.kt:1)
	at androidx.lifecycle.LifecycleRegistry.moveToState(LifecycleRegistry.kt:170)
	at androidx.lifecycle.LifecycleRegistry.setCurrentState(LifecycleRegistry.kt:140)
	at androidx.navigation.internal.NavBackStackEntryImpl.updateState$navigation_common(NavBackStackEntryImpl.kt:107)
	at androidx.navigation.internal.NavBackStackEntryImpl.handleLifecycleEvent$navigation_common(NavBackStackEntryImpl.kt:92)
	at androidx.navigation.NavBackStackEntry.handleLifecycleEvent(NavBackStackEntry.nonAndroid.kt:135)
	at androidx.navigation.internal.NavControllerImpl.lifecycleObserver$lambda$0(NavControllerImpl.kt:153)
	at androidx.lifecycle.LifecycleRegistry$ObserverWithState.dispatchEvent(LifecycleRegistry.kt:394)
	at androidx.lifecycle.LifecycleRegistry.backwardPass$lambda$0(LifecycleRegistry.kt:329)
	at androidx.lifecycle.FastSafeIterableMap.forEachReversed(FastSafeIterableMap.kt:159)
	at androidx.lifecycle.LifecycleRegistry.backwardPass(LifecycleRegistry.kt:322)
	at androidx.lifecycle.LifecycleRegistry.sync(LifecycleRegistry.kt:363)
	at androidx.lifecycle.LifecycleRegistry.moveToState(LifecycleRegistry.kt:180)
	at androidx.lifecycle.LifecycleRegistry.handleLifecycleEvent(LifecycleRegistry.kt:157)
	at androidx.compose.ui.test.SkikoComposeUiTest.closeScene(ComposeUiTest.skiko.kt:345)
	at androidx.compose.ui.test.SkikoComposeUiTest.access$closeScene(ComposeUiTest.skiko.kt:139)
	at androidx.compose.ui.test.SkikoComposeUiTest$withScene$2.invoke(ComposeUiTest.skiko.kt:289)
	at androidx.compose.ui.test.SkikoComposeUiTest$withScene$2.invoke(ComposeUiTest.skiko.kt:289)
	at androidx.compose.ui.test.DesktopSynchronization_desktopKt.runOnUiThread$lambda$0(DesktopSynchronization.desktop.kt:32)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)
	at java.desktop/java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:308)
	at java.desktop/java.awt.EventQueue.dispatchEventImpl(EventQueue.java:773)
	at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:720)
```
