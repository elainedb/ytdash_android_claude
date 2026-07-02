# Tasks: ytdash (Android)

**Input**: `plan.md`, `spec/spec.md`, `spec/acceptance-criteria.md`
**Organization**: grouped by the spec's 4 iterations (each maps to a checkpoint of ACs).

## Phase 1: Setup

- [X] T001 Gradle project scaffold: version catalog (`gradle/libs.versions.toml`), root +
  `:app` build scripts, wrapper (Gradle 8.14), `AndroidManifest.xml`, launcher icon, theme
- [X] T002 Package skeleton: `core/`, `domain/`, `data/`, `presentation/` under
  `com.example.ytdash`
- [X] T003 [P] `copyChannelsConfig` Gradle task bundles `config/channels.json` into
  `assets/` (no hardcoded channel list in source)

## Phase 2: Foundational (blocks all iterations)

- [X] T004 `TestConfig.fromIntent` — parses `uiTestMode`/`mockAuthEmail`/`apiBaseUrl`/`apiKey`/
  `authorizedEmails`/`captureExternalLinks` from launch-intent extras (`core/testmode/`)
- [X] T005 `RuntimeConfig` — mutable singleton holding the resolved config, applied in
  `MainActivity.onCreate` before `setContent` (`core/config/`)
- [X] T006 `NetworkModule` — Retrofit/OkHttp with a per-request interceptor that rewrites
  scheme/host/port + injects the API key from `RuntimeConfig` (so base URL/key are truly
  runtime-swappable, constitution §4)
- [X] T007 `DatabaseModule` — Room `AppDatabase`/`VideoDao`
- [X] T008 `RepositoryModule` — binds domain repository interfaces to impls
- [X] T009 `MainActivity` sets `testTagsAsResourceId = true` once on the Compose root
  (constitution §3) and hosts the single `NavHost`
- [X] T010 `ExternalLinkViewModel` (Activity-scoped) + `ExternalLinkBanner` at the app root —
  shared `external_open_url`/`external_open_error` surface for iterations 2 and 4

**Checkpoint**: app launches to a login screen; DI graph resolves; ready for iteration work.

## Phase 3: Iteration 1 — Authentication & access control (P1)

**Independent test**: `AC-LOGIN-01/02/03`

- [X] T011 [US1] `IsAuthorizedEmailUseCase` (pure Kotlin, case-insensitive) +
  `app/src/test/.../IsAuthorizedEmailUseCaseTest.kt`
- [X] T012 [US1] `AuthRepository`/`AuthRepositoryImpl` — real path via Credential Manager +
  Google ID; UI-test-mode path short-circuits to `mockAuthEmail` in `LoginViewModel`
- [X] T013 [US1] `LoginScreen`/`LoginViewModel`/`LoginUiState` — `screen_login`,
  `login_google_button`, `login_error_message`, `loading_indicator`
- [X] T014 [US1] `logout_button` wired directly in the home `TopAppBar` (no overflow menu, to
  avoid the Compose-popup `testTagsAsResourceId` trap — constitution §5a)

**Checkpoint**: sign-in/whitelist/logout independently pass against the mock.

## Phase 4: Iteration 2 — Video list (P1)

**Independent test**: `AC-LIST-01/02/03`, `AC-COUNT-01`, `AC-LINK-01`

- [X] T015 [US2] `YouTubeApiService` (search.list + videos.list, mirrors
  `spec/youtube-api.md`) + DTOs
- [X] T016 [US2] `VideoRepositoryImpl.refresh()` — iterates every configured channel, follows
  `nextPageToken` to exhaustion per channel, dedupes by videoId, batches `videos.list` (chunks
  of 50) for `recordingDetails.location`
- [X] T017 [US2] Room `VideoEntity`/`VideoDao` (`observeAll`, `replaceAll` = clear+insert in one
  `@Transaction`)
- [X] T018 [US2] `HomeScreen`/`HomeViewModel`/`HomeUiState` — `video_list`, `video_list_item`
  (tag on the title `Text`, not the row), `video_count` (= total loaded, unfiltered),
  `refresh_control`, `loading_indicator`, `error_view`/`error_retry_button`
- [X] T019 [US2] Row tap → `ExternalLinkViewModel.openVideo` (capture vs. real launch per
  `captureExternalLinks`)

**Checkpoint**: list loads all paginated videos from all channels; count/tap/refresh pass.

## Phase 5: Iteration 3 — Caching, filtering, sorting (P2)

**Independent test**: `AC-CACHE-01`, `AC-FILTER-01`, `AC-SORT-01`

- [X] T020 [US3] Stale-fallback: `HomeViewModel` shows `Error` only when the store is *empty and*
  refresh failed; a non-empty store always renders `Content`, even mid-failed-refresh
  (`app/src/androidTest/.../VideoDaoTest.kt` covers the underlying read/write contract)
- [X] T021 [US3] `SortVideosUseCase`/`FilterVideosUseCase` (pure Kotlin) +
  `SortVideosUseCaseTest`/`FilterVideosUseCaseTest`
- [X] T022 [US3] Filter panel replaces the list while open; option text = category verbatim
  (matches flows' full-string `(?i)${FILTER_LABEL}` regex)
- [X] T023 [US3] Sort panel replaces the list while open; labels end in "Newest"/"Oldest"/etc.
  (matches `(?i)date.*(desc|newest)`)

**Checkpoint**: offline relaunch still shows cached rows; filter/sort change the visible set.

## Phase 6: Iteration 4 — Map (P2)

**Independent test**: `AC-MAP-01/02/03`

- [X] T024 [US4] `MapScreen` — osmdroid `AndroidView` (visual pins, human-facing only — canvas
  markers have no a11y nodes)
- [X] T025 [US4] `map_marker` native affordance — one `AssistChip` per located video in the same
  Compose tree as the map (constitution §5 "accessible affordance", not the rendered pin)
- [X] T026 [US4] `detail_bottom_sheet` — inline `Surface` (not `ModalBottomSheet`), with
  `detail_video_url` + `detail_open_youtube_button` wired to the shared
  `ExternalLinkViewModel`

**Checkpoint**: map shows markers; tapping one opens a sheet whose YouTube button opens the
*same* video's URL.

## Phase 7: Polish / cross-cutting

- [ ] T027 Build APK, install on `25251FDF60029V`, run `flows/` against the mock; iterate to
  12/12
- [ ] T028 Wire real-mode smoke path (real API base URL/key, real Google Sign-In) —
  `BUILD-REPORT.md` records what could/couldn't be verified given the credentials available in
  this environment
- [ ] T029 `BUILD-REPORT.md` + `.build-complete` (only after all flows pass)

## Notes
- No `[P]` parallelization was used for the actual build — one agent authored the whole app for
  cross-file coherence (shared `RuntimeConfig`, shared `ExternalLinkViewModel`, shared selector
  contract), which is the same reason the constitution keeps ID strings identical across screens.
- Tests were written alongside their use case (T011, T021) and the cache layer (T017/T020), not
  deferred to a final pass — the constitution's "at least one persistence test" and "unit tests
  for the domain layer" are satisfied by T011/T017/T021's test files.
