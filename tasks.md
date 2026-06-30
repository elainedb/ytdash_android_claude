# Tasks — YouTube Dashboard ("ytdash"), native Android

> Spec-Kit `/tasks` artifact, derived from `plan.md`. Dependency-ordered; `[x]` = done.

## Phase 0 — Toolchain & scaffold
- [x] T001 Install `android` CLI; import the corporate (Zscaler) CA into the JDK truststore so
  Gradle/the CLI can reach Google Maven through TLS interception.
- [x] T002 Scaffold the Compose project (`android create empty-activity`, minSdk 29, compileSdk 36).
- [x] T003 Add dependencies (OkHttp, kotlinx.serialization, osmdroid, Coil, play-services-auth);
  enable `buildConfig`; inject `YOUTUBE_API_KEY` from gitignored `config/secrets.env`.
- [x] T004 Manifest: INTERNET permission, `<queries>` for https VIEW, network-security-config
  permitting cleartext only to the local mock hosts.
- [x] T005 Gradle `Copy` task syncing `config/channels.json` → `assets/channels.json`.

## Phase 1 — Contracts (must precede UI so flows can drive it)
- [x] T010 `TestTags` constants for every selector-contract id (constitution §3).
- [x] T011 `TestConfig.fromIntent` reading all UI-test-mode extras (§4).
- [x] T012 `testTagsAsResourceId = true` on the app-root composition.

## Phase 2 — Data layer
- [x] T020 DTOs mirroring search.list / videos.list / channels.list JSON.
- [x] T021 `YouTubeApi`: paginated `searchAllPages`, batched `videoDetails`.
- [x] T022 `ChannelConfig` (assets) + `VideoCache` (SharedPreferences JSON).
- [x] T023 `VideoRepository`: aggregate all channels → dedupe → enrich with location → cache;
  stale-fallback on network error.

## Phase 3 — Domain
- [x] T030 `Auth.isAuthorized` (case-insensitive whitelist).
- [x] T031 `VideoQuery` filter + sort + categories (pure).

## Phase 4 — Presentation (iterations 1–4)
- [x] T040 `AuthViewModel` + `LoginScreen` (mock + real Google sign-in)  → AC-LOGIN-01/02/03.
- [x] T041 `HomeViewModel` + `HomeScreen` list, `video_count`, refresh  → AC-LIST-01/02, AC-COUNT-01.
- [x] T042 Row tap → external open; app-root capture banner            → AC-LIST-03, AC-LINK-01.
- [x] T043 Inline filter/sort panels (replace list, instant apply)      → AC-FILTER-01, AC-SORT-01.
- [x] T044 `MapViewModel` + `MapScreen`: osmdroid + native `map_marker` chips + inline sheet
  → AC-MAP-01/02/03.
- [x] T045 Offline relaunch reads disk cache, no blocking error         → AC-CACHE-01.

## Phase 5 — Tests & validation
- [x] T050 Unit tests: `AuthTest`, `VideoQueryTest` (JVM) — passing.
- [x] T051 Instrumented `VideoCacheTest` (persistence round-trip).
- [ ] T052 Run all 14 `flows/AC-*.yaml` against the mock until green.
- [ ] T053 Real-mode smoke (swap apiBaseUrl/apiKey to real YouTube).
- [ ] T054 `BUILD-REPORT.md`; create `.build-complete` once flows pass.
