# Tasks — YouTube Dashboard ("ytdash")

Spec-Kit `/tasks` breakdown, dependency-ordered. Each maps to acceptance criteria (`AC-*`).

## Phase 0 — Project setup
- [x] T001 Scaffold Gradle/Compose project (AGP 9, Kotlin 2.3, compileSdk 36, minSdk 29).
- [x] T002 Add deps: OkHttp, kotlinx.serialization, Coil, osmdroid, play-services-auth.
- [x] T003 Manifest: INTERNET, cleartext (mock), web-intent `<queries>`.
- [x] T004 Bundle `config/channels.json` as an asset + Gradle `syncChannels` copy task.

## Phase 1 — Data + domain (no Android deps where possible)
- [x] T010 `TestConfig.fromIntent` — read UI-test-mode extras (§4).
- [x] T011 DTOs + `YouTubeApi` (search pagination, videos.list batching).
- [x] T012 `VideoRepository` — aggregate all channels, dedupe, enrich location, cache-backed SoT.
- [x] T013 `FileVideoCache` + `VideoCacheCodec` (persistence). → AC-CACHE-01
- [x] T014 `AuthService` whitelist. → AC-LOGIN-01/02
- [x] T015 `VideoOps` sort/filter/categories. → AC-FILTER-01, AC-SORT-01
- [x] T016 `AppContainer` DI seam from `TestConfig`.

## Phase 2 — Presentation
- [x] T020 `AppViewModel` + `AppUiState` (Loading/Content/Empty/Error, filter/sort/map/external).
- [x] T021 `App` root: `testTagsAsResourceId`, screen switch, external-open banner. → AC-LIST-03, AC-MAP-03, AC-LINK-01
- [x] T022 `LoginScreen` (`screen_login`, `login_google_button`, `login_error_message`). → AC-LOGIN-01/02/03
- [x] T023 `HomeScreen` list (`video_list`, `video_list_item`, `video_count`, refresh, logout). → AC-LIST-01/02, AC-COUNT-01
- [x] T024 Filter/Sort panels that replace the list while open. → AC-FILTER-01, AC-SORT-01
- [x] T025 `MapScreen` osmdroid + `map_marker` chips + inline `detail_bottom_sheet`. → AC-MAP-01/02/03
- [x] T026 `MainActivity` wiring + real Google Sign-In path.

## Phase 3 — Tests
- [x] T030 Unit tests: auth, sort, filter, cache codec.

## Phase 4 — Validation & delivery
- [ ] T040 Build APK; `adb reverse` mock; install on `25251FDF60029V`.
- [ ] T041 Run all 14 `flows/AC-*.yaml`; iterate to green.
- [ ] T042 Confirm real-mode base-URL/key swap path.
- [ ] T043 `BUILD-REPORT.md`; create `.build-complete` after flows pass.
