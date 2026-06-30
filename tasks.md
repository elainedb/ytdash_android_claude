# Tasks — YouTube Dashboard ("ytdash"), Android

> Spec-Kit `/tasks` artifact, dependency-ordered. Each task notes the AC(s) it serves. All complete.

## Phase 0 — Project setup
- [x] T001 Gradle project: version catalog, AGP 8.7.3 / Kotlin 2.0.21 / Gradle 8.14 wrapper, `:app` module.
- [x] T002 `app/build.gradle.kts`: Compose, Room (KSP), Retrofit, kotlinx.serialization, Coil, osmdroid,
      play-services-auth. `BuildConfig` for API key (from gitignored `config/secrets.env`), default base
      host, default whitelist. Release signed with the debug key (standalone-installable).
- [x] T003 Manifest: INTERNET, `usesCleartextTraffic` (mock), `<queries>` for https VIEW, launcher Activity.
- [x] T004 Bundle `config/channels.json` → `assets/channels.json`; launcher icon; theme; `.gitignore` secrets.

## Phase 1 — Domain (pure, testable)
- [x] T010 `Video` model (with `youtubeUrl`, `hasLocation`). → all
- [x] T011 `AuthGate.isAuthorized` (case-insensitive whitelist). → AC-LOGIN-01/02/03
- [x] T012 `SortOption` + `VideoSort` (default/date/title); labels anchored for the harness regex. → AC-SORT-01
- [x] T013 `VideoFilter` (by source-channel label) + `labels()`. → AC-FILTER-01
- [x] T014 `VideoRepository` interface + `LoadResult`. → all
- [x] T015 Unit tests for AuthGate / VideoSort / VideoFilter. → quality bar §2

## Phase 2 — Data layer
- [x] T020 Retrofit `YouTubeApi` (search, videos) + serialization DTOs (search/videos/recordingDetails).
- [x] T021 `RemoteDataSource.fetchAll`: iterate channels, follow `nextPageToken`, dedupe by id
      (order-preserving), enrich locations via `videos.list`. → AC-LIST-01, AC-COUNT-01, AC-MAP-01
- [x] T022 Room `VideoEntity`/`VideoDao`/`AppDatabase`; replace-on-refresh, order via `position`.
- [x] T023 `VideoRepositoryImpl`: network refreshes store; stale-cache fallback on error. → AC-CACHE-01
- [x] T024 Instrumented Room read/write test. → quality bar §2

## Phase 3 — Config & DI
- [x] T030 `TestConfig.fromIntent` (UI-test-mode contract §4); runtime base URL + key.
- [x] T031 `ChannelsLoader` reads bundled channels.
- [x] T032 `AppContainer` (manual DI) builds the repo from the runtime `TestConfig`; `YtdashApp` + osmdroid init.

## Phase 4 — Presentation
- [x] T040 `MainViewModel` + `AppUiState` (single observable state): auth, list state, sort/filter,
      map selection, external-link state. → all
- [x] T041 `LoginScreen` (`screen_login`, `login_google_button`, `login_error_message`). → AC-LOGIN-01/02
- [x] T042 `MainActivity`: read extras, build VM, real Google sign-in launcher, mock sign-in path. → AC-LOGIN-03
- [x] T043 `HomeScreen`: top bar with `video_count`, `filter_button`, `sort_button`, `refresh_control`,
      `logout_button`; `map_nav_button` FAB; `video_list`/`video_list_item`; loading/empty/`error_view`
      +`error_retry_button`; filter & sort panels that REPLACE the list while open.
      → AC-LIST-01/02, AC-COUNT-01, AC-FILTER-01, AC-SORT-01
- [x] T044 `MapScreen`: osmdroid `AndroidView` + native `map_marker` chip row; inline `detail_bottom_sheet`
      with `detail_video_url` + `detail_open_youtube_button`. → AC-MAP-01/02/03
- [x] T045 `RootApp`: `testTagsAsResourceId` root; screen switch; app-root `external_open_url` /
      `external_open_error` banner (tag on the URL `Text`). → AC-LIST-03, AC-MAP-03, AC-LINK-01

## Phase 5 — Validation
- [x] T050 Build debug APK; `adb reverse tcp:8090`; install on `25251FDF60029V`.
- [x] T051 Run `flows/AC-*.yaml`; iterate to 14/14. (One fix: move `external_open_url` tag onto the URL
      `Text` so id+text share a node — the Compose unmerged-semantics asymmetry from cross-framework §D.4.)
- [x] T052 Real-mode wiring: same code, base URL/key swapped via extras; Google Sign-In path.
- [x] T053 `plan.md`, `tasks.md`, `BUILD-REPORT.md`; `.build-complete` after all flows pass.
