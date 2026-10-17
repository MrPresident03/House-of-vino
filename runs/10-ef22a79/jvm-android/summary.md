# jvm-android: failure

- commit: ef22a79f93aef6d40554101b23bcb0d662ddebaf
- run: https://github.com/MrPresident03/House-of-vino/actions/runs/39058269762
- tests: 151 run, 0 failed, 0 skipped

## Compiler errors (216)
```
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/App.kt:123:74 Unresolved reference 'pendingOps'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:15:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:16:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:17:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:18:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:27:34 Unresolved reference 'SyncEngine'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:28:8 Unresolved reference 'io'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:20 Unresolved reference 'SupabaseClient'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:56 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:56 Candidate 'fun <T> T.takeIf(predicate: (T) -> Boolean): T?' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:63 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:68 Unresolved reference 'isConfigured'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:84 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:84 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:90 Unresolved reference 'createBackend'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:62:18 Unresolved reference 'SupabaseClient'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:18 Unresolved reference 'AccountService'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:49 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:51 Unresolved reference 'AccountService'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:23 Unresolved reference 'PhotoStorage'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:52 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:54 Unresolved reference 'PhotoStorage'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:21 Unresolved reference 'SyncEngine'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:50 Unresolved reference 'SyncEngine'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:65 Unresolved reference 'SupabaseSyncRemote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:4:34 Unresolved reference 'AuthRequiredException'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:5:34 Unresolved reference 'SyncStatus'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:6:34 Unresolved reference 'TransientSyncException'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:23:17 Unresolved reference 'SyncStatus'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:23:30 Unresolved reference 'SyncStatus'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:31:120 Unresolved reference 'currentUser'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:48:37 Unresolved reference 'status'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:48:54 Cannot infer type for value parameter 's'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:49:24 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:49:24 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:49:24 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:49:30 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:51:25 Unresolved reference 'user'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:51:40 Cannot infer type for value parameter 'user'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:53:39 Suspension functions can only be called within coroutine body.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:70:21 Unresolved reference 'currentUser'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:72:21 Unresolved reference 'bootstrap'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:73:20 Unresolved reference 'sync'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:74:33 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:74:33 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:74:33 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:74:39 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:30 Unresolved reference 'syncFiles' on receiver of type 'PhotoRepository'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:103 Cannot infer type for value parameter 'id'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:107 Cannot infer type for value parameter 'bytes'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:77:51 Unresolved reference 'KEY_LAST_SYNC'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:80:21 Unresolved reference 'TransientSyncException'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:83:21 Unresolved reference 'AuthRequiredException'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:87:20 Unresolved reference 'refreshStatus'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/platform/PlatformServices.kt:8:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/platform/PlatformServices.kt:27:18 Unresolved reference 'BackendConfig'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:50:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:113:43 Unresolved reference 'currentUser'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:114:67 Unresolved reference 'email' on receiver of type 'Any'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:171:42 Unresolved reference 'signIn'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:172:42 Unresolved reference 'signUp'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:173:44 Unresolved reference 'sendCode'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:174:46 Unresolved reference 'verifyCode'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:175:43 Unresolved reference 'signOut'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:198:38 Unresolved reference 'currentUser' on receiver of type 'Any'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:200:43 Unresolved reference 'email' on receiver of type 'Any'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:232:50 Unresolved reference 'Membership'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:232:63 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:232:63 Argument type mismatch: actual type is 'List<uninferred T (of fun <T> emptyList)>', but 'List<??? (Unresolved qualified name: Membership)>' was expected.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:233:33 Unresolved reference 'Membership'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:243:94 Assignment type mismatch: actual type is 'String', but 'List<??? (Unresolved qualified name: Membership)>' was expected.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:243:111 Unresolved reference 'memberships'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:246:39 Unresolved reference 'invite'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:250:35 Unresolved reference 'acceptInvite'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:270:32 Unresolved reference 'currentUser'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:31 Unresolved reference 'Membership'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:36 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:46 Unresolved reference 'name'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:58 Unresolved reference 'role'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:77 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:99 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:420:41 Unresolved reference 'deleteAccount'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:44 Unresolved reference 'outboxSummary' on receiver of type 'LocalQueries'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:82 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:91 Cannot infer type for type parameter 'A'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:91 Cannot infer type for type parameter 'B'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:94 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:89:43 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:89:66 Unresolved reference 'resolveKeepLocal'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:90:68 Unresolved reference 'resolveKeepServer'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:91:36 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:91:59 Unresolved reference 'retryFailed'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:113:37 Unresolved reference 'running'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:114:50 Unresolved reference 'lastSuccessAt'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:114:65 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:114:65 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:114:69 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:120:139 Unresolved reference 'lastError'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/App.kt:123:74 Unresolved reference 'pendingOps'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:15:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:16:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:17:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:18:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:27:34 Unresolved reference 'SyncEngine'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:28:8 Unresolved reference 'io'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:20 Unresolved reference 'SupabaseClient'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:56 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:56 Candidate 'fun <T> T.takeIf(predicate: (T) -> Boolean): T?' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:63 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:68 Unresolved reference 'isConfigured'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:84 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:84 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:43:90 Unresolved reference 'createBackend'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:62:18 Unresolved reference 'SupabaseClient'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:18 Unresolved reference 'AccountService'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:45 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:49 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:63:51 Unresolved reference 'AccountService'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:23 Unresolved reference 'PhotoStorage'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:48 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:52 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:64:54 Unresolved reference 'PhotoStorage'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:21 Unresolved reference 'SyncEngine'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:44 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:50 Unresolved reference 'SyncEngine'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/AppGraph.kt:65:65 Unresolved reference 'SupabaseSyncRemote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:4:34 Unresolved reference 'AuthRequiredException'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:5:34 Unresolved reference 'SyncStatus'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:6:34 Unresolved reference 'TransientSyncException'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:23:17 Unresolved reference 'SyncStatus'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:23:30 Unresolved reference 'SyncStatus'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:31:120 Unresolved reference 'currentUser'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:48:37 Unresolved reference 'status'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:48:54 Cannot infer type for value parameter 's'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:49:24 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:49:24 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:49:24 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:49:30 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:51:25 Unresolved reference 'user'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:51:40 Cannot infer type for value parameter 'user'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:53:39 Suspension functions can only be called within coroutine body.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:70:21 Unresolved reference 'currentUser'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:72:21 Unresolved reference 'bootstrap'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:73:20 Unresolved reference 'sync'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:74:33 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:74:33 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:74:33 Candidate 'fun <T, R> T.let(block: (T) -> R): R' is inapplicable because of a receiver type mismatch.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:74:39 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:30 Unresolved reference 'syncFiles' on receiver of type 'PhotoRepository'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:103 Cannot infer type for value parameter 'id'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:75:107 Cannot infer type for value parameter 'bytes'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:77:51 Unresolved reference 'KEY_LAST_SYNC'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:80:21 Unresolved reference 'TransientSyncException'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:83:21 Unresolved reference 'AuthRequiredException'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/SyncCoordinator.kt:87:20 Unresolved reference 'refreshStatus'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/platform/PlatformServices.kt:8:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/platform/PlatformServices.kt:27:18 Unresolved reference 'BackendConfig'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:50:29 Unresolved reference 'remote'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:113:43 Unresolved reference 'currentUser'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:114:67 Unresolved reference 'email' on receiver of type 'Any'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:171:42 Unresolved reference 'signIn'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:172:42 Unresolved reference 'signUp'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:173:44 Unresolved reference 'sendCode'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:174:46 Unresolved reference 'verifyCode'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:175:43 Unresolved reference 'signOut'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:198:38 Unresolved reference 'currentUser' on receiver of type 'Any'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:200:43 Unresolved reference 'email' on receiver of type 'Any'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:232:50 Unresolved reference 'Membership'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:232:63 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:232:63 Argument type mismatch: actual type is 'List<uninferred T (of fun <T> emptyList)>', but 'List<??? (Unresolved qualified name: Membership)>' was expected.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:233:33 Unresolved reference 'Membership'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:243:94 Assignment type mismatch: actual type is 'String', but 'List<??? (Unresolved qualified name: Membership)>' was expected.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:243:111 Unresolved reference 'memberships'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:246:39 Unresolved reference 'invite'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:250:35 Unresolved reference 'acceptInvite'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:270:32 Unresolved reference 'currentUser'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:31 Unresolved reference 'Membership'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:36 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:46 Unresolved reference 'name'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:58 Unresolved reference 'role'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:77 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:275:99 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SettingsScreens.kt:420:41 Unresolved reference 'deleteAccount'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:44 Unresolved reference 'outboxSummary' on receiver of type 'LocalQueries'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:82 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:91 Cannot infer type for type parameter 'A'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:91 Cannot infer type for type parameter 'B'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:67:94 Unresolved reference 'it'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:89:43 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:89:66 Unresolved reference 'resolveKeepLocal'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:90:68 Unresolved reference 'resolveKeepServer'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:91:36 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:91:59 Unresolved reference 'retryFailed'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:113:37 Unresolved reference 'running'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:114:50 Unresolved reference 'lastSuccessAt'.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:114:65 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:114:65 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:114:69 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/ui/settings/SyncAndOnboarding.kt:120:139 Unresolved reference 'lastError'.
```

## Gradle failures
```
* What went wrong:
Execution failed for task ':composeApp:compileKotlinJvm' (registered in build file 'composeApp/build.gradle.kts').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_REFERENCE
       Kotlin compiler error
         Unresolved reference 'pendingOps'.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/App.kt line 123

---
* What went wrong:
Execution failed for task ':composeApp:compileAndroidMain' (registered by plugin 'com.android.internal.kotlin.multiplatform.library').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Kotlin compiler: UNRESOLVED_REFERENCE
       Kotlin compiler error
         Unresolved reference 'pendingOps'.
         Location: /home/runner/work/House-of-vino/House-of-vino/composeApp/src/commonMain/kotlin/com/houseofvino/app/App.kt line 123

```

