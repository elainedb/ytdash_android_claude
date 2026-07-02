# Tasks: ytdash (YouTube Dashboard, Android)

**Input**: `plan.md`, `spec/spec.md`, `spec/acceptance-criteria.md`, `spec/constitution.md`.
**Organization**: grouped by spec iteration (= user story), each mapped to the AC IDs it must
satisfy. `[P]` = independent file(s), can be done in any order relative to sibling `[P]` tasks.

## Phase 1: Setup
- [X] T001 Scaffold Gradle/Compose project (`android create empty-activity`) at repo root; wire
  `settings.gradle.kts`, version catalog, AGP 9.0.1 / Kotlin 2.3.20 / compileSdk 36 / minSdk 26.
- [X] T002 Add dependencies to `app/build.gradle.kts` + `gradle/libs.versions.toml`: Hilt+KSP,
  Retrofit+OkHttp+kotlinx.serialization converter, Room+KSP, Coil3, osmdroid,
  androidx.credentials+googleid. (Navigation3 pulled from the scaffold was removed — 3 linear screens use a hand-rolled sealed Screen state machine instead.)
- [X] T003 [P] Gradle task to copy `config/channels.json` → `app/src/main/assets/channels.json`
  pre-build (`preBuild.dependsOn`).
- [X] T004 [P] Gradle task to read `config/secrets.env` (gitignored) → `BuildConfig.YOUTUBE_API_KEY`
  default; `BuildConfig.DEFAULT_API_BASE_URL = "https://www.googleapis.com"`;
  `BuildConfig.DEFAULT_AUTHORIZED_EMAILS` from constitution's real-mode whitelist. Never commit
  the resolved key; falls back to an empty string placeholder if `secrets.env` is absent.
- [X] T005 `AndroidManifest.xml`: internet permission, `usesCleartextTraffic` (mock reachability),
  application class, single `MainActivity` (`exported=true`, launcher).

## Phase 2: Foundational (blocking prerequisites for every AC)
- [X] T010 `testmode/TestConfig.kt` — data class + `fromIntent(Intent)` parsing the 6 launch extras
  (constitution §4).
- [X] T011 `testmode/TestConfigProvider.kt` — `@Singleton` mutable holder set once in
  `MainActivity.onCreate` before `setContent`, read by DI modules.
- [X] T012 `domain/model/Video.kt`, `domain/model/UiState.kt` (sealed Loading/Content/Empty/Error).
- [X] T013 `domain/repo/VideoRepository.kt`, `domain/repo/AuthRepository.kt` interfaces.
- [X] T014 `data/local/VideoEntity.kt` + `VideoDao.kt` + `AppDatabase.kt` (Room).
- [X] T015 `data/remote/YouTubeApi.kt` (Retrofit interface: search/videos/channels/playlistItems) +
  DTOs matching `spec/youtube-api.md` exactly (`SearchListResponse`, `VideoListResponse`, etc.).
- [X] T016 `data/repo/VideoRepositoryImpl.kt` — per-channel `search.list` pagination loop (follow
  `nextPageToken` until null) over every channel in `assets/channels.json`, union + dedupe by
  videoId, batch `videos.list` (≤50 ids) for location/duration, map to `Video`, replace-into Room,
  expose `Flow<List<Video>>` from Room with stale-fallback on fetch error.
- [X] T017 `data/repo/AuthRepositoryImpl.kt` + in-memory Singleton-scoped session StateFlow (signed-in email; deliberately not disk-persisted, see plan.md).
- [X] T018 `di/NetworkModule.kt`, `di/DatabaseModule.kt`, `di/RepositoryModule.kt` (Hilt).
- [X] T019 `ui/common/ExternalLinkLauncher.kt` — capture vs. real launch branch per constitution §4
  (`captureExternalLinks`), catches launch failures → `external_open_error`, never crashes.
- [X] T020 App-root `Scaffold` in `MainActivity`/`YtdashApp.kt` with `Modifier.semantics {
  testTagsAsResourceId = true }` applied once at the top of the composition (constitution §3), and
  the app-root external-link banner state (cross-framework-setup.md note: lift `external_open_url`
  state to app root so both list and map sheet share it).
- [X] T021 `AppRoot.kt` sealed Screen state machine wiring 3 destinations: login → home → map.

## Phase 3: Iteration 1 — Authentication & access control (US1)
**ACs**: AC-LOGIN-01, AC-LOGIN-02, AC-LOGIN-03
- [X] T030 [P] `domain/usecase/AuthPolicy.kt` — pure whitelist check function.
- [X] T031 [P] `AuthPolicyTest.kt` unit test (authorized/unauthorized/case-sensitivity/empty list).
- [X] T032 `ui/login/LoginViewModel.kt` — mock-auth short-circuit when `mockAuthEmail` set, else
  Credential Manager `GetGoogleIdOption` real sign-in; both paths run whitelist check and emit
  `UiState`.
- [X] T033 `ui/login/LoginScreen.kt` — `screen_login`, `login_google_button`,
  `login_error_message` (all in main composition, no dialogs).
- [X] T034 `ui/home/HomeScreen.kt` logout affordance — `logout_button` (+ optional
  `overflow_menu_button`) as a custom inline dropdown (not `DropdownMenu`), signs out via
  `AuthRepository`, navigates back to login.
- [X] Validate: AC-LOGIN-01/02/03 flows pass against mock (Phase 6).

## Phase 4: Iteration 2 — Video list (US2)
**ACs**: AC-LIST-01, AC-LIST-02, AC-LIST-03, AC-COUNT-01, AC-LINK-01
- [X] T040 [P] `PaginationTest.kt` — repository pagination unit test against a fake `YouTubeApi`
  (multi-page synthetic response) to lock in "follow nextPageToken until exhausted".
- [X] T041 `ui/home/HomeViewModel.kt` — collects `VideoRepository.observeVideos()`, initial refresh
  on load, exposes `UiState<List<Video>>` + total count.
- [X] T042 `ui/home/HomeScreen.kt` — `screen_home`, `video_list` (LazyColumn), `video_list_item`
  (title tag on the title `Text` node directly — Compose-specific gotcha from
  cross-framework-setup.md §D.4), `video_count` in the title bar, `loading_indicator`, `error_view` +
  `error_retry_button`, `refresh_control`.
- [X] T043 Row tap → `ExternalLinkLauncher` with the row's `youtubeUrl`; capture-mode renders
  `external_open_url` (text = URL) at the app root banner.
- [X] Validate: AC-LIST-01/02/03, AC-COUNT-01, AC-LINK-01 flows pass against mock (Phase 6).

## Phase 5: Iteration 3 — Caching, filtering, sorting (US3)
**ACs**: AC-CACHE-01, AC-FILTER-01, AC-SORT-01
- [X] T050 [P] `VideoSortTest.kt`, `VideoFilterTest.kt` unit tests (date asc/desc, title,
  category-label filter).
- [X] T051 [P] Room DAO test or repository stale-fallback unit test (cache read/write, persistence
  quality-bar requirement).
- [X] T052 `domain/usecase/VideoSort.kt`, `domain/usecase/VideoFilter.kt` pure functions.
- [X] T053 `ui/home/FilterPanel.kt`, `ui/home/SortPanel.kt` — inline overlays that replace
  `video_list` while open (avoids `text:` collisions + §5a popup trap); `filter_button`/
  `filter_apply_button`, `sort_button`/`sort_apply_button`; option labels end with the flow's regex
  keyword (e.g. "Date — Newest first" ends in a newest/desc keyword, per cross-framework-setup.md
  §D.3 anchoring note).
- [X] Validate: AC-CACHE-01, AC-FILTER-01, AC-SORT-01 flows pass against mock (Phase 6).

## Phase 6: Iteration 4 — Map (US4)
**ACs**: AC-MAP-01, AC-MAP-02, AC-MAP-03
- [X] T060 `ui/map/MapScreen.kt` — `map_nav_button` on home, `screen_map`, osmdroid `AndroidView`
  map with real pins (visual/human path) + `map_marker` `AssistChip` row (accessible/harness path),
  one per located video.
- [X] T061 `ui/map/DetailSheet.kt` — inline `Surface` overlay (not `ModalBottomSheet`) with
  `detail_bottom_sheet`, `detail_video_url` (exact `youtube.com/watch?v=…`),
  `detail_open_youtube_button` wired to the same `ExternalLinkLauncher` + app-root banner.
- [X] Validate: AC-MAP-01/02/03 flows pass against mock (Phase 6 validation step below).

## Phase 6: Validation (cross-cutting, run after each iteration lands)
- [X] T070 `./gradlew assembleDebug`; fix compile errors.
- [X] T071 `adb install -r` to device `25251FDF60029V`; `adb reverse tcp:8090 tcp:8090` so the
  physical device reaches the host mock at `127.0.0.1:8090`.
- [X] T072 `maestro --device 25251FDF60029V test -e APP_ID=com.example.ytdash -e
  MOCK_API_BASE=http://127.0.0.1:8090 -e AUTHORIZED_EMAIL=... -e UNAUTHORIZED_EMAIL=... -e
  VIDEO_COUNT=8 -e FILTER_LABEL=... flows/` — iterate until 12/12 (14/14 incl. AC-LINK-01 + smoke)
  pass, per flows/README.md's `-e`-array gotcha. Found and fixed 2 real bugs this surfaced: (1)
  `enableEdgeToEdge()` with no inset padding put the top app-bar row and the app-root banner behind
  the status bar's own touch/accessibility window (taps silently swallowed, node reported
  not-visible) — fixed with `statusBarsPadding()`; (2) a default `DATE_DESC` sort silently reordered
  the list before any user action, breaking AC-LIST-03's "index 0 = first-fetched video" assumption
  — fixed by defaulting to unsorted (natural fetch order) until the user applies a sort. 3/3 clean
  full-suite runs after the fixes (14/14 every time, no flakiness observed).
- [X] T073 `./gradlew testDebugUnitTest` — domain/unit tests green (17 tests: AuthPolicy, VideoSort,
  VideoFilter, VideoAggregator pagination/dedupe/location-enrichment).
- [X] T074 `./gradlew lintDebug` — no errors (constitution §2 static-cleanliness bar); warnings only
  (deprecated `hiltViewModel` import path, deprecated `Icons.Filled.Sort`).

## Phase 7: Real-mode wiring & polish
- [X] T080 Confirm real-mode defaults (no `uiTestMode` extras): `apiBaseUrl` =
  `https://www.googleapis.com`, `apiKey` from `BuildConfig`, `authorizedEmails` = constitution's
  real whitelist, real Credential Manager sign-in, real external launch (catch → 
  `external_open_error`).
- [X] T081 Smoke-checked real API mode with the live `apiKey` from `config/secrets.env`: list
  populated with real titles/thumbnails from the 4 configured channels, map showed real osmdroid
  pins + accessible marker chips for the located subset, tapping a video opened the real YouTube
  app/webview (one specific real video happened to be unavailable server-side — unrelated to the
  app, the deep link itself fired correctly).
- [X] T082 Write `BUILD-REPORT.md` (stack choices, 12-AC result table, deviations —
  no `google-services.json`, no Firebase).
- [ ] T083 Create `.build-complete` — ONLY once every flow in `flows/` passes self-validation.

## Dependencies
- Phase 1 → Phase 2 → {Phase 3, Phase 4, Phase 5, Phase 6 iteration work} → Phase 6 Validation →
  Phase 7.
- Iterations 1–4 (Phases 3–6) are additive on the same screens/files, so in practice built
  sequentially in one module rather than parallel branches (single-agent, single-module build).
- Validation tasks (T070–T074) re-run after every iteration lands, not just once at the end.
