# BUILD-REPORT — YouTube Dashboard ("ytdash"), Android (native Compose)

## Summary
A production-quality native-Android app built against the frozen spec, validated end-to-end with the
hidden Maestro harness. **All 14 acceptance criteria pass** against the running mock.

- applicationId: `com.example.ytdash`
- Validated on device `25251FDF60029V` (Pixel 6, API 36) against the mock at `http://127.0.0.1:8090`
  (reached via `adb reverse tcp:8090 tcp:8090`).
- Build: `./gradlew :app:assembleDebug` (Gradle 8.14, JDK 17). APK at
  `app/build/outputs/apk/debug/app-debug.apk`.

## Stack chosen (see `plan.md` for the full rationale)
| Concern | Choice |
|---|---|
| Language / build | Kotlin 2.0.21, AGP 8.7.3, Gradle 8.14, JDK 17, minSdk 26 / compile+target 35 |
| UI | Jetpack Compose + Material 3 |
| State | MVVM, one activity-scoped `MainViewModel` exposing a single `StateFlow<AppUiState>` |
| DI | Manual constructor injection (`AppContainer`) behind the `VideoRepository` interface |
| Networking | Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization (runtime-configurable base URL + key) |
| Persistence | Room 2.6.1 (replace-on-refresh, stale-cache fallback, order-preserving) |
| Maps | osmdroid (OpenStreetMap) via `AndroidView` + a native accessible `map_marker` chip row |
| Images / Auth | Coil 2.7 / Play Services Google Sign-In + runtime email whitelist |

## Acceptance-criteria result (14/14)
| AC | Result | AC | Result |
|---|---|---|---|
| AC-LOGIN-01 | ✅ | AC-CACHE-01 | ✅ |
| AC-LOGIN-02 | ✅ | AC-FILTER-01 | ✅ |
| AC-LOGIN-03 | ✅ | AC-SORT-01 | ✅ |
| AC-LIST-01 | ✅ | AC-MAP-01 | ✅ |
| AC-LIST-02 | ✅ | AC-MAP-02 | ✅ |
| AC-LIST-03 | ✅ | AC-MAP-03 | ✅ |
| AC-COUNT-01 | ✅ | AC-LINK-01 | ✅ |

Stability: the full suite was run 3× (`results/run-final2.xml`, `results/run-stability-2.xml`,
`results/run-stability-3.xml`) → **min/median/max = 14/14 each**. JUnit XML in `results/`.

Also green: JVM unit tests (`AuthGate`/`VideoSort`/`VideoFilter`, `./gradlew testDebugUnitTest`) and an
instrumented Room read/write test (`VideoDaoTest`).

## How the contracts were honored
- **Selectors (§3):** `Modifier.testTag(id)` + `testTagsAsResourceId=true` on the root. The
  `video_list_item`, `external_open_url`, and `detail_video_url` ids sit on the **title/URL `Text`** so
  the id and the asserted text resolve to the *same* unmerged-semantics node (the Compose asymmetry
  called out in cross-framework §D.4).
- **UI-test-mode (§4):** `TestConfig.fromIntent` reads all six extras; `apiBaseUrl`/`apiKey` are read at
  runtime so one build serves the mock and the real API. Robust to Maestro delivering booleans as
  Boolean or String.
- **Overlays (§5a):** logout is a plain top-bar action; the map detail sheet is an inline `Surface`
  (not `ModalBottomSheet`); the `external_open_url`/`external_open_error` banner lives at the app root —
  all in the main composition, so no separate-window id loss.
- **Markers (§5):** osmdroid renders the 5 OSM pins (Canvas → not reachable), and the accessible
  affordance is a native `AssistChip` row carrying `map_marker`, one per located video.
  `map_marker_fallback_used = false`.
- **Pagination / aggregation (anti-overfit):** the data layer reads channels from
  `assets/channels.json`, fetches each channel (channels in parallel, pages sequential), follows
  `nextPageToken` to exhaustion, dedupes by videoId preserving order, then enriches locations via
  `videos.list`. No fixture values are hardcoded; `category` = the source-channel label; no catch-all.

## Real-API mode
The same code path runs against real YouTube by swapping only the runtime extras: `apiBaseUrl` →
`https://www.googleapis.com` and `apiKey` → a real key (the DTOs mirror the real v3 signatures, so
parsing is unchanged). Outside UI-test-mode the login button launches real Google Sign-In
(Play Services), and the email whitelist (`elaine.batista1105@gmail.com,edbpmc@gmail.com`, overridable)
gates entry. External "open in YouTube" performs a real `ACTION_VIEW` launch and surfaces
`external_open_error` if it ever fails — never crashes. (A production `google-services.json` was not
present in the workspace; the scored harness exercises only UI-test-mode, where sign-in is deterministic
via `mockAuthEmail`. The Google Sign-In path is wired and compiles, requiring only a real OAuth client
config to exercise interactively.)

## Notable engineering decisions / deviations
1. **Manual DI instead of Hilt** (the v3 reference's choice). Dependency inversion is fully preserved
   (presentation depends only on `VideoRepository`); the graph is small and explicit, avoiding a second
   annotation processor and a class of first-build failures. Documented as a deliberate trade-off.
2. **Progressive two-phase load.** The list renders right after the search aggregation, then locations
   are added from `videos.list`. This both improves UX and made the timing-sensitive AC-LIST-03 (which
   asserts `video_list_item` with no extended wait) fast and reliable; channel fetches run in parallel.
3. **State-based navigation** (a `Screen` enum in the single view-state) rather than a navigation
   library — appropriate for a 3-screen app and keeps the single-source-of-truth state model clean.

## Secrets
No secrets committed. The API key is read at runtime from the `apiKey` extra, or at build time from the
gitignored `config/secrets.env` into `BuildConfig` (default empty). `.gitignore` covers
`config/secrets.env` and `local.properties`.
