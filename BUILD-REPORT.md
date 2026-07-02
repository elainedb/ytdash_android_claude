# Build Report — ytdash (Android)

## Stack

Kotlin 2.2.21 + Jetpack Compose/Material3, single-Activity + Navigation-Compose, MVVM with
sealed `StateFlow`-driven `UiState` per screen, Hilt 2.57.2 for DI, Retrofit2 + OkHttp +
kotlinx.serialization for network, Room 2.8.4 as the single source of truth, osmdroid 6.1.20 for
the map, Coil3 for images, Credential Manager + Google ID for real sign-in. AGP 8.13.2, JDK 17,
minSdk 26 / targetSdk 35 / compileSdk 35. Full rationale for every stack choice — and how each
maps to a constitution requirement — is in `plan.md`; the phased build log is in `tasks.md`.

## Acceptance criteria result

`spec/acceptance-criteria.md` defines **14** ACs (`AC-LOGIN-01/02/03`, `AC-LIST-01/02/03`,
`AC-COUNT-01`, `AC-CACHE-01`, `AC-FILTER-01`, `AC-SORT-01`, `AC-MAP-01/02/03`, `AC-LINK-01`). Ran
`flows/` with Maestro 2.6.1 against the physical device `25251FDF60029V`, pointed at the local
mock (`http://127.0.0.1:8090` via `adb reverse`), **6 times** (3 with `--format junit`, all with
`clearState:true` per-flow plus one full run starting from a freshly `pm clear`-ed app):

**14/14 pass — min = median = max = 14/14, 0 flaky criteria** across all runs.

| Run | Result |
|---|---|
| 1 (initial) | 12/14 — `AC-LIST-03`, `AC-MAP-03` failed |
| 2 (after fix) | 14/14 |
| 3 | 14/14 |
| 4 | 14/14 |
| 5 (fresh `pm clear`) | 14/14 |
| 6 (after AGP bump, reinstall) | 14/14 |

### The one real bug found and fixed
Run 1 failed `AC-LIST-03`/`AC-MAP-03` (`external_open_url` never became visible within timeout).
Root cause, found via `uiautomator dump` + a debug screenshot: `enableEdgeToEdge()` drew the
app-root `ExternalLinkBanner` under the status bar (it lives outside `Scaffold`'s own inset
handling, since it must overlay both the list and the map screens). Fixed by adding
`.statusBarsPadding()` to the banner. While investigating, also found the home screen defaulted
to `SortOption.DATE_NEWEST` on load, which reordered row 0 away from the fixture's natural
`VIDEO_ID_1` — AC-LIST-03 assumes natural (unsorted) order until the user explicitly sorts. Fixed
by making "no sort chosen yet" (`sortOption = null`) the real default, distinct from any
`SortOption` value.

## Unit / persistence tests
- `app/src/test/.../domain/*`: 16 JVM unit tests (`IsAuthorizedEmailUseCase`,
  `SortVideosUseCase`, `FilterVideosUseCase`) — `./gradlew testDebugUnitTest` passes.
- `app/src/androidTest/.../data/local/VideoDaoTest.kt`: 3 instrumented Room tests (write→read,
  replace-clears-first, cache-survives-as-source-of-truth) — `./gradlew connectedDebugAndroidTest`
  passes on both the physical device and the attached emulator.
- `./gradlew lintDebug`: **0 errors, 39 warnings** (mostly a deprecated `hiltViewModel()` import
  path and one Kotlin annotation-target advisory — none are correctness issues).

## Real-mode wiring (deviation / environment limits)
The base-URL/API-key/whitelist swap is fully runtime-driven (constitution §4: an OkHttp
interceptor reads a single `RuntimeConfig` on every request), so no rebuild is needed to point at
the real API — only the launch extras change. What was actually verified in this environment:

- **Real (non-test) launch**: login screen renders correctly, no crash. ✅ (screenshot-verified)
- **Real Google Sign-In path**: tapping "Sign in with Google" invokes Credential Manager as
  designed; since `config/secrets.env` only ships an *empty* `YOUTUBE_API_KEY` (no
  `GOOGLE_WEB_CLIENT_ID` was provided at all), it fails with a clear, non-crashing message
  ("Google Sign-In is not configured…") rather than a stack trace. ✅ graceful-degradation verified
- **Real API base URL** (`https://www.googleapis.com`) with `uiTestMode` (to skip auth) and an
  empty key: confirmed the interceptor correctly rewrites the request host, and confirmed
  **stale-cache fallback works against a real endpoint** (a prior mock-fetched cache kept
  rendering, no `error_view`, when the real call failed). ✅
- On a fully cleared app + real endpoint: the device itself has **no outbound network route**
  ("Network is unreachable" / DNS resolution failure) — this sandbox's device is only reachable
  via the `adb reverse` tunnel to the local mock, not the open internet. The app correctly showed
  `error_view` + `error_retry_button` (no crash) for this failure. ✅ error-path verified, but the
  **actual real-YouTube-API response could not be exercised end-to-end** in this environment (no
  key, no WAN route from the test device) — this is an environment constraint, not unimplemented
  app behavior. Given a real `YOUTUBE_API_KEY` + `GOOGLE_WEB_CLIENT_ID` and a networked device,
  no code change should be required — only the launch/build config.

## No secrets committed
`config/secrets.env` (gitignored) is read only at build time as a *default fallback*; the
UI-test-mode `apiKey`/`authorizedEmails` extras always take priority at runtime. No key or
whitelist is hardcoded in source.

## Anti-overfit checklist
- Source channels come from `config/channels.json` (copied into `assets/` at build time), never
  inlined in Kotlin.
- `category` is taken from each API response's `snippet.channelTitle`, not a hardcoded
  `tech`/`music`/`news` string.
- `VideoRepositoryImpl.refresh()` follows `nextPageToken` to exhaustion **per channel**, and
  iterates every configured channel — no `channelId=ALL`/catch-all shortcut.
- `video_count` reflects the actual fetched total (`videos.size`), not a fixture-derived literal.

## Deliverables
`plan.md`, `tasks.md` (this build's actual phased log), `BUILD-REPORT.md` (this file),
`.build-complete` (created after the final 14/14 verification run above).
