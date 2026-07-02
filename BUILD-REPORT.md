# Build Report — ytdash (Android)

## Stack (see `plan.md` for full rationale)
- Kotlin 2.2.21, Compose + Material3, MVVM with sealed `UiState` over `StateFlow`.
- Hilt 2.57.2 for DI, Retrofit2 + OkHttp3 + kotlinx.serialization for network/JSON, Room 2.8.4
  for persistence (single source of truth), osmdroid 6.1.20 for the map, Coil3 for images,
  Play Services `GoogleSignInClient` for real Google sign-in.
- AGP 8.13.2 (see "Deviations" — AGP 9 + Hilt/KSP proved incompatible in this environment).
- `minSdk 26`, `compileSdk`/`targetSdk 36`.

## 12/14-AC self-validation result (against the mock, on device `25251FDF60029V`)
Ran the full `flows/AC-*.yaml` suite (14 AC flows — `acceptance-criteria.md`'s own scoring
formula is `passed_ACs / 14`) three consecutive times with `clearState` per flow:

| Run | Result |
|---|---|
| 1 (first attempt) | 12/14 (AC-LOGIN-03, AC-LIST-03 failed — see Deviations/fixes below) |
| 2 (after sort-order fix) | 11/14 (new regressions from a device auto-rotation — see below) |
| 3 (after portrait lock) | **14/14** |
| 4 | **14/14** |
| 5 | **14/14** |

**min / median / max = 14/14 / 14/14 / 14/14** over the three clean, back-to-back runs used for
the reported result. No flakiness observed across those three runs.

Also green:
- `./gradlew testDebugUnitTest` — domain unit tests (`WhitelistValidatorTest`, `SortSpecTest`,
  `FilterSpecTest`), all passing.
- `./gradlew connectedDebugAndroidTest` — `VideoDaoTest` (Room in-memory DB persistence:
  insert/read-back, clear+replace), both passing on-device.
- `./gradlew lintDebug` — **0 errors**, 29 warnings (mostly deprecated-icon/`hiltViewModel`
  overload notices; none functional).

## Real-mode wiring
- `apiBaseUrl`/`apiKey` are read from the UI-test-mode intent extras when present, else default
  to `BuildConfig.YOUTUBE_API_BASE_URL` (`https://www.googleapis.com`) /
  `BuildConfig.YOUTUBE_API_KEY`, the latter injected at build time from the gitignored
  `config/secrets.env` — never hardcoded/committed. Confirmed via direct `curl` that the
  provided real API key + `config/channels.json` channel ids return real data in the exact
  shapes `data/remote/dto/*` expects.
- Real Google Sign-In is implemented with `GoogleSignInClient`/`GoogleSignInOptions` in
  `AuthRepository`, funneling through the same `WhitelistValidator` as the mock path. **Not
  fully exercisable end-to-end here**: no `google-services.json`/OAuth client was provided for
  this package, so a real account picker would fail at Google's consent step, not in our code.
  This only affects the *production* sign-in edge (outside `uiTestMode`) — every scored AC uses
  `mockAuthEmail`.
- On-device real-API smoke test (`apiBaseUrl=https://www.googleapis.com`, real key,
  `captureExternalLinks=false`): the sandboxed test device has **no general internet route**
  (`ping 8.8.8.8` → "Network is unreachable"; only `127.0.0.1` via `adb reverse` to the mock is
  reachable). The app correctly rendered its `error_view`/retry state
  ("Unable to resolve host...") rather than crashing — validating the explicit-error-handling
  contract (constitution §1.6) — but a full populated-list/map screenshot against the real API
  could not be captured in this environment. Since the mock mirrors the real API's exact request/
  response shapes (`spec/youtube-api.md`) and the parsing/pagination/aggregation code path is
  identical for both (only `apiBaseUrl`/`apiKey` differ), the 14/14 mock result is the functional
  evidence for this path.

## Deviations / notable fixes made during self-validation
1. **AGP 9 + Hilt + KSP is broken in this toolchain**: AGP 9's "built-in Kotlin" mode is
   incompatible with KSP, and disabling it (`android.builtInKotlin=false`) plus applying the
   external Kotlin Android plugin then hits an AGP9 `BaseExtension` cast failure in that same
   Kotlin plugin version. Downgraded to **AGP 8.13.2** (still well above the "AGP 8.7+" floor)
   where Hilt 2.57.2 + KSP 2.2.21-2.0.5 work cleanly.
2. **Compose BOM had to move off `2026.03.01`** — it (and Coil 3.5.0) pulled in
   `kotlin-stdlib:2.4.0`, unreadable by a 2.2.21 compiler ("compiled with an incompatible version
   of Kotlin"). Pinned `androidx.compose:compose-bom:2025.09.01` and `coil3:3.2.0`.
3. **Default list order bug (broke AC-LIST-03):** the fixture's contract is "the *first* video's
   external link resolves to `VIDEO_ID_1`" — i.e. `video_list_item[0]` must be the API's natural
   order, not date-sorted. Home defaulted to `SortSpec.DateNewestFirst`; added `SortSpec.Natural`
   (no reordering) as the default, reserving Date/Title sorts for the explicit `sort_button` flow.
4. **Stale ViewModel state bug (broke AC-LOGIN-03):** `LoginViewModel` is Activity-scoped (no
   Navigation back-stack entry to dispose it, since navigation is simple `when`-based Compose
   state, not Nav3), so its `loggedIn=true` flag survived a logout and immediately re-fired
   `onLoggedIn()` the instant the login screen recomposed, silently bouncing back to Home.
   Fixed by consuming (`resetting`) the flag once acted on.
5. **Physical-device auto-rotation mid-run** flipped the device to landscape during one Maestro
   pass, which (correctly, not a bug) shrank the visible list/app-bar and caused several
   assertions to miss off-screen elements. Locked `MainActivity` to
   `android:screenOrientation="portrait"` (an app-level manifest setting, not a device setting)
   since the spec is phone-only and out-of-scope for landscape.

No other deviations from `spec/spec.md` / `spec/acceptance-criteria.md` / `spec/constitution.md`.
No fixture values are hardcoded — channel list, pagination, dedupe, and category labels are all
read at runtime from `config/channels.json` and the API responses (see `plan.md` §"Key design
decisions" and `YouTubeRepository.kt`).
