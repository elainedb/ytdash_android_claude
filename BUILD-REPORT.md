# BUILD-REPORT — ytdash (YouTube Dashboard, Android)

## Stack

Native Android, Kotlin 2.2.21, Jetpack Compose (Material3) + AGP 8.13.2, single Gradle module
(`app/`), package-by-layer (`data` / `domain` / `ui`). Full rationale in `plan.md`; task breakdown
in `tasks.md`. Summary of the choices that matter:

| Concern | Choice | Why |
|---|---|---|
| DI | Hilt | idiomatic for a Compose+Kotlin app this size |
| Network/JSON | Retrofit + OkHttp + kotlinx.serialization | matches the DTO shapes in `spec/youtube-api.md` exactly |
| Persistence | Room | single source of truth for the video list (constitution §1.5) |
| Images | Coil3 | Compose-native async image loading |
| Map | osmdroid (`AndroidView`) | native OSM widget; canvas markers made accessible via a chip-row affordance (constitution §5) |
| Auth | androidx.credentials + googleid (Credential Manager) | modern replacement for the deprecated `GoogleSignInClient` |
| Navigation | hand-rolled sealed `Screen` state in `AppRoot` | 3 linear screens don't need a nav library; keeps the external-link banner state naturally hoisted |
| Session | in-memory `Singleton` `StateFlow<String?>` | deliberately NOT disk-persisted — every flow re-taps `login_google_button` after `launchApp`, including AC-CACHE-01's `clearState:false` relaunch |

## Deviations from the reference recipe

1. **AGP 8.13.2, not 9.x.** The scaffold (`android create`) defaults to AGP 9.0.1, which requires
   `hilt-navigation-compose`/`lifecycle` versions that need `compileSdk 37` (not yet available) and
   whose standalone Kotlin plugin path is broken against KSP on this AGP/Kotlin combination.
   Downgraded to AGP 8.13.2 (still "AGP 8.7+" per the brief) with matched dependency versions
   (Kotlin 2.2.21, KSP 2.2.21-2.0.5, Hilt 2.57.2, Compose BOM 2025.09.01, Coil 3.2.0).
2. **No `google-services.json` / OAuth client ID.** `config/` contains only `channels.json` and
   `secrets.env` (the YouTube API key) — no Firebase config or Google OAuth Web client ID was
   provided. Real Google Sign-In is fully wired (Credential Manager + `GetGoogleIdOption`) and will
   work once a `GOOGLE_SERVER_CLIENT_ID` is added to `config/secrets.env`; until then it returns a
   clear, non-crashing error (`login_error_message`) rather than attempting a doomed native call.
   Every scored flow uses `mockAuthEmail` (constitution §4), so this doesn't affect the AC suite.
3. **No sort applied by default.** The list's *natural* order is the fetch/aggregation order
   (channel-by-channel, page-by-page) until the user explicitly picks a sort — see "Bugs found and
   fixed" below.

## Two real bugs the self-validation loop caught

Both were invisible in a plain `assembleDebug` + manual glance, and only surfaced once the Maestro
suite actually drove the compiled APK:

1. **Status-bar touch interception.** `enableEdgeToEdge()` draws Compose content edge-to-edge, but
   nothing was consuming the status-bar inset. The top app-bar row (`refresh_control`,
   `filter_button`, `sort_button`, `overflow_menu_button`) and the app-root external-link banner sat
   partially *behind* the status bar's own system window. Android's accessibility layer treats a
   node obscured by another window as not-visible-to-user, and (separately) the status bar window
   appears to intercept touches in that strip — so taps on those elements silently did nothing, for
   both Maestro-driven taps *and* raw `adb shell input tap` at the exact reported coordinates (ruled
   out a Maestro-specific issue by reproducing with plain `adb`). Fixed with `statusBarsPadding()`
   on the affected roots (`HomeScreen`'s top bar, `AppRoot`'s external-link banner).
2. **Default sort silently broke the "first item" contract.** Defaulting `HomeViewModel`'s sort to
   `DATE_DESC` reordered the list before any user interaction, so `video_list_item index:0` was
   "ZZZ Newest Clip" instead of the fixture's actual first-fetched video. AC-LIST-03 (tap index 0 →
   expect `VIDEO_ID_1`) failed as a result. Fixed by making "no sort" (`null`, natural fetch order)
   the default; `VideoSort` is only applied once the user picks an option in `SortPanel`.

Both are documented inline in `tasks.md` Phase 6 and in code comments at the fix sites.

## Self-validation result (mock server, device `25251FDF60029V`)

Ran the full `flows/` suite (14 flows: the 12 scored ACs from `acceptance-criteria.md` +
`AC-LINK-01` + one login smoke overlap) **3 times** after the fixes above:

| Run | Result |
|---|---|
| 1 | 14/14 passed (2m 13s) |
| 2 | 14/14 passed (2m 11s) |
| 3 | 14/14 passed (2m 11s) |

**min / median / max pass rate: 14/14 / 14/14 / 14/14 — no flakiness observed across 3 runs.**

All 12 acceptance criteria in `spec/acceptance-criteria.md` pass: AC-LOGIN-01/02/03, AC-LIST-01/02/03,
AC-COUNT-01, AC-CACHE-01, AC-FILTER-01, AC-SORT-01, AC-MAP-01/02/03. `map_marker_fallback_used=false`
— the accessible chip-row affordance is the documented contract, not a fallback (osmdroid's own pins
are canvas-drawn and genuinely unreachable, per constitution §5).

Quality bar: `./gradlew testDebugUnitTest` — 17 unit tests green (whitelist policy, sort, filter,
and pagination/dedupe/location-enrichment against a fake `YouTubeApi`, no instrumentation needed).
`./gradlew lintDebug` — 0 errors (warnings only: two deprecation notices).

## Real-mode smoke check

Pointed the same build at the real YouTube Data API (`apiBaseUrl=https://www.googleapis.com`,
`apiKey` from `config/secrets.env`, `mockAuthEmail` to skip the (unconfigured) real sign-in):

- List populated with real titles/descriptions/thumbnails aggregated from all 4 configured channels
  (pagination + dedupe exercised against live data, not just the fixture).
- Map screen rendered real osmdroid pins (clustered around France) plus the accessible marker-chip
  row for the located subset.
- Tapping a row fired a real external YouTube launch (deep link succeeded; the specific video
  happened to be unavailable server-side, unrelated to the app).

## Not done / out of scope

- Real Google Sign-In needs a `GOOGLE_SERVER_CLIENT_ID` (or `google-services.json`) that wasn't
  provided in this workspace; the code path is complete and will work once one is supplied.
- Visual design/theming/animations/localization/accessibility beyond the required identifiers —
  explicitly out of scope per `spec/spec.md`.
