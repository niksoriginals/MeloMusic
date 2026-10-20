# Melo — Deep Static Analysis Report

> Date: 2026-10-19  
> Branch: `main`  
> HEAD: `bfdaf4c4 feat(player): update player UI layout, controls, and bottom actions`  
> Working tree at analysis time: modified `AndroidManifest.xml`, `NavigationBuilder.kt`, `SearchableSettings.kt`, `SettingsScreen.kt`; untracked `app/src/main/kotlin/com/niksoriginals/melo/updater/`  
> No project files were modified during this analysis. This document is the only file added.

---

## Table of contents

1. [Scope and method](#1-scope-and-method)
2. [Executive summary and rating](#2-executive-summary-and-rating)
3. [Project structure](#3-project-structure)
4. [Unused / hidden / dead code](#4-unused--hidden--dead-code)
5. [Bugs and correctness issues](#5-bugs-and-correctness-issues)
6. [Security and privacy issues](#6-security-and-privacy-issues)
7. [Performance issues](#7-performance-issues)
8. [Release, CI, and update pipeline](#8-release-ci-and-update-pipeline)
9. [Testing and static analysis](#9-testing-and-static-analysis)
10. [Compiler warnings from the clean build](#10-compiler-warnings-from-the-clean-build)
11. [Prioritized fix list](#11-prioritized-fix-list)
12. [Verification commands and limits](#12-verification-commands-and-limits)

---

## 1. Scope and method

### Scope

- **571 Kotlin files**, **133,633 lines**.
- **10 Gradle modules** (from `settings.gradle.kts`):
  - `app`
  - `innertube`
  - `paxsenixlyrics`
  - `kugou`
  - `betterlyrics`
  - `lrclib`
  - `shazamkit`
  - `artistvideo`
  - `canvas`
  - `applecanvas`
- Kotlin/Compose Android app targeting `compileSdk 36`, `minSdk 26`, `targetSdk 36`.
- Stack: Compose 1.10.2, Kotlin 2.3.10, AGP 9.0.0, Hilt 2.59.1, Room 2.8.4, Media3 1.7.1, WorkManager, Coil 3.3.0, Ktor 3.4.0, R8 minification.

### Method

- Static scan for declarations and references.
- Manual verification of each candidate dead file/member against:
  - Kotlin call sites,
  - Hilt modules,
  - `AndroidManifest.xml`,
  - reflection/string references,
  - ProGuard keep rules.
- Manifest and Gradle/CI inspection.
- Clean Kotlin recompile:

```text
gradlew.bat :app:compileArm64FossDebugKotlin --offline --rerun-tasks --console=plain
BUILD SUCCESSFUL in 5m47s
106 actionable tasks: 106 executed
```

The build succeeded. The updater package is therefore compile-clean in its current state. The report focuses on runtime correctness, dead code, security, and maintainability.

### Limits

- No runtime/device testing was performed.
- Unused resources (drawables, strings, XML) were not exhaustively scanned; `./gradlew lint` is recommended.
- Some “unused” counts are heuristic (for example, Kotlin delegate imports are intentionally excluded from the unused-import count).
- Indirect usage through reflection, DI, or manifest registration was checked where visible, but runtime-only mechanisms can hide references.

---

## 2. Executive summary and rating

Melo is a feature-rich music client with a useful modular skeleton, but it carries a large amount of abandoned or unfinished code, several user-visible navigation/update bugs, and meaningful security/privacy debt. The code compiles cleanly, but correctness currently depends on manual testing because app-level test coverage is near zero.

### Scorecard

| Area | Score | Summary |
|---|---:|---|
| Architecture / modularization | **6/10** | Good module split, but several god-classes and no consistent data layer |
| Code quality / hygiene | **4/10** | ~423 unused imports, ~1,760 lines of wholly dead files, dead features and settings keys |
| Correctness / reliability | **4/10** | Two real navigation bugs, stale WorkManager state, 179 `!!`, 19 swallowed exceptions |
| Security / privacy | **4/10** | Release logging of cookie material, plaintext/backed-up DataStore, global cleartext, embedded secrets, weak signing fallback |
| Performance | **5/10** | `runBlocking` in UI paths, large heap, heavy settings search, 64 MB APK |
| Testing / CI | **2/10** | 4 test files total, none in `app`; CI builds but never tests or lints |
| UX / i18n / accessibility | **5/10** | Many locales, but RTL disabled, new updater UI hardcoded in English, many null `contentDescription` |
| **Overall** | **5/10 (C-)** | Feature-rich and ambitious, but significant dead-code, reliability, and security debt |

### One-paragraph verdict

For a personal or experimental fork, this is workable and impressive in scope. For production distribution, it needs cleanup before shipping: delete the dead feature graveyard, fix the artist-item and update-state bugs, remove release logging/secrets, and add navigation plus updater tests.

---

## 3. Project structure

### Strengths

- The module split is genuinely useful:
  - `innertube` isolates the YouTube API/models.
  - Lyric providers are separate: `lrclib`, `kugou`, `paxsenixlyrics`, `betterlyrics`.
  - `canvas`, `applecanvas`, `artistvideo`, `shazamkit` isolate optional features.
- Modern stack: Compose, Hilt, Room, Media3, WorkManager, Coil, version catalog.
- R8 minification and resource shrinking are enabled for release.
- Release manifest merge succeeds.
- Broad feature set and many locale resources.
- CI and CodeQL workflows exist.

### Weaknesses

- **God classes** (approx.):
  - `MusicService.kt` — ~4,000 lines
  - `YouTube.kt` — ~4,200 lines
  - `Player.kt` — ~3,700 lines
  - `Lyrics.kt` — ~2,100 lines
  - `PlayerMenu.kt` — ~2,000 lines
  - `ListenTogetherManager` — ~1,700 lines
- Playback state, queue logic, network calls, persistence, and UI concerns are mixed.
- **No consistent data layer:** some screens call `YouTube`/DAOs directly; repository boundaries are ad hoc.
- **String-typed navigation:** route literals are scattered across screens (`"settings/update"`, `"update"`, `"artist/..."`). This directly caused two of the bugs found.
- **Settings schema drift:** 48 of 283 preference keys have no live reader, and preference UI exists for features that do not.
- **No static-analysis gate:** no ktlint/detekt, no lint in CI. This is why hundreds of unused imports and a large dead-code pile survive.
- **No app-level tests:** navigation and updater logic — exactly where the bugs are — are untested.

### Module observability note

Five library modules have no direct imports from `app` sources (`canvas`, `applecanvas`, `artistvideo`, `betterlyrics`, `paxsenixlyrics`). Some are reached indirectly through providers/registries:

- `canvas`, `applecanvas`, `artistvideo` are referenced via provider code in player/artist flows.
- `betterlyrics` appears to be referenced only as a registry-name string; verify whether it is actually reachable.
- `paxsenixlyrics` may be resolved via a provider registry; confirm before declaring dead.

---

## 4. Unused / hidden / dead code

### 4.1 Completely unused files (~1,760 lines)

These have exactly one reference each — their own declaration. Hilt- and manifest-referenced components were excluded.

| File | Lines | Note |
|---|---:|---|
| `app/src/main/kotlin/.../ui/screens/ExploreScreen.kt` | 450 | No route, no caller |
| `app/src/main/kotlin/.../ui/component/NewMenuComponents.kt` | 326 | `NewActionButton`, `NewMenuItem` unused |
| `app/src/main/kotlin/.../utils/sabr/EjsNTransformSolver.kt` | 298 | Dead, but ProGuard still keeps `utils.sabr.**` |
| `app/src/main/kotlin/.../utils/LocalFileDownloader.kt` | 131 | Unused |
| `innertube/src/main/kotlin/.../NetworkConfig.kt` | 125 | Unused |
| `app/src/main/kotlin/.../playback/queues/LocalMixQueue.kt` | 68 | Unused |
| `app/src/main/kotlin/.../ui/component/BigSeekBar.kt` | 60 | Unused |
| `app/src/main/kotlin/.../ui/component/NavigationTile.kt` | 59 | Unused |
| `app/src/main/kotlin/.../utils/KeyUtils.kt` | 41 | Unused |
| `app/src/main/kotlin/.../ui/component/LoadingScreen.kt` | 39 | Unused |
| `app/src/main/kotlin/.../data/FlowNeuroengineApi.kt` | 35 | Unused + unused BuildConfig key |
| `app/src/main/kotlin/.../viewmodels/ThemeViewModel.kt` | 34 | Unused |
| `app/src/main/kotlin/.../ui/screens/library/LibraryAlbumsPage.kt` | 27 | Unused |
| `innertube/src/main/kotlin/.../models/AddItemYouTubePlaylistResponse.kt` | 20 | Unused |
| `app/src/main/kotlin/.../ui/theme/Font.kt` + `bbh_bartle.kt` | 20 | Duplicate `BbhBartle` font, both unused |
| `app/src/main/kotlin/.../models/ItemWrapper.kt` | 17 | Unused |
| `innertube/src/main/kotlin/.../renderers/MusicMultiRowImageItemRenderer.kt` | 11 | Unused |

**Total: approximately 1,760 lines across 18 wholly unused files.**

Partially dead files:

- `utils/WebAuthSessionCleaner.kt` — `clearWebAuthSession` is used by Spotify import; `clearPlaybackWebAuthSession` and `resetAuthWebViewSession` are unused.
- `utils/UriUtils.kt` — `safeOpenUri` unused.
- `utils/NetworkUtils.kt` — `isInternetAvailable` unused.
- `utils/LocalMediaIntents.kt` — `shareLocalAudio` unused; `isLocalMediaId` is used.
- `ui/component/GridMenu.kt` — all three grid-menu composables unused.
- `ui/component/AutoResizeText.kt` — unused.
- `ui/component/Preference.kt` — most preference composables unused.
- `ui/component/TopSearch.kt` — `TopSearch` unused.

### 4.2 Dead members hidden inside live files

- `AppNavigationBar` + `NavItemState`:
  - Declared twice: `ui/component/AppNavigation.kt:127` and `ui/component/FloatingNavBar.kt:35`.
  - Only `AppNavigationRail` is used.
- `GridMenu`, `DownloadGridMenu`, `SleepTimerGridMenu`.
- `AutoResizeText`, `TopSearch`, `InfoLabel`.
- `SwitchPreference`, `EditTextPreference`, `SliderPreference`, `PreferenceGroupTitle`.
- `FloatingToolbarOverflowMenuButton`, `FabAction`.
- `WavyShape`, `MoreActionsButton`, `PlayerMoreMenuButton`.
- Legacy mini-player composables:
  - `FavoriteButton`
  - `LegacyMiniPlayer`
  - `LegacyMiniMediaInfo`
  - `LegacyPlayPauseButton`
- `Queue`’s `PlayerQueueButton`.
- Several lyric scroll constants in `ui/component/Lyrics.kt`:
  - `melo_AUTO_SCROLL_DURATION`
  - `melo_INITIAL_SCROLL_DURATION`
  - `melo_SEEK_DURATION`
  - `melo_FAST_SEEK_DURATION`
- `LocalMediaIntents.shareLocalAudio`, `SpotifyToken`, `SpotifySavedAlbum`.

Unused private declarations:

- `MusicService.lastPresenceToken`
- `MusicService.shareSong`
- `PlayerConnection.PLAYER_INIT_TIMEOUT_MS`
- `Shazam.mutex`
- `MeloWidgetManager.database`
- `ScrobbleManager.scope`
- `AppleMusicCanvasProvider.ITUNES_SEARCH_URL`

Compiler-confirmed dead conditions:

- `ui/menu/PlaylistMenu.kt:640` — `if (browseId != null)` is always true (`createPlaylist` returns non-null).
- `utils/YTPlayerUtils.kt:435` — `if (mainPlayerResponse == null)` is always false; the intended guard is ineffective.
- `AudioDeviceBottomSheet.kt:853,919` — redundant `else` on an exhaustive `when`.
- `RecognitionScreen.kt:294` — redundant `else` on an exhaustive `when`.

### 4.3 Dead settings keys (48 of 283)

The settings schema has drifted from the implementation. Representative dead keys by feature group:

#### Scrobbling / LastFM

- `EnableLastFMScrobblingKey`
- `LastFMSessionKey`
- `LastFMUsernameKey`
- LastFM API keys/settings
- **`ScrobbleManager` itself is a no-op and never instantiated.**

#### ListenBrainz

- All ListenBrainz keys.

#### Spatial audio

- `SpatialAudioEnabledKey`
- `SpatialAudioModeKey`
- `SpatialAudioReverbPresetKey`
- `VirtualizerEnabledKey`
- `CrossfeedEnabledKey`

#### Translation / AI

- `AIProviderKey`
- `AITranslationEnabledKey`
- `AIResponseLanguageKey`
- `DeepLApiKey`
- Additional AI provider keys

#### Lossless / auth

- `LOSSLESS_ENABLED`
- GitHub PAT keys

#### Per-provider lyrics

- `LRCLIB_ENABLED`
- `KUGOU_ENABLED`
- `PAXSENIX_ENABLED`
- Other provider toggles

#### Misc

- `IsFirstRunKey`
- `SlimNavBarKey`
- `DeveloperModeKey`
- Legacy `ListenTogether*` keys
- `EnableListenTogetherKey`
- `PersistentQueueEnabledKey` (verify before removal)

These are the clearest sign that features were removed or never finished while UI/schema remained.

### 4.4 Whole features that are dead or stubs

#### Scrobbling is a no-op

`utils/ScrobbleManager.kt` contains a class with empty method bodies. It is never instantiated — even the import in `MusicService.kt:169` is unused. LastFM keys are dead. Either finish it or remove it and the related settings.

#### FlowNeuro engine

- `data/FlowNeuroengineApi.kt` is unused.
- `BuildConfig.FLOW_NEURO_API_KEY` and `FLOW_NEURO_BASE_URL` are unused.

#### `EjsNTransformSolver`

Dead, but explicitly kept in release builds by `proguard-rules.pro`:

```proguard
-keep class com.niksoriginals.melo.utils.sabr.** { *; }
```

This means dead code is retained in the shipped APK.

#### `UpdateDownloadWorker` redirect branch

The manual redirect loop is effectively unreachable because `instanceFollowRedirects = true` (`UpdateDownloadWorker.kt:62,70`). `HttpURLConnection` follows redirects automatically, so the local `300..399` branch normally never runs.

#### `melo://update` deep link

Doubly broken — see section 5.3.

#### Betterlyrics module

No direct app import; appears to be referenced only as a registry-name string. Confirm whether the module is reachable at all.

### 4.5 Imports, duplicates, and resources

#### Unused imports

- **≈423 unused imports across 88 files.**
- Raw scan: 642 hits; the filtered count excludes Kotlin delegate imports such as `getValue`/`setValue`.
- Worst offenders:
  - `ContentSettings.kt` — 26
  - `AudioDeviceBottomSheet.kt` — 25
  - `MainActivity.kt` — 22
  - `Player.kt` — 20
  - `AppearanceSettings.kt` — 20
  - `LibraryPlaylistsScreen.kt` — 19
  - `AlbumScreen.kt` — 16
  - `AboutScreen.kt` — 14

#### Duplicates

- `BbhBartle` font declared twice (`Font.kt`, `bbh_bartle.kt`), both unused.
- `AppNavigationBar` declared in two files.
- Manifest duplicate receivers:
  - `MusicWidgetReceiver` at `AndroidManifest.xml:322` and `:360`.
  - `TurntableWidgetReceiver` at `AndroidManifest.xml:339` and `:389`.
- Manifest duplicate `FileProvider` at `AndroidManifest.xml:252` and `:453`.
- Duplicate `<queries>` blocks in the manifest.

#### Resource-level dead code (not exhaustively scanned)

Run `./gradlew lint` to identify unused drawables, strings, and XML. The project has many feature-specific assets, so this is likely a significant additional cleanup area.

### 4.6 Test coverage

Only **4 test files** exist:

- `innertube`:
  - `MainTest.kt`
  - `SearchVideoTest.kt`
  - `models/RunsTest.kt`
- `canvas`:
  - `TidalCanvasProviderTest.kt`

There is no `app/src/test` and no instrumentation tests for:

- navigation,
- updater,
- playback queue,
- settings search,
- scrobbling (which does not exist),
- Listen Together protocol.

---

## 5. Bugs and correctness issues

### 5.1 HIGH — Artist “See all” route is malformed

**Evidence**

- `ui/screens/NavigationBuilder.kt:235`:

```kotlin
route = "artist/{artistId}/items?browseId={browseId}?params={params}"
```

- `ui/screens/artist/ArtistScreen.kt:821`:

```kotlin
"artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}"
```

**Problem**

A URI has only one query separator (`?`); additional parameters require `&`. The second `?` becomes part of the `browseId` value. As a result:

- `params` is never parsed as its own argument.
- `browseId` may be received as `"<id>?params=<value>"`.
- Artist item pages launched from the artist screen can fetch the wrong data or fail.

**Fix**

```kotlin
route = "artist/{artistId}/items?browseId={browseId}&params={params}"
```

and update the navigation string accordingly.

**Prevention**

Use type-safe Navigation Compose routes or central route builders. Add a navigation smoke test that navigates every declared route.

---

### 5.2 HIGH — Download state reads the wrong `WorkInfo`

**Evidence**

- `updater/UpdateScreen.kt:57-64`:

```kotlin
val workInfos by workManager.getWorkInfosForUniqueWorkFlow("melo_app_update")
    .collectAsState(initial = emptyList())

val currentWork = workInfos.firstOrNull()
val isDownloading = currentWork?.state == WorkInfo.State.RUNNING
val downloadProgress = currentWork?.progress?.getFloat(UpdateDownloadWorker.KEY_PROGRESS, 0f) ?: 0f
val isDownloadSuccess = currentWork?.state == WorkInfo.State.SUCCEEDED
val downloadedFilePath = currentWork?.outputData?.getString(UpdateDownloadWorker.KEY_FILE_PATH)
```

**Problem**

WorkManager returns all `WorkInfo` records associated with a unique work name, including older cancelled, failed, and succeeded runs. `firstOrNull()` selects the oldest record.

Consequences:

- After a retry, the UI can show a cancelled/failed old download as active.
- If an old successful run is first, `isDownloadSuccess` can remain true during a new download.
- The UI can offer to install a stale APK, or show progress from the wrong attempt.

**Fix**

- Track the work ID returned by `enqueueUniqueWork` and match on it, or
- Use the newest relevant record:

```kotlin
val currentWork = workInfos.lastOrNull { it.state != WorkInfo.State.CANCELLED }
```

Also consider filtering by state/tags and handling `BLOCKED`, `ENQUEUED`, and `FAILED` explicitly.

---

### 5.3 HIGH — `melo://update` deep link is broken

**Evidence**

- `MainActivity.kt:1353-1358`:

```kotlin
if (uri.host?.equals("update", ignoreCase = true) == true ||
    uri.pathSegments.firstOrNull() == "update") {
    navController.navigate("update")
    return
}
```

- The only registered update destination is `NavigationBuilder.kt:447-449`:

```kotlin
composable("settings/update") {
    UpdateScreen(navController, scrollBehavior)
}
```

- No `composable("update")` exists anywhere.
- The custom-scheme filter in `AndroidManifest.xml:121-123` combines scheme and host:

```xml
<data android:scheme="melo" />
<data android:scheme="echomusic" />
<data android:host="listen" />
```

Because a host is present, the filter matches only `melo://listen` and `echomusic://listen`. `melo://update` is not delivered to the app at all.

- `MainActivity.kt:1152` checks `currentRoute != "update"`, which is also the wrong route, so the rail would not hide even if navigation succeeded.

**Impact**

- The documented deep link does nothing.
- If an explicit intent/legacy path ever delivers the URI, `navigate("update")` throws `IllegalArgumentException`.
- The rail visibility check is wrong for the in-app update screen.

**Fix**

- Register the `update` host in the manifest if the deep link is intended, or remove the dead branch.
- Navigate to `"settings/update"`.
- Change the route check to `"settings/update"`.

---

### 5.4 MEDIUM — Update version detection is brittle

**Evidence**

- `updater/UpdateManager.kt:144`:

```kotlin
val releaseVersion = releaseObj.optString("name").takeIf { it.isNotBlank() } ?: tagName
```

- `UpdateManager.kt:23-28` parses only dotted integers after stripping `v`/`b`:

```kotlin
val latestClean = latestVersion.removePrefix("b").removePrefix("v").trim()
val currentClean = currentVersion.removePrefix("b").removePrefix("v").trim()
```

- `BuildConfig.VERSION_NAME` is `"1.0.0"` from `app/build.gradle.kts:36`.

**Problem**

The updater uses the GitHub release **title**, not `tag_name` or a structured version field. A title such as `"Melo 1.0.1"` becomes `[0, 0, 0]` during parsing and is considered older than `1.0.0`. The update will silently never appear.

`versionName` is also hardcoded; CI does not bump it.

**Fix**

- Use `tag_name` (or a version field in the release body/changelog).
- Parse with a real semver parser, tolerating prefixes such as `v`, `b`, and product names.
- Centralize `versionName`/`versionCode` and update them in the release workflow.

---

### 5.5 MEDIUM — Update date is displayed in the wrong timezone

**Evidence**

- `UpdateManager.kt:48-54`:

```kotlin
val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
LocalDateTime.parse(dateStr, isoFormatter).format(displayFormatter)
```

**Problem**

The quoted `'Z'` is a literal, not a UTC offset. `published_at` from GitHub looks like `2026-09-03T07:51:42Z`, which is a UTC instant. Parsing it as `LocalDateTime` and formatting it as local time displays the wrong time (off by the device’s UTC offset).

**Fix**

```kotlin
OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    .atZoneSameInstant(ZoneId.systemDefault())
    .format(displayFormatter)
```

or parse as `Instant` and convert to the local zone.

---

### 5.6 MEDIUM — `runBlocking` in UI-facing DataStore access

**Evidence**

- `utils/DataStore.kt:27-38`:

```kotlin
operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? =
    runBlocking(Dispatchers.IO) {
        data.first()[key]
    }

fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>,
    defaultValue: T,
): T =
    runBlocking(Dispatchers.IO) {
        data.first()[key] ?: defaultValue
    }
```

- Similar pattern in `ContextExt.kt:16,22`.
- `MusicService.kt` has 8 `runBlocking` calls, including crossfade logic.
- `MusicDatabase.withTransaction` contains `runBlocking` (around `db/MusicDatabase.kt:75-84`).
- `MusicDatabaseDao.incrementPlayCount` also uses it (`DatabaseDao.kt` around line 1215).
- `SelectionSongsMenu.kt` and `LyricsMenuViewModel.kt` each have 2 more.

**Problem**

`runBlocking(Dispatchers.IO)` still blocks the calling thread while the IO operation runs on another dispatcher. If the caller is the main thread (composition, click handling), the UI freezes. Inside a database transaction it can stall the transaction thread and risks deadlock/starvation.

**Fix**

- Read settings as flows in ViewModels via `stateIn`/`collectAsState`.
- Use `flow.first()` from a coroutine when a one-shot value is needed.
- Remove blocking reads from composition and from `MusicService` hot paths.
- Never call `runBlocking` inside `withTransaction`.

---

### 5.7 MEDIUM — Room queries missing `@Transaction`

**Compiler warning**

```text
DatabaseDao.kt:991: The return value includes a data class with a @Relation.
It is usually desired to annotate this function with @Transaction ...
DatabaseDao.kt:994: same
```

**Evidence**

- `DatabaseDao.kt:987-989` has `@Transaction` on `playlist(playlistId): Flow<Playlist?>`.
- `:990-991` `getPlaylistById` and `:993-994` `getPlaylistByIdBlocking` do **not** have `@Transaction`.

**Problem**

The query returns a `Playlist` with a `@Relation`. Without `@Transaction`, the entity and its related rows may be read from different database snapshots, producing inconsistent results.

**Fix**

Add `@Transaction` to both functions.

---

### 5.8 MEDIUM — Platform-type nullability hazards

The clean build emitted Java type-mismatch warnings where `null` or `String?` is passed to Java APIs inferred as non-null.

#### Changelog

`core/changelog/changelogscreen.kt:158-160`:

```kotlin
val desc = changelogData.optString("description", null)
val imageUrl = changelogData.optString("image", null)
val warning = changelogData.optString("warning", null)
```

Same pattern at `:533-535`.

`JSONObject.optString(name, fallback)` can return `null` when the key is absent and the fallback is null. The compiler warns because the fallback parameter is a platform `String`. The result is then treated as a non-null `String` in Kotlin, which can lead to a later NPE.

#### Commit screen

`core/commitscreen/commitscreen.kt:133,136` — same `optString(..., null)` pattern.

#### Instagram

`instagram/InstagramNotesManager.kt:148`:

```kotlin
?: track.optString("id", clusterId)
```

`clusterId` is `String?`; if it is null and `"id"` is absent, the result can be null while the code treats it as a title/id string.

**Fix**

Use explicit empty-string defaults and `takeIf { it.isNotBlank() }`, or nullable types:

```kotlin
val desc = changelogData.optString("description", "").takeIf { it.isNotBlank() }
```

For `clusterId`, use `clusterId ?: ""` or a nullable chain.

---

### 5.9 MEDIUM — `!!`, swallowed exceptions, and console printing

#### `!!` assertions

- **179** occurrences total.
- Worst files:
  - `YouTube.kt` — 18
  - `HomeScreen.kt` — 14
  - `ListenTogetherClient.kt` — 12
  - `MainActivity.kt` — 9
- Example: `YouTube.kt:1172` checks an empty response and then immediately uses `!!`.
- The compiler flags several `!!` as unnecessary (e.g. `Lyrics.kt:1090`, `LocalSongScreen.kt:649`, `YTPlayerUtils.kt:581,596,626,644`), meaning they are not documenting real invariants.

#### Empty catches

- **19** empty `catch` blocks.
- `Utils/YouTube.kt` — 8
- `PlayerConnection.kt` — 3
- `SearchSuggestions.kt` — 2
- `ListenTogetherClient.kt` — 2
- `MusicService.kt` — 1
- Others.

Empty catches hide network, parsing, and playback failures, turning them into silent wrong behavior.

#### Console output

- **56 `println` calls**:
  - `YouTube.kt` — 28
  - `LibraryPage.kt` — 9
  - `YouTubeUrlParser.kt` — 4
  - `SearchPage.kt` — 3
  - `AppleMusicCanvasProvider.kt` — 3
  - `InnerTube.kt` — 1
- **14 `printStackTrace()` calls.**

`println` in a production Android app is not captured by structured logging and can leak request metadata.

**Fix**

- Replace `!!` on external data with null-safe handling and explicit failure states.
- Log and surface recoverable errors; never swallow without a signal.
- Replace `println`/`printStackTrace` with Timber/structured logging.
- Gate verbose logs behind `BuildConfig.DEBUG`.

---

### 5.10 MEDIUM — Updater robustness

**Evidence**

- `updater/UpdateDownloadWorker.kt`:
  - No SHA-256 verification against a release asset.
  - Integrity checks are only:
    - byte-count match (`:135-140`),
    - APK ZIP magic `PK\x03\x04` (`:142-147`).
  - No `setForeground()` call; the worker is not a foreground worker.
  - Notification permission may be denied; progress is then invisible on Android 13+.
  - `Result.retry()` is returned when `isStopped` (`:106-113`), which can reschedule after cancellation.
- `updater/InstallHelper.kt`:
  - Returns `false` when unknown-sources permission is missing.
  - `UpdateScreen.kt:410` ignores the return value, so the user gets no feedback.
- Cache directory can be cleared by the system, leaving an install path that no longer exists.
- Download URL comes from the GitHub API without host allowlisting.

**Fix**

- Publish and verify a SHA-256 checksum (or verify the APK signature before install).
- Use `setForeground` with a progress notification, or set an explicit notification/permission strategy.
- Do not return `retry` when the work has been cancelled.
- Surface install failures in the UI.
- Handle cache eviction by re-downloading or prompting.
- Restrict the download host to `github.com`/`objects.githubusercontent.com`.

---

### 5.11 MEDIUM — Settings search performance and noise

**Evidence**

- `ui/screens/settings/SettingsScreen.kt:257-262`:

```kotlin
val subSettings = getAllSearchableSettings()
val matchedSubSettings = subSettings
    .filter { it.first.lowercase().contains(searchLower) }
    .map { (title, parentTitle, route) -> ... }
```

- `SearchableSettings.kt` is a **332-line hand-maintained list** of `Triple(stringResource(...), parentTitle, route)`.
- The list is rebuilt on every keystroke when a query is present.
- Entries include generic UI strings (`Cancel`, cache-warning copy, `cd_back`, etc.), so search results are noisy.
- `highlightKey` is appended to every route, but only routes that declare the argument (appearance/player/privacy) use it. On other routes it is silently ignored.

**Fix**

- Move the search index to a static list of resource IDs + route metadata, and resolve strings lazily/remembered.
- Filter on stable keys, not translated display text.
- Exclude non-settings UI strings from the index.
- Add `highlightKey` arguments to the routes that need it, or stop appending it.

---

### 5.12 LOW — i18n, RTL, and accessibility

- **RTL disabled:** `android:supportsRtl="false"` (`AndroidManifest.xml:61`) despite RTL locale resources (Arabic, Hebrew, Persian).
- **Hardcoded English:**
  - `"Cancel Download"` (`UpdateScreen.kt:400`)
  - `"No settings found for ..."` (`SettingsScreen.kt:283`)
  - `"Clear"` (`SettingsScreen.kt:133`)
  - `"Failed to check for updates"` (`UpdateScreen.kt:75`)
  - Updater failure reasons in `UpdateDownloadWorker` (`"Server returned $code"`, `"Redirect failed"`, etc. — these are notification text, only some localized).
- **Null `contentDescription`:** many icons, including navigation and player controls. Some are decorative, but search/settings icons need labels.

**Fix**

- Set `supportsRtl="true"` and audit mirrored layouts.
- Move all new strings to `strings.xml`.
- Add content descriptions for actionable icons.

---

### 5.13 LOW — Manifest and build configuration issues

- **Global cleartext:** `network_security_config.xml:5` has `<base-config cleartextTrafficPermitted="true">` for every host. Only local Listen Together servers plausibly need cleartext.
- **Over-broad FileProvider paths:** `provider_paths.xml:3-11` exposes:
  - `<external-path path=".">`
  - `<cache-path path=".">`
  - `<external-cache-path path=".">`
- **Backup includes DataStore:** `allowBackup="true"` (`AndroidManifest.xml:50`). `backup_rules.xml` and `data_extraction_rules.xml` exclude only ExoPlayer/download files and `exoplayer_internal.db`; they do not exclude `files/datastore/settings.preferences_pb`.
- **`largeHeap="true"`** (`AndroidManifest.xml:62`) and **`extractNativeLibs="true"`** (`:55`) increase memory/install footprint.
- **Exported media service:** `MusicService` is `android:exported="true"` (`:275`) with media session/browse actions. This may be required for media integration, but it deserves a security review.
- **Duplicate manifest entries** (receivers, FileProvider) are tolerated by the merger but brittle.
- **Dependency skew:** `material-icons-extended:1.7.8` is hardcoded at `app/build.gradle.kts:357`, while Compose is `1.10.2` (`gradle/libs.versions.toml:7`). The catalog already defines `compose-material-icons-extended` at line 83.
- **Deprecated Gradle option:** `android.newDsl=false` emits a warning and will be removed in AGP 10.
- **Version fields:** `versionCode = 9`, `versionName = "1.0.0"` are hardcoded. CI does not bump them.

---

### 5.14 LOW — Listen Together remote server configuration

**Evidence**

- `listentogether/ListenTogetherServers.kt:40-64`:

```kotlin
init {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(SERVER_JSON_URL).build()
            val response = client.newCall(request).execute()
            response.body?.string()?.let { jsonString ->
                ...
                _servers.value = listOf(
                    ListenTogetherServer(
                        name = name,
                        url = url,
                        location = region,
                        operator = "Melo"
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback implicitly retained
        }
    }
}
```

**Problems**

- `GlobalScope` is not lifecycle-scoped.
- The server URL is fetched from a GitHub raw file and accepted without validation.
- No scheme validation (`wss://`), no host allowlist.
- Exceptions are silently swallowed.
- The default `server.json` path is under the app source tree in the repo (`app/server.json`), so app releases and server config are coupled.

**Impact**

If the GitHub repository or raw content is compromised, clients can be pointed at an attacker-controlled WebSocket server, exposing Listen Together session data.

**Fix**

- Use an application-scoped coroutine (`SupervisorJob` + `Dispatchers.IO`).
- Validate `wss://` and optionally pin/allowlist the host.
- Log failures.
- Prefer a signed configuration or bundle a default and treat remote config as an enhancement.

---

### 5.15 LOW — Hardcoded English in the new updater

The updater package is new and largely bypasses the app’s localization system:

- `UpdateScreen.kt:400` — `"Cancel Download"`.
- `UpdateScreen.kt:75` — `"Failed to check for updates"`.
- `UpdateDownloadWorker` failure notifications:
  - `"Redirect failed"`
  - `"Server returned $code"`
  - `"Connection failed"`
  - `"Incomplete download"`
  - `"Invalid APK downloaded"`
  - `"Download failed"`

The app has locale folders for many languages; these strings should be resources too.

---

## 6. Security and privacy issues

### 6.1 Release logging of session material

**Evidence**

- `App.kt:107-114`:

```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    // This fork distributes signed APKs that the owner debugs directly on-device
    // (no Play/Crashlytics console), so keep Timber writing to logcat on release too.
    // R8 keeps Timber calls; only android.util.Log is stripped by proguard rules.
    Timber.plant(Timber.DebugTree())
}
```

- `App.kt:344`:

```kotlin
Timber.d("forgetAccount: Before - cookie=${YouTube.cookie?.take(50)}, ...")
```

**Risk**

- The release build intentionally plants a debug logger.
- The first 50 characters of the YouTube session cookie are logged on logout.
- ProGuard’s `-assumenosideeffects` strips `Log.v/d` but keeps `Log.i/w/e`; Timber is not stripped. Relying on R8 to remove this specific call is fragile.
- Debug and hardened builds definitely leak it; any future change to log level or R8 rules can leak it in release.

**Fix**

- Remove the cookie log entirely.
- Never log credentials/tokens, even partially.
- Plant Timber only in debug, or use a redacting release tree.

### 6.2 Secrets embedded in BuildConfig

**Evidence**

`app/build.gradle.kts:42-55`:

```kotlin
buildConfigField("String", "LASTFM_API_KEY", ...)
buildConfigField("String", "LASTFM_SECRET", ...)
buildConfigField("String", "GH_CLIENT_ID", ...)
buildConfigField("String", "GH_CLIENT_SECRET", ...)
buildConfigField("String", "FLOW_NEURO_API_KEY", ...)
```

- `LASTFM_SECRET` belongs to a dead implementation.
- `FLOW_NEURO_API_KEY` belongs to an unused API.
- `GH_CLIENT_SECRET` is a client secret compiled into an APK; anyone can extract it.

**Fix**

- Remove secrets for dead features.
- For OAuth, use PKCE and no client secret in a public client.
- Rotate any secret that has ever shipped in an APK.

### 6.3 Signing key fallback

**Evidence**

- `app/build.gradle.kts:125-127`:

```kotlin
storePassword = envOrNull("STORE_PASSWORD") ?: "melokey123"
keyAlias = envOrNull("KEY_ALIAS") ?: "melo"
keyPassword = envOrNull("KEY_PASSWORD") ?: "melokey123"
```

- `.github/workflows/gradle.yml:59-62`:

```bash
keytool -genkeypair -alias melo ... \
  -storepass melokey123 -keypass melokey123
```

**Risk**

If the release secrets are absent, the build is signed with a publicly known keystore/password. A release signed with a known key undermines update authenticity and lets anyone produce an APK with the same signing identity (subject to distribution).

**Fix**

- Fail the release build if signing secrets are missing.
- Never commit keystores or passwords.
- Rotate keys if a known-password release ever shipped.

### 6.4 Backup and plaintext storage

**Evidence**

- `AndroidManifest.xml:50` — `android:allowBackup="true"`.
- `res/xml/backup_rules.xml` excludes only:
  - `file:./exoplayer`
  - `file:./download`
  - `database:exoplayer_internal.db`
- `res/xml/data_extraction_rules.xml` has the same exclusions for cloud backup and device transfer.
- `utils/DataStore.kt:25` uses plain `preferencesDataStore(name = "settings")`.

**Risk**

- The DataStore preferences file (`files/datastore/settings.preferences_pb`) is not excluded from backup or device transfer.
- It contains account/session preferences (and potentially auth state).
- Data is stored unencrypted inside the app sandbox; backup/root access exposes it.

**Fix**

- Exclude the DataStore file (and any cookie/token files) from both backup rule sets.
- Consider Keystore-backed encryption for tokens/cookies.
- Set `allowBackup="false"` if account data should never leave the device.

### 6.5 Global cleartext traffic

**Evidence**

`res/xml/network_security_config.xml:3-9`:

```xml
<base-config cleartextTrafficPermitted="true">
```

**Risk**

HTTP is allowed for every domain, not just local Listen Together servers. This weakens TLS enforcement and enables downgrade/MITM on any accidental `http://` URL.

**Fix**

- Remove the global `base-config` exception.
- Add a `domain-config` only for local/LAN Listen Together hosts if absolutely necessary.
- Prefer `wss://` everywhere.

### 6.6 Over-broad FileProvider paths

**Evidence**

`res/xml/provider_paths.xml:2-11` exposes entire trees:

```xml
<external-path name="external_files" path="." />
<cache-path name="cache" path="." />
<external-cache-path name="external_cache" path="." />
```

**Risk**

Any component that receives a content URI grant can read broad parts of external/cache storage. The app currently grants URIs for APK installation, but the provider should still be narrow.

**Fix**

- Expose only `cache/updates` (and any other specific export directory).
- Remove external-path entries unless truly needed.

### 6.7 Remote Listen Together config

See 5.14. A compromised remote config can redirect session traffic.

---

## 7. Performance issues

1. **Blocking DataStore reads** — `runBlocking` on `DataStore.data.first()` in `utils/DataStore.kt:27-38` and `ContextExt.kt:16,22`. If called from composition/main thread, this is disk I/O on the UI thread.
2. **Blocking playback paths** — `MusicService.kt` has 8 `runBlocking` calls, including crossfade.
3. **Blocking inside DB transactions** — `MusicDatabase.withTransaction` and `incrementPlayCount` use `runBlocking`.
4. **Settings search** — 332-line `getAllSearchableSettings()` rebuilt on each keystroke; every row calls `stringResource`.
5. **Large heap** — `largeHeap="true"` can hide leaks and increases memory pressure.
6. **`extractNativeLibs="true"`** — larger install footprint and extraction at install time.
7. **Universal APK size** — the release asset is ~64.8 MB; app bundles or per-ABI splits would reduce download size.
8. **Recomposition-heavy player UI** — large composables (`Player.kt`, `PlayerMenu.kt`, `Lyrics.kt`) with many state reads; no evidence of profiling.
9. **`GlobalScope` network call** — `ListenTogetherServers` fetches on a global scope with no lifecycle/cancellation.
10. **`println` in hot network paths** — `YouTube.kt` has 28 `println` calls; string construction and I/O on every request.

---

## 8. Release, CI, and update pipeline

### 8.1 CI does not build what the updater expects

- Updater (`UpdateManager.kt`):
  - `TARGET_TAG = "melo"` (`:18`).
  - Prefers asset `app-universal-foss-release.apk` (`:153,171`).
- CI (`.github/workflows/gradle.yml`):
  - Triggers on tags `v*` (`:6-7`).
  - Builds `assembleUniversalGmsRelease` (`:76`).
  - Renames artifacts to `Melo-${VERSION_NAME}-Universal.apk` (`:84`).
  - Creates a GitHub release only for `refs/tags/v*` (`:105-111`).

**Consequences**

- CI-created releases use `v*` tags; the updater looks for tag `melo`.
- CI produces a GMS APK with a different name; the updater prefers a FOSS asset.
- The currently working updater depends on a manually maintained `melo` release.
- If the fallback “any APK” path is used, FOSS users may be offered the GMS build.

### 8.2 Versioning

- `versionCode = 9`, `versionName = "1.0.0"` are hardcoded.
- CI does not bump them.
- Release titles are read from `RELEASE_INFO.md` and used as the GitHub release name; the updater then uses that name as the version.
- Any non-numeric title breaks update detection (see 5.4).

### 8.3 Signing fallback

See 6.3. The CI creates a keystore with `melokey123` if the secret is missing and `build.gradle.kts` falls back to the same password.

### 8.4 `google-services.json`

The workflow has a step that writes `app/google-services.json` from a secret, but the step’s `if` condition references `HAS_GOOGLE_SERVICES`, which is defined in that same step’s `env`. Depending on GitHub Actions evaluation semantics, this may not behave as intended. The build also always invokes a GMS flavor, so verify that GMS/Cast features are present in CI artifacts.

### 8.5 No tests or lint in CI

- The workflow runs only `assembleUniversalGmsRelease`.
- No `test`, `lint`, `detekt`, or `ktlint`.
- CodeQL exists (`codeql.yml`), which is good, but it does not replace unit/navigation tests or style checks.

---

## 9. Testing and static analysis

### Current state

- 4 test files, none in `app`.
- No navigation route tests.
- No updater tests.
- No playback queue tests.
- No settings search tests.
- No lint/ktlint/detekt configuration found.

### Recommended minimum

1. **Navigation tests**
   - Every route declared in `NavigationBuilder` resolves.
   - `melo://update` and `melo://listen` deep links resolve to the correct destination.
   - Artist items route passes `browseId` and `params` separately.
2. **Updater tests**
   - Version parsing: `v1.2.3`, `1.2.3`, `Melo 1.2.3`, `1.2.3-beta`.
   - Date parsing with `Z`.
   - WorkInfo selection: old cancelled work plus new running work.
   - Asset selection: FOSS vs GMS, missing asset, multiple APKs.
3. **Database tests**
   - Playlist relation consistency with `@Transaction`.
   - `incrementPlayCount` behavior under coroutines.
4. **Static analysis**
   - ktlint/detekt with unused-import and unused-declaration rules.
   - `./gradlew lint` in CI.
   - Treat new warnings as errors for touched modules.

---

## 10. Compiler warnings from the clean build

The clean build succeeded but emitted roughly **90 warning lines**. They are not blockers, but many point to real debt.

### Nullability / platform types

- `core/changelog/changelogscreen.kt:158-160, 533-535` — `optString(..., null)` → `Nothing?`/`String` mismatch.
- `core/commitscreen/commitscreen.kt:133,136` — same.
- `instagram/InstagramNotesManager.kt:148` — `String?` passed where `String` expected.

### Room

- `db/DatabaseDao.kt:991,994` — `@Relation` return without `@Transaction`.

### Dead/redundant conditions

- `ui/menu/PlaylistMenu.kt:640` — condition always true.
- `utils/YTPlayerUtils.kt:435` — condition always false.
- `core/AudioDeviceBottomSheet.kt:853,919` — redundant `else` on exhaustive `when`.
- `ui/screens/recognition/RecognitionScreen.kt:294` — redundant `else`.
- `ui/component/Lyrics.kt:1135,1192,1239,1295,1362,1416` — Elvis operator always returns left operand.
- `ui/screens/playlist/CachePlaylistScreen.kt:467` — same.

### Unnecessary `!!` / safe calls

- `ui/component/Lyrics.kt:1090,1431`
- `ui/component/MetroLyrics.kt:841`
- `ui/screens/library/LocalSongScreen.kt:649`
- `utils/YTPlayerUtils.kt:581,596,626,644`
- Many “unnecessary safe call on a non-null receiver” warnings around `ResponseBody` in network code.

### Deprecated APIs

- `Icons.Filled.VolumeUp` / `VolumeOff` — use AutoMirrored.
- `ClickableText` — use `Text`/`BasicText` with `LinkAnnotation`.
- `hiltViewModel` old package — move to `androidx.hilt.lifecycle.viewmodel.compose`.
- `largeTopAppBarColors`.
- `ModalBottomSheet` `windowInsets`.
- `LyricsAnimationStyle.echomusic_1` — use `melo_1`.
- `WebView.clearFormData` / `clearUsernamePassword`.
- `asFrameworkPaint`.
- `POSITION_OUT_OF_RANGE`.
- `isBluetoothA2dpOn` / `isBluetoothScoOn`.
- `databaseEnabled`.

### Redundant conversion / casts

- `ui/utils/ShowMediaInfo.kt:189,195,210`
- `utils/ComposeToImage.kt:115`

### Gradle / plugin

- `android.newDsl=false` is deprecated (AGP 10 removal).
- `Configuration cache` is not enabled.

---

## 11. Prioritized fix list

### P0 — correctness and releases

1. **Fix the artist route:** `?params` → `&params` in `NavigationBuilder.kt:235` and `ArtistScreen.kt:821`.
2. **Fix updater WorkInfo selection:** use the newest non-cancelled work or match the enqueued work ID.
3. **Fix `melo://update`:** register the host if intended, navigate to `settings/update`, and correct the rail route check.
4. **Align CI and updater:** use `tag_name`/semver, produce the FOSS asset the updater expects, bump version fields, and remove the `melokey123` fallback.
5. **Remove release cookie logging** and secrets for dead features.

### P1 — security and stability

6. Exclude DataStore from backup/device transfer; consider encrypting tokens.
7. Narrow cleartext traffic to local Listen Together hosts.
8. Narrow FileProvider paths to `cache/updates`.
9. Replace `runBlocking` DataStore reads with flows/`first()` inside coroutines.
10. Add `@Transaction` to `DatabaseDao.kt:991,994`.
11. Replace `optString(..., null)` with safe defaults.
12. Remove empty catches; replace `println`/`printStackTrace` with Timber.

### P2 — maintainability and quality

13. Delete the 18 dead files (~1,760 lines) and the dead members listed above.
14. Remove 48 dead preference keys and their UI.
15. Finish or remove Scrobbling/LastFM and FlowNeuro.
16. Remove 423 unused imports; add ktlint/detekt and run lint/test in CI.
17. Add navigation, updater, and database tests.
18. Restore RTL (`supportsRtl="true"`) and move hardcoded strings to resources.
19. Replace the hardcoded `material-icons-extended:1.7.8` with the version-catalog alias.
20. Split the god classes (`MusicService`, `Player`, `YouTube`, `Lyrics`, `PlayerMenu`, `ListenTogetherManager`).

### P3 — polish

21. Remove duplicate manifest receivers/FileProvider and duplicate `AppNavigationBar`/font.
22. Review `largeHeap` and `extractNativeLibs`.
23. Replace the string-typed navigation with a sealed destination type.
24. Add content descriptions for actionable icons.
25. Reduce APK size with per-ABI splits or app bundles.

---

## 12. Verification commands and limits

### Commands used

```text
gradlew.bat :app:processArm64FossDebugMainManifest --offline
gradlew.bat :app:compileArm64FossDebugKotlin --offline --rerun-tasks --console=plain
```

Results:

- Manifest processing: successful.
- Clean Kotlin compile: `BUILD SUCCESSFUL in 5m47s`, 106 tasks executed.
- The build emitted the warnings summarized in section 10.

### What was not verified

- No device/emulator runtime testing.
- No playback, Cast, Listen Together, widget, or updater end-to-end test.
- No exhaustive unused-resource scan.
- No dynamic analysis or network interception.
- No benchmark/profiling of player UI or startup.

### Important caveat

The working tree contains uncommitted changes (`AndroidManifest.xml`, `NavigationBuilder.kt`, `SearchableSettings.kt`, `SettingsScreen.kt`, and the new `updater/` package). The clean compile validates their current source state, but it does not validate runtime behavior.

---

## Appendix A — Quick file-to-issue index

| File | Issue |
|---|---|
| `ui/screens/NavigationBuilder.kt:235` | Malformed artist route (`?params`) |
| `ui/screens/artist/ArtistScreen.kt:821` | Mirrors malformed route |
| `MainActivity.kt:1356` | Navigates to nonexistent `"update"` route |
| `MainActivity.kt:1152` | Checks wrong update route |
| `AndroidManifest.xml:121-123` | `melo://update` host not registered |
| `updater/UpdateScreen.kt:60` | `firstOrNull()` stale WorkInfo |
| `updater/UpdateManager.kt:144` | Version from release title |
| `updater/UpdateManager.kt:49` | `Z` treated as literal |
| `utils/DataStore.kt:27-38` | Blocking DataStore getters |
| `utils/ContextExt.kt:16,22` | Blocking settings access |
| `db/DatabaseDao.kt:991,994` | Missing `@Transaction` |
| `App.kt:107-114,344` | Release logging + cookie prefix |
| `build.gradle.kts:42-55` | Embedded secrets |
| `build.gradle.kts:120-137` | Hardcoded signing fallback |
| `.github/workflows/gradle.yml:59-62` | Generated keystore with known password |
| `.github/workflows/gradle.yml:76` | CI builds GMS, updater expects FOSS |
| `res/xml/provider_paths.xml:3-11` | Over-broad FileProvider paths |
| `res/xml/network_security_config.xml:5` | Global cleartext |
| `res/xml/backup_rules.xml` | DataStore not excluded |
| `ui/screens/settings/SettingsScreen.kt:257-262` | Search index rebuilt per keystroke |
| `ui/screens/settings/SearchableSettings.kt` | 332-line hand-maintained index |
| `utils/ScrobbleManager.kt` | No-op, unused scrobbling |
| `utils/sabr/EjsNTransformSolver.kt` | Dead code kept by ProGuard |
| `listentogether/ListenTogetherServers.kt:40-64` | `GlobalScope` + unvalidated remote URL |
| `ui/menu/PlaylistMenu.kt:640` | Always-true null check |
| `utils/YTPlayerUtils.kt:435` | Always-false null check |

---

## Appendix B — Dead preference keys (observed examples)

The scan found **48 of 283** keys unreferenced. Representative names:

```text
IsFirstRunKey
SlimNavBarKey
DeveloperModeKey
LOSSLESS_ENABLED
EnableLastFMScrobblingKey
EnableListenTogetherKey
LastFMSessionKey
LastFMUsernameKey
SpatialAudioEnabledKey
SpatialAudioModeKey
SpatialAudioReverbPresetKey
VirtualizerEnabledKey
CrossfeedEnabledKey
AIProviderKey
AITranslationEnabledKey
AIResponseLanguageKey
DeepLApiKey
LRCLIB_ENABLED
KUGOU_ENABLED
PAXSENIX_ENABLED
```

Plus all ListenBrainz keys and several legacy Listen Together keys.

Before deleting, re-run the reference scan against the final branch, because some keys may be read through helper indirection or added back by pending changes.

---

## Appendix C — Recommended quality gate

A minimal CI gate that would have caught much of this:

```text
./gradlew lint
./gradlew test
./gradlew :app:compileUniversalFossReleaseKotlin
./gradlew detekt   # or ktlintCheck
```

Suggested rules:

- unused imports = error
- unused private declarations = warning
- `runBlocking` in `app` = warning/review
- `!!` on platform types = warning
- empty `catch` = error
- hardcoded text in Compose = warning
- missing `@Transaction` on `@Relation` queries = error (already compiler warning)
- navigation route test suite = required

---

*End of report.*
