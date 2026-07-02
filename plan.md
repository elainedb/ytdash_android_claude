# Implementation Plan: ytdash (Android)

**Date**: 2026-07-02 | **Spec**: `spec/spec.md` + `spec/acceptance-criteria.md` | **Constitution**: `spec/constitution.md`

## Summary
Native Android app (Kotlin + Jetpack Compose) that signs a user in with Google, aggregates
videos across the configured YouTube source channels (`config/channels.json`), caches them
locally, lets the user filter/sort, and plots geolocated videos on an OpenStreetMap map. Built
test-first against the UI-test-mode contract so the shared Maestro flow set drives the compiled
APK deterministically.

## Technical Context
**Language/Version**: Kotlin 2.0.x, JDK 17
**Primary Dependencies**: Jetpack Compose + Material3, Hilt (DI), Retrofit2 + OkHttp3 +
kotlinx.serialization (network/JSON), Room (persistence), osmdroid (OSM map), Coil (image
loading), Play Services Auth `GoogleSignInClient` (real Google sign-in), Coroutines/Flow.
**Storage**: Room (SQLite) — single source of truth for the video list; `SharedPreferences` for
small auth/session state.
**Testing**: JUnit5/JUnit4 + kotlinx-coroutines-test for domain unit tests (whitelist, sort,
filter) and a Room in-memory-DB persistence test. Maestro (external, black-box) for the 12 ACs —
already provided in `flows/`.
**Target Platform**: Android (minSdk 26, compileSdk 35, targetSdk 35), phones.
**Project Type**: mobile-app (single Gradle module `app/`).
**Performance Goals**: cold list render < 2s against mock; no ANRs (no network/disk on main thread).
**Constraints**: offline-capable (stale-cache fallback), no secrets in source control, base URL +
API key overridable at runtime via intent extras.
**Scale/Scope**: ~4 screens (login, home/list, map, and modal surfaces for filter/sort/detail),
4 source channels, single fixture-sized dataset in dev, unbounded in production.

## Constitution Check
*GATE — re-checked after design below.*
- **Layered separation (§1.1)**: `data/` (remote `YouTubeApi` + `VideoDao`/Room + `VideoRepository`),
  `domain/` (whitelist check, sort/filter comparators — plain Kotlin, no Android deps),
  `ui/` (Compose screens + `ViewModel`s exposing sealed `UiState`). PASS.
- **Dependency inversion (§1.2)**: ViewModels depend on `VideoRepository`/`AuthRepository`
  interfaces; Hilt binds the impls. PASS.
- **Unidirectional observable state (§1.3)**: each screen has one `StateFlow<UiState>` (sealed
  `Loading/Content/Empty/Error`); Compose collects via `collectAsStateWithLifecycle`. PASS.
- **No blocking work on UI thread (§1.4)**: Retrofit suspend fns + Room suspend/Flow queries, all
  called from `viewModelScope` (Dispatchers.IO for repository work). PASS.
- **Single source of truth (§1.5)**: Room `videos` table is what the list/map/filter/sort read;
  network refresh does insert-or-replace into Room, UI never reads network results directly. PASS.
- **Explicit error handling (§1.6)**: sealed `Result<T>`/`AppError` from repositories; every screen
  UiState has an `Error(message, retry)` variant wired to `error_view`/`error_retry_button`. PASS.

No violations requiring the Complexity Tracking table.

## Project Structure

### Documentation (this feature)
```text
plan.md               # this file
tasks.md              # phase-2 output (this plan's task breakdown)
BUILD-REPORT.md        # written after self-validation
```

### Source Code (repository root)
```text
app/
├── build.gradle.kts
└── src/main/java/com/example/ytdash/
    ├── YtDashApp.kt                 # @HiltAndroidApp
    ├── MainActivity.kt              # reads intent extras -> TestConfig, hosts NavHost
    ├── core/
    │   ├── TestConfig.kt            # uiTestMode/mockAuthEmail/apiBaseUrl/apiKey/authorizedEmails/captureExternalLinks
    │   ├── AppConfig.kt             # runtime-resolved base URL + key (test extras > BuildConfig default)
    │   └── ExternalLinkLauncher.kt  # capture-vs-real launch, surfaces external_open_error
    ├── data/
    │   ├── remote/YouTubeApi.kt, dto/*.kt (search/videos/channels DTOs)
    │   ├── remote/YouTubeRepository.kt  # paginate all channels, merge/dedupe, map to domain Video
    │   ├── local/ (Room: VideoEntity, VideoDao, AppDatabase, Converters)
    │   ├── local/VideoCacheRepository.kt # source of truth read/write, stale-fallback
    │   └── auth/AuthRepository.kt   # whitelist check, mock vs real Google sign-in
    ├── domain/
    │   ├── model/Video.kt, ChannelConfig.kt
    │   ├── SortSpec.kt, FilterSpec.kt   # pure comparators/predicates
    │   └── WhitelistValidator.kt
    ├── ui/
    │   ├── login/LoginScreen.kt, LoginViewModel.kt
    │   ├── home/HomeScreen.kt, HomeViewModel.kt, FilterSheet.kt, SortSheet.kt
    │   ├── map/MapScreen.kt, MapViewModel.kt (osmdroid AndroidView + AssistChip marker row)
    │   ├── detail/DetailSheet.kt (inline Surface, not ModalBottomSheet — §5a)
    │   └── common/UiState.kt, LoadingView.kt, ErrorView.kt
    └── di/ (NetworkModule, DatabaseModule, RepositoryModule)

app/src/test/java/com/example/ytdash/       # domain unit tests + Room persistence test
app/src/main/assets/channels.json           # copied from config/channels.json at build time
```

**Structure Decision**: single-module native Android app (`app/`), Compose UI, MVVM +
StateFlow, Hilt DI, Retrofit/OkHttp/kotlinx.serialization, Room, osmdroid. This mirrors the
proven `android-claude-flagship` reference stack in `spec/cross-framework-setup.md` (already
verified 12/12 on the identical constitution/flows), chosen specifically because its two known
sharp edges — Compose popups breaking `testTagsAsResourceId` (§5a) and osmdroid markers being
canvas-only (§5) — have documented, verified mitigations (inline `Surface` sheets; `AssistChip`
marker row) that this plan adopts directly.

## Key design decisions
1. **Channels config bundled as an asset** (`app/src/main/assets/channels.json`, copied verbatim
   from `config/channels.json` at build time via a Gradle copy task) — read at runtime, not
   hardcoded in Kotlin, so swapping the config file doesn't require code changes.
2. **API base URL + key resolution order**: `TestConfig` (intent extras, uiTestMode) > BuildConfig
   default (`https://www.googleapis.com` + empty key placeholder). The mock/real swap is 100%
   runtime for uiTestMode; production reads the key from `BuildConfig.YOUTUBE_API_KEY`, itself
   injected from `config/secrets.env` via `local.properties`/Gradle at build time — never committed.
3. **Pagination**: `YouTubeRepository` loops `search.list` per channel following `nextPageToken`
   until absent, merges all channels' video ids (dedup by id), then batches `videos.list` (id
   batches of ≤50) to fetch `recordingDetails.location` + `contentDetails`. No shortcut endpoint.
4. **Category = source channel label** (from `channels.json`), attached when mapping each
   `search.list` item, per `spec/youtube-api.md`.
5. **Map markers**: osmdroid `MapView` in an `AndroidView` for the real map surface (5 OSM pins),
   PLUS a `LazyRow` of `AssistChip(testTag="map_marker")`, one per located video, as the
   accessible/harness-reachable affordance (constitution §5). Tapping a chip sets `selected` and
   shows the inline detail `Surface` (not `ModalBottomSheet`, to keep `detail_bottom_sheet` and
   `detail_open_youtube_button` in the main composition per §5a).
6. **External link capture**: a single app-root `capturedUrl`/`captureError` state (hoisted above
   NavHost) renders `external_open_url`/`external_open_error` as an overlay banner, fed by both the
   list row tap (iteration 2) and the map detail sheet (iteration 4) via one `ExternalLinkLauncher`.
7. **Auth**: `AuthRepository` — in `uiTestMode` with `mockAuthEmail` set, "sign-in" resolves
   immediately to that email (no Google dialog); otherwise uses real `GoogleSignInClient`
   (`requestEmail()`), then applies whitelist logic (`WhitelistValidator`, from `authorizedEmails`
   extra when present, else `BuildConfig.AUTHORIZED_EMAILS`) identically in both paths.
8. **Cache**: Room `videos` table is the only read path for the list/map/filter/sort. Refresh =
   fetch network → on success, replace table contents transactionally; on failure, keep existing
   rows and surface the failure only if the table is *empty* (stale-fallback, AC-CACHE-01).
9. **Tests**: unit tests for `WhitelistValidator`, `SortSpec`, `FilterSpec` (domain, no Android
   deps) + one Room DAO test (`AndroidX Test` in-memory DB) for cache read/write.
