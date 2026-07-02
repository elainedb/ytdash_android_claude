# Tasks: ytdash (Android)

**Input**: `plan.md`, `spec/spec.md`, `spec/acceptance-criteria.md`, `spec/constitution.md`
**Organization**: grouped by the spec's own 4 iterations (each independently testable via a
subset of the Maestro `AC-*` flows).

## Phase 1: Setup
- [ ] T001 Scaffold Gradle project (`app/`) via `android` CLI: applicationId
      `com.example.ytdash`, minSdk 26, compileSdk/targetSdk 35, AGP 8.7+, Kotlin 2.0.x, Compose.
- [ ] T002 Add dependencies: Hilt, Retrofit+OkHttp+kotlinx.serialization, Room, osmdroid, Coil,
      Play Services Auth, Navigation-Compose, coroutines-test/junit for tests.
- [ ] T003 [P] Gradle copy task: `config/channels.json` → `app/src/main/assets/channels.json`.
- [ ] T004 [P] Wire `local.properties`/Gradle to inject `YOUTUBE_API_KEY` (from
      `config/secrets.env`, gitignored) and `AUTHORIZED_EMAILS` into `BuildConfig`; commit a dummy
      placeholder path only.
- [ ] T005 [P] `.gitignore` covers `local.properties`, `config/secrets.env`, `*.jks`.

## Phase 2: Foundational (blocks all iterations)
- [ ] T006 `core/TestConfig.kt` — parse intent extras (`uiTestMode`, `mockAuthEmail`,
      `apiBaseUrl`, `apiKey`, `authorizedEmails`, `captureExternalLinks`).
- [ ] T007 `MainActivity.kt` — read `TestConfig` from `intent.extras`, hold in a
      `CompositionLocal`/app-level singleton, set `Modifier.semantics { testTagsAsResourceId =
      true }` at the NavHost root; host the app-root external-link capture banner.
- [ ] T008 `domain/model/Video.kt`, `ChannelConfig.kt` — framework-agnostic domain models.
- [ ] T009 `ui/common/UiState.kt` — sealed `Loading/Content/Empty/Error` + `common/LoadingView.kt`
      (`loading_indicator`), `common/ErrorView.kt` (`error_view`/`error_retry_button`).
- [ ] T010 `data/remote/YouTubeApi.kt` + DTOs (search/videos responses) per `spec/youtube-api.md`.
- [ ] T011 `data/local/` Room: `VideoEntity`, `VideoDao`, `AppDatabase`.
- [ ] T012 `di/` Hilt modules: `NetworkModule` (Retrofit/OkHttp using runtime `AppConfig` base
      URL/key), `DatabaseModule`, `RepositoryModule`.
- [ ] T013 `core/ExternalLinkLauncher.kt` — capture vs real launch, `external_open_url` /
      `external_open_error` contract.

**Checkpoint**: project builds and installs a blank Compose app on the device.

## Phase 3: Iteration 1 — Authentication & access control (P1) 🎯 MVP
**ACs**: AC-LOGIN-01, AC-LOGIN-02, AC-LOGIN-03

- [ ] T014 [US1] `domain/WhitelistValidator.kt` (+ unit test `WhitelistValidatorTest`).
- [ ] T015 [US1] `data/auth/AuthRepository.kt` — mock path (uiTestMode+mockAuthEmail) and real
      `GoogleSignInClient` path; both funnel through `WhitelistValidator`.
- [ ] T016 [US1] `ui/login/LoginScreen.kt` — `screen_login`, `login_google_button`,
      `login_error_message`.
- [ ] T017 [US1] `ui/login/LoginViewModel.kt` — sealed login UiState; on success navigates home.
- [ ] T018 [US1] `ui/home/HomeScreen.kt` shell — `screen_home`, `logout_button` (behind
      `overflow_menu_button` if menu-based, kept in main composition per §5a) → returns to login.
- [ ] T019 [US1] Wire Navigation-Compose graph: login ⇄ home.

**Checkpoint**: AC-LOGIN-01/02/03 pass standalone.

## Phase 4: Iteration 2 — Video list (P2)
**ACs**: AC-LIST-01, AC-LIST-02, AC-LIST-03, AC-COUNT-01, AC-LINK-01

- [ ] T020 [P] [US2] `data/remote/YouTubeRepository.kt` — per channel in `channels.json`: loop
      `search.list` following `nextPageToken` to exhaustion, collect all `videoId`s; dedupe across
      channels; batch `videos.list` (≤50 ids) for `recordingDetails.location` +
      `contentDetails`; map to `Video` (category = channel label).
- [ ] T021 [US2] `data/local/VideoCacheRepository.kt` — single source of truth: refresh (network
      success → replace Room table), stale-fallback on failure (used again in Iteration 3).
- [ ] T022 [US2] `ui/home/HomeViewModel.kt` — loads via `VideoCacheRepository`, exposes
      `Loading/Content(list, totalCount)/Empty/Error`.
- [ ] T023 [US2] `ui/home/HomeScreen.kt` list UI — `video_list`, `video_list_item` (title on the
      `Text` node directly, per cross-framework-setup §D.4), thumbnail (Coil), description;
      `video_count` in the title showing total.
- [ ] T024 [US2] `refresh_control` (pull-to-refresh or button) → re-invoke fetch.
- [ ] T025 [US2] Row tap → `ExternalLinkLauncher.open(video.youtubeUrl)`.
- [ ] T026 [US2] Loading/error/retry wiring for the home screen network call.

**Checkpoint**: AC-LIST-01/02/03, AC-COUNT-01, AC-LINK-01 pass.

## Phase 5: Iteration 3 — Caching, filtering, sorting (P3)
**ACs**: AC-CACHE-01, AC-FILTER-01, AC-SORT-01

- [ ] T027 [P] [US3] `domain/FilterSpec.kt` + unit test — filter predicate by category label.
- [ ] T028 [P] [US3] `domain/SortSpec.kt` + unit test — by `publishedAt` asc/desc, by title.
- [ ] T029 [US3] `data/local/VideoDaoTest.kt` — Room in-memory DB persistence test (write, read
      back, replace).
- [ ] T030 [US3] `ui/home/FilterSheet.kt` — `filter_button` opens a panel that REPLACES the list
      while open (cross-framework-setup §D.2); category options incl. `(?i)tech`-matching label;
      `filter_apply_button` applies + closes.
- [ ] T031 [US3] `ui/home/SortSheet.kt` — `sort_button` opens replacing panel; options labelled
      to END with the matched keyword (e.g. "Date — Newest first" ends in a date/newest token per
      §D.3); `sort_apply_button` applies + closes.
- [ ] T032 [US3] Wire filter/sort state into `HomeViewModel` (derived from the Room-backed list,
      never re-fetching).
- [ ] T033 [US3] Confirm `VideoCacheRepository` stale-fallback path renders `video_list_item`s
      with no `error_view` when network is disabled and app relaunches (AC-CACHE-01).

**Checkpoint**: AC-CACHE-01, AC-FILTER-01, AC-SORT-01 pass; full list still passes iter-2 ACs.

## Phase 6: Iteration 4 — Map (P4)
**ACs**: AC-MAP-01, AC-MAP-02, AC-MAP-03

- [ ] T034 [US4] `ui/map/MapScreen.kt` — osmdroid `MapView` via `AndroidView`
      (`Configuration.getInstance().userAgentValue = packageName` to avoid 403s), pins at each
      located video's lat/lng; `screen_map`, reached via `map_nav_button` on home.
- [ ] T035 [US4] `ui/map/MapScreen.kt` marker affordance — `LazyRow`/`AssistChip` row, one
      `map_marker` per located video (main composition, not a popup).
- [ ] T036 [US4] `ui/detail/DetailSheet.kt` — inline `Surface` (not `ModalBottomSheet`):
      `detail_bottom_sheet`, `detail_video_url` (exact `watch?v=` URL text),
      `detail_open_youtube_button`.
- [ ] T037 [US4] Wire chip tap → select video → show `DetailSheet`; button →
      `ExternalLinkLauncher.open(selectedVideo.youtubeUrl)` (same capture banner as iter-2).
- [ ] T038 [US4] `ui/map/MapViewModel.kt` — reads located videos from `VideoCacheRepository`
      (Room), no separate network call.

**Checkpoint**: AC-MAP-01/02/03 pass; all 12 ACs pass together.

## Phase 7: Real-mode wiring
- [ ] T039 Confirm `AppConfig` resolves `apiBaseUrl`/`apiKey` from `TestConfig` when present,
      else `BuildConfig` production defaults (real YouTube host + `YOUTUBE_API_KEY`).
- [ ] T040 Real Google Sign-In path (`GoogleSignInClient`, `authorizedEmails` from
      `BuildConfig.AUTHORIZED_EMAILS` outside test mode) — smoke test manually (not scored by ACs).
- [ ] T041 Real external launch path (`captureExternalLinks=false`) — `Intent(ACTION_VIEW)`,
      catch `ActivityNotFoundException`/`SecurityException` → `external_open_error`.

## Phase 8: Polish
- [ ] T042 [P] Lint clean (`./gradlew lint`) — zero errors.
- [ ] T043 [P] Unit tests green (`./gradlew test`).
- [ ] T044 Run full Maestro suite on `25251FDF60029V` against the mock; fix until 12/12 pass.
- [ ] T045 Write `BUILD-REPORT.md`; create `.build-complete` only after T044 is green.

## Dependencies
Setup → Foundational → Iteration 1 → Iteration 2 → Iteration 3 → Iteration 4 → Real-mode → Polish.
Iterations are additive on the same Home/Map screens, so — unlike an independent-microservice
project — they are implemented sequentially rather than in parallel, but each has its own
checkpoint of ACs to validate before moving on.
