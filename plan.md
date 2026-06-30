# Implementation Plan — YouTube Dashboard ("ytdash"), Android

> Spec-Kit `/plan` artifact. The frozen spec (`spec/spec.md`, `spec/acceptance-criteria.md`) defines
> WHAT; this defines the HOW. Architecture, libraries, and the map widget are my engineering
> decisions and are the thing being evaluated.

## Stack decision (and why)

| Concern | Choice | Rationale |
|---|---|---|
| Language / build | Kotlin 2.0.21, AGP 8.7.3, Gradle 8.14, JDK 17 | Current idiomatic native-Android toolchain; version-catalog (`libs.versions.toml`) for one source of truth. |
| Min / compile / target SDK | 26 / 35 / 35 | Covers the Pixel-6 (API 36) test device and the vast majority of devices; compileSdk 35 per BOOTSTRAP. |
| UI | Jetpack Compose + Material 3 | Declarative, the modern default. `testTag` + `testTagsAsResourceId=true` gives the constitution's stable selectors for free. |
| State management | MVVM, a single `StateFlow<AppUiState>` | Unidirectional, observable view-state (loading/content/empty/error) per constitution §1.3. One activity-scoped `MainViewModel` is the single source of UI truth, which also lets the app-root `external_open_url` banner serve both the list and the map sheet without duplication. |
| DI | **Manual constructor injection** via `AppContainer` | Dependency inversion is satisfied (presentation depends only on the `VideoRepository` interface). Chosen over Hilt/Dagger deliberately: the graph is tiny and fully visible, and it avoids a second annotation processor — fewer moving parts, a more reliable first build. Justified trade-off vs. the v3 reference (Hilt). |
| Networking / JSON | Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization 1.7 | Idiomatic; the serialization converter parses the YouTube-shaped JSON. Base URL is built at **runtime** from the `apiBaseUrl` extra so one build hits mock or real. |
| Persistence (cache) | Room 2.6.1 (KSP) | The local store is the single source of truth (constitution §1.5). Replace-on-refresh + stale-cache fallback on network error (AC-CACHE-01). `position` column preserves API aggregation order across a process restart. |
| Errors / UI state | Sealed `LoadResult` + sealed `ListUiState` | Typed errors; every failure point resolves to a visible state, never a crash (constitution §1.6). |
| Maps | **osmdroid** via `AndroidView` + a native `AssistChip` marker row | OpenStreetMap per spec. osmdroid draws markers on a Canvas → **not reachable** by a black-box driver (constitution §5), so the accessible affordance is a native chip per located video carrying `map_marker`. `map_marker_fallback_used=false`. |
| Images | Coil 2.7 | Idiomatic Compose image loading for thumbnails. |
| Auth | Play Services Auth (Google Sign-In) + a runtime email whitelist | Real Google sign-in outside UI-test-mode; in UI-test-mode `mockAuthEmail` skips the picker and the same `AuthGate` whitelist runs. |

## Layered architecture (constitution §1.1 / §1.2)

```
ui/ (Compose screens + MainViewModel)         ← presentation, depends on domain abstractions only
  └─ RootApp · LoginScreen · HomeScreen · MapScreen · MainViewModel(AppUiState)
domain/ (pure Kotlin, unit-tested)            ← business logic, no Android/data types
  └─ Video · VideoRepository · LoadResult · AuthGate · VideoSort · VideoFilter · SortOption
data/                                         ← data access behind the repository interface
  ├─ remote/  YouTubeApi (Retrofit) · RemoteDataSource (aggregate+paginate+enrich) · DTOs
  ├─ local/   Room: VideoEntity · VideoDao · AppDatabase
  └─ repo/    VideoRepositoryImpl (network → store → UI; stale-cache fallback)
config/  TestConfig (launch-extra contract) · ChannelsLoader (assets/channels.json)
di/      AppContainer (manual wiring; builds the repo from the runtime TestConfig)
```

A change in one layer does not force edits in the others: the UI knows only `VideoRepository` +
domain models; the data layer knows nothing about Compose.

## Key contract implementations
- **Selectors (§3):** `Modifier.testTag(id)` everywhere + `testTagsAsResourceId=true` on the root Box.
  The list-item id and the `external_open_url`/`detail_video_url` ids sit on the **title/URL `Text`**
  (not the row/Surface) so the id and asserted text resolve to the same unmerged-semantics node.
- **UI-test-mode (§4):** `TestConfig.fromIntent` reads `uiTestMode`, `mockAuthEmail`, `apiBaseUrl`,
  `apiKey`, `authorizedEmails`, `captureExternalLinks` (robust to boolean-or-string extras). `apiKey`
  and `apiBaseUrl` are read at runtime, never baked in.
- **Overlays (§5a):** logout is a plain top-bar button; the map detail sheet is an inline `Surface`
  (NOT `ModalBottomSheet`); the external banner is at the app root — all in the main composition.
- **Markers (§5):** native chip affordance as above.

## Anti-overfit
The data layer iterates **every** configured channel from `assets/channels.json`, follows
`nextPageToken` to exhaustion, dedupes by videoId preserving order, and enriches locations via
`videos.list`. Nothing is hardcoded to the fixture: counts, ids, titles, categories (= source-channel
label), and locations all come from API responses. There is no catch-all shortcut.

## Validation
Maestro `flows/AC-*.yaml` against the running mock on device `25251FDF60029V` (with
`adb reverse tcp:8090`), plus JVM unit tests (`AuthGate`/`VideoSort`/`VideoFilter`) and an
instrumented Room read/write test.
