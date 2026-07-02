# Implementation Plan: ytdash (YouTube Dashboard, Android)

**Branch**: `main` | **Date**: 2026-07-02 | **Spec**: `spec/spec.md` + `spec/acceptance-criteria.md`

**Input**: Feature specification from `spec/spec.md` (frozen). Constitution: `spec/constitution.md`.
Cross-framework recipe: `spec/cross-framework-setup.md`. YouTube API shapes: `spec/youtube-api.md`.

## Summary
A native Android (Kotlin + Jetpack Compose) app that signs a user in (Google identity, whitelist
gated), aggregates videos from a configured set of YouTube channels (paginated, deduped), caches
them locally as the source of truth, lets the user filter/sort, and plots geolocated videos on an
OpenStreetMap map with an accessible marker affordance. Built test-first against the acceptance
criteria via Maestro, driven through a UI-test-mode contract read from launch-intent extras.

## Technical Context
**Language/Version**: Kotlin 2.2.21 (pinned down from the scaffold's 2.3.20 default — KSP, needed
for Hilt/Room annotation processing, has no build for 2.3.20 yet; 2.2.21 is its latest matching
release), JVM target 17.
**Primary Dependencies**: Jetpack Compose (BOM 2026.03.01) + Material3, Hilt (DI),
Retrofit2 + OkHttp3 + kotlinx.serialization (network/JSON), Room (persistence), Coil3 (image
loading), osmdroid (map), androidx.credentials + googleid (Google Sign-In). Navigation: a small
hand-rolled sealed `Screen` state machine held in `AppRoot` (no navigation-compose/Navigation3
dependency) — the app is 3 linear screens (login → home → map, with a single back edge), so a full
navigation library is unneeded ceremony; this also lifts the external-link banner state naturally
to the composable that survives screen switches (cross-framework-setup.md's "lift to app root"
note).
**Storage**: Room (SQLite) — single source of truth for the video list; the signed-in session is an
in-memory `Singleton`-scoped `StateFlow<String?>` (deliberately NOT persisted to disk — every flow
in `flows/`, including AC-CACHE-01's offline *relaunch*, re-taps `login_google_button` after
`launchApp`, so a disk-persisted session would skip `screen_login` and break that assumption; only
the video cache is meant to survive a relaunch).
**Testing**: JUnit4 + kotlinx-coroutines-test for domain/unit tests (whitelist, sort, filter, cache
DAO), Maestro (external, black-box) for the 12 acceptance-criteria flows in `flows/`.
**Target Platform**: Android (minSdk 26, target/compileSdk 36), physical device
`25251FDF60029V` for self-validation.
**Project Type**: Single Android app module (`app/`), Gradle Kotlin DSL, AGP 9.0.1.
**Performance Goals**: N/A (functional correctness is the measured dimension, per spec §Out of
scope).
**Constraints**: No secrets in source control (constitution §2); base URL + API key overridable at
runtime (constitution §4); every harness-asserted element reachable, including inside
popups/overlays (constitution §5a); map markers exposed via a native accessible affordance
(constitution §5); no catch-all channel endpoint — must iterate `config/channels.json` and follow
pagination (spec.md §Data, youtube-api.md).
**Scale/Scope**: 3 screens (login, home/list, map), 4 iterations, 12 scored acceptance criteria
(AC-LOGIN-01..03, AC-LIST-01..03, AC-COUNT-01, AC-CACHE-01, AC-FILTER-01, AC-SORT-01,
AC-MAP-01..03) + AC-LINK-01 (real-launch smoke, 14 flows total in `flows/`, 12 in the scored table
in acceptance-criteria.md — the extra two, `AC-LINK-01` and the fixture "0" indices, are covered by
the same suite run).

## Architectural decisions & rationale

1. **Layering (constitution §1.1/§1.2).** Three Gradle-source packages under
   `com.example.ytdash`: `data` (network DTOs + Retrofit service, Room entities/DAO, repositories),
   `domain` (pure Kotlin: `AuthPolicy`, `VideoSort`, `VideoFilter`, no Android imports — directly
   unit-testable), `ui` (Compose screens + `ViewModel`s). Presentation depends on repository
   *interfaces* declared in `domain`, bound to `data` implementations via Hilt `@Binds` — dependency
   inversion without a second module (single-module DI is idiomatic for an app this size; a
   multi-module split would be premature for 3 screens).
2. **State (constitution §1.3).** Every screen renders from a sealed `UiState` (`Loading` /
   `Content` / `Empty` / `Error`) exposed as a `StateFlow` from its `ViewModel`. No business logic in
   `onClick` lambdas — they call a `ViewModel` function; the `ViewModel` calls a `domain`/`data`
   function and reduces the result into `UiState`.
3. **Threading (constitution §1.4).** All repository functions are `suspend`; Retrofit + Room both
   dispatch off the main thread by default (Retrofit suspend calls run on OkHttp's dispatcher, Room
   generates a `suspend`/`Flow` API that runs on its own executor). No raw `Thread`/`AsyncTask`.
4. **Single source of truth (constitution §1.5).** The home screen's `Flow<List<Video>>` comes from
   Room, not directly from the network. A refresh (manual or on launch) fetches from the API and
   `REPLACE`s the Room rows; on network failure the flow keeps emitting whatever Room already has
   (stale-fallback), which is what makes `AC-CACHE-01` (offline relaunch) pass without a fresh
   network call.
5. **Errors (constitution §1.6).** A small `Result<T>`-returning repository layer distinguishes
   "network/parse failure with nothing cached" (→ `error_view` + `error_retry_button`) from
   "network failure but cache has data" (→ show cache, no error). External link failures
   (`ActivityNotFoundException` or any throw from `startActivity`) are caught and surfaced as
   `external_open_error` — never a crash, per constitution §4's `captureExternalLinks=false` path.
6. **DI**: Hilt. Idiomatic for a Compose+Kotlin app of this scope, minimal boilerplate vs. manual
   containers, first-class `ViewModel`/`WorkManager` integration if needed later.
7. **Networking/JSON**: Retrofit + OkHttp + kotlinx.serialization (`search.list`/`videos.list` DTOs
   mirror `spec/youtube-api.md` exactly — `id.videoId` (search) vs. `id` string (videos),
   `recordingDetails.location`). `search.list` is used for the per-channel paginated listing
   (`channelId`, `order=date`, `type=video`, `maxResults` small enough to exercise pagination against
   the mock's 2-per-page default) and `videos.list` (batched by id, ≤50/call) is used to backfill
   `recordingDetails.location` + `contentDetails` for the merged id set — matching the two-endpoint
   split the mock/real API actually has (search has no location field).
8. **Channel aggregation (spec.md, youtube-api.md — "no catch-all").** `config/channels.json` is
   copied by a Gradle task into `app/src/main/assets/channels.json` (single source of truth stays
   the repo-root config file; the app reads its runtime copy from assets, never a hardcoded Kotlin
   list). The repository calls `search.list` once per configured channel, follows `nextPageToken`
   until absent for *each* channel, unions the results, dedupes by `videoId`, then calls
   `videos.list` in batches of ≤50 ids to attach location/duration, and finally maps to the domain
   `Video` model. This is deliberately channel-driven (not `channelId=ALL`) so it does not overfit to
   the mock, per spec.md's anti-overfit note.
9. **Cache**: Room. `VideoDao.observeAll(): Flow<List<VideoEntity>>` is the screen's source of
   truth; a refresh does `@Transaction { deleteAll(); insertAll(new) }` (replace-on-refresh). No TTL
   auto-expiry is required by the ACs, but a `lastFetchedAt` timestamp is stored for future use /
   transparency.
10. **Filter/Sort UI (cross-framework-setup.md §D, applies to any black-box driver, not just
    cross-framework).** Filter and sort open an **inline overlay that replaces the list within the
    same Compose composition** (a `Box`/state toggle, not `Dialog`/`ModalBottomSheet`/`DropdownMenu`)
    — this avoids two problems at once: (a) Maestro `text:` selector collisions between an option
    label like "Tech" and an item title containing "Tech", and (b) the Compose "popup is a separate
    composition window" trap (constitution §5a) that would silently hide `testTag`s from
    `testTagsAsResourceId`. The same reasoning applies to `overflow_menu_button`/`logout_button`
    (custom inline dropdown, not `DropdownMenu`) and `detail_bottom_sheet` (inline `Surface` overlay,
    not `ModalBottomSheet`).
11. **Map**: osmdroid (`AndroidView` interop) — idiomatic native-Android OSM widget. Per constitution
    §5, osmdroid draws markers on a `Canvas` with no accessibility nodes, so a horizontal
    `LazyRow` of `AssistChip`s (one `map_marker` per located video, in the main composition) is the
    accessible affordance Maestro drives; tapping a chip sets `selectedVideoId` and shows the inline
    `detail_bottom_sheet`. The real osmdroid pins still render (visual correctness, human path);
    `map_marker_fallback_used=false` is recorded in `BUILD-REPORT.md` since the chip *is* the
    documented affordance, not a last-resort fallback.
12. **Auth**: `androidx.credentials` (Credential Manager) + `googleid` `GetGoogleIdOption` for real
    Google Sign-In (modern replacement for the deprecated `GoogleSignInClient`). In UI-test-mode with
    `mockAuthEmail` set, the sign-in call is skipped entirely and the mock email is used directly —
    this is the deterministic seam constitution §4 requires. No `google-services.json`/Firebase is
    present in `config/`; the Google Sign-In / Credential Manager path does not require Firebase (it
    only needs an OAuth web client id for full ID-token verification, which is optional for this
    app since the app only needs the account's email, not a verified backend session). This is
    called out as a deviation in `BUILD-REPORT.md`.
13. **Test-mode plumbing**: `TestConfig` data class parsed once from `intent.extras` in
    `MainActivity.onCreate`, provided via a small Hilt-friendly holder (`TestConfigProvider`,
    `@Singleton`, mutated once at startup, read everywhere else) — avoids re-plumbing intent access
    through every layer while keeping `data`/`domain` Android-import-free (the provider itself lives
    in `app` alongside `MainActivity`, injected into the network module (`apiBaseUrl`/`apiKey`),
    auth module (`authorizedEmails`, `mockAuthEmail`), and the external-link launcher
    (`captureExternalLinks`).

## Constitution Check
*GATE: re-checked below after the design above.*
- Layered separation ✅ (data/domain/ui packages, §1 above).
- Dependency inversion ✅ (Hilt `@Binds` repository interfaces in `domain`).
- Unidirectional/observable state ✅ (sealed `UiState` + `StateFlow`).
- No main-thread blocking I/O ✅ (Retrofit suspend + Room Flow).
- Single source of truth ✅ (Room).
- Explicit error handling ✅ (`error_view`/`external_open_error`, no silent failure).
- Selector contract ✅ (`testTagsAsResourceId = true` at the Compose root; every ID in constitution
  §3 mapped to a `Modifier.testTag`, see `tasks.md`).
- UI-test-mode contract ✅ (`TestConfig` reads all 6 extras).
- §5a overlay reachability ✅ (no `Dialog`/`ModalBottomSheet`/`DropdownMenu` used for any
  harness-asserted element; all are inline overlays in the main composition).
- Map marker contract ✅ (native `AssistChip` affordance, §11 above).
No violations requiring the Complexity Tracking table.

## Project Structure

### Documentation (this feature)
```text
plan.md            # this file
tasks.md           # dependency-ordered task breakdown
BUILD-REPORT.md     # written after self-validation (stack choices, 12-AC result, deviations)
```
(No `specs/[###-feature]/` subtree — the spec is a single frozen, pre-existing feature at
`spec/spec.md`; Spec-Kit's branch-per-feature layout is not applicable to this single-feature,
non-git workspace, so `plan.md`/`tasks.md` are written at the repo root per the run prompt's
fallback instruction.)

### Source Code (repository root)
```text
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── assets/channels.json          # Gradle-copied from config/channels.json
│   └── java/com/example/ytdash/
│       ├── MainActivity.kt            # intent extras -> TestConfig, NavHost host
│       ├── YtdashApp.kt               # @HiltAndroidApp
│       ├── testmode/                  # TestConfig + TestConfigProvider
│       ├── data/
│       │   ├── remote/                # Retrofit API iface + DTOs (search/videos/channels)
│       │   ├── local/                 # Room entities/DAO/Database
│       │   └── repo/                  # VideoRepositoryImpl, AuthRepositoryImpl
│       ├── domain/
│       │   ├── model/                 # Video, UiState<T>
│       │   ├── repo/                  # VideoRepository, AuthRepository interfaces
│       │   └── usecase/               # AuthPolicy, VideoSort, VideoFilter
│       ├── di/                        # Hilt modules (Network, Database, Repository)
│       └── ui/
│           ├── login/                 # LoginScreen, LoginViewModel
│           ├── home/                  # HomeScreen, HomeViewModel, FilterPanel, SortPanel
│           ├── map/                   # MapScreen, MapViewModel, DetailSheet
│           ├── common/                # LoadingView, ErrorView, ExternalLinkLauncher
│           └── theme/
├── src/test/java/...                  # unit tests: AuthPolicyTest, VideoSortTest,
│                                       # VideoFilterTest, PaginationTest
└── src/androidTest/java/...           # (optional) Room DAO instrumented test
```
**Structure Decision**: Single Gradle Android app module (`app/`), package-by-layer
(`data`/`domain`/`ui`) inside one module — matches "Option 1: Single project" from the template,
adapted for Android. A multi-module split (`:data`, `:domain`, `:app`) was considered and rejected
as unneeded ceremony for a 3-screen app; package boundaries + Hilt interface bindings already give
the dependency-inversion property the constitution requires.

## Complexity Tracking
*No violations — table intentionally omitted.*
