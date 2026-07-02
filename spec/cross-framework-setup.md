# Cross-framework setup — making the ONE Maestro flow set work on Android, Flutter & RN

> Grounded in the three v3 best-run repos (android_claude/run3, flutter_claude/run2,
> rn_codex/run3). **Key finding from all three: none of them could be driven by a black-box UI
> tool, and none could be pointed at a mock server.** They had no stable test IDs, no test mode,
> and hardcoded the YouTube base URL. v4's constitution fixes this — this doc is the concrete
> per-framework recipe each build must follow so the *same* `flows/AC-*.yaml` run unchanged.

The flows select elements by `id:` (and sometimes visible `text:`). Maestro maps `id` to the
platform's accessibility/resource id. Three things must hold on every build:

| Contract | Android (Compose) | Flutter | React Native (Expo) |
|---|---|---|---|
| **A. Stable selector** (constitution §3) | `Modifier.testTag("x")` + `testTagsAsResourceId = true` | `Semantics(identifier: "x")` (Flutter 3.19+) | `testID="x"` |
| **B. Read launch args** (constitution §4) | `intent.extras` (native) | MethodChannel reading the host Activity intent | native/Expo module reading the launch intent extras |
| **C. Accessible markers** (constitution §5) | osmdroid markers are **canvas → not reachable**; expose a native marker affordance | flutter_map markers are widgets → wrap child in `Semantics(identifier:"map_marker")` | Leaflet-in-WebView markers are **DOM → not reachable**; expose a native marker affordance |

What the v3 repos actually did (so the v4 build does NOT repeat it):

| | Android run3 | Flutter run2 | RN(codex) run3 |
|---|---|---|---|
| Test IDs | none (one dynamic `contentDescription`) | none (no `Key`/`Semantics`) | none (no `testID`) |
| Base URL override | hardcoded in `NetworkModule` | hardcoded `_baseUrl` const | hardcoded in datasource |
| Launch args read | no | no | no (scheme declared, params ignored) |
| Map | osmdroid (native canvas) | flutter_map (widgets) | **WebView + Leaflet** |
| Cache | Room, 24h TTL, stale-fallback | sqflite, 24h TTL, stale-fallback | expo-sqlite, 24h TTL, stale-fallback |

---

## A. Stable selectors — already in the constitution, here's the per-framework "gotcha"
- **Android:** set `testTagsAsResourceId = true` once on a high node, then `testTag` everywhere.
  **But popups don't inherit it** — `DropdownMenu`/`Dialog`/`ModalBottomSheet` are separate
  composition windows (we hit this in the smoke test; constitution §5a). Keep asserted elements in
  the main tree or re-apply the flag on the popup root.
- **Flutter:** use the `identifier` field of `Semantics`, **not** `label` (label is localized;
  identifier is stable and surfaces to Maestro as the element `id`). Wrap each interactive widget:
  `Semantics(identifier: 'login_google_button', button: true, child: ...)`. Flutter dialogs/sheets
  are separate routes but still in the same semantics tree, so they're reachable (unlike Compose
  popups) — still, give the sheet + its button identifiers. Call
  `SemanticsBinding.instance.ensureSemantics()` in `main()` so the tree builds for Maestro.
  **Verified working (flutter-claude-flagship: 6/6 with the same flows).** Common agent footgun
  this caught: `setState(() => x = future)` (arrow body) *returns* a Future → Flutter throws
  "setState callback returned a Future" and renders its error overlay (no semantics → Maestro sees
  an empty screen). Use a **block body**: `setState(() { x = future; })`.
- **React Native:** `testID="login_google_button"`. Maestro maps it to the Android resource-id.
  Put `testID` on the `Pressable`/`Touchable` itself, not only the inner `Text`.

## B. Reading launch args (the UI-test-mode contract) — the part v3 totally lacked
Maestro `launchApp.arguments` are delivered as **Android intent extras** to the launched Activity,
**regardless of framework**. So every build runs in an Android Activity that receives them — the
only per-framework work is surfacing those extras to your app layer. This keeps the flows
identical (they all use `launchApp.arguments`).

**Android (proven in the smoke build):**
```kotlin
val cfg = TestConfig.fromIntent(intent)   // intent.extras.getBoolean("uiTestMode"), getString("apiBaseUrl"), …
```

**Flutter** — read the host Activity's intent via a MethodChannel at startup:
```kotlin
// android/app/src/main/kotlin/.../MainActivity.kt
class MainActivity : FlutterActivity() {
  override fun configureFlutterEngine(engine: FlutterEngine) {
    super.configureFlutterEngine(engine)
    MethodChannel(engine.dartExecutor.binaryMessenger, "ytdash/testconfig")
      .setMethodCallHandler { call, result ->
        if (call.method == "get") {
          val e = intent.extras
          result.success(mapOf(
            "uiTestMode" to (e?.getBoolean("uiTestMode") ?: false),
            "mockAuthEmail" to e?.getString("mockAuthEmail"),
            "apiBaseUrl" to e?.getString("apiBaseUrl"),
            "authorizedEmails" to e?.getString("authorizedEmails"),
            "captureExternalLinks" to (e?.getBoolean("captureExternalLinks") ?: false),
          ))
        } else result.notImplemented()
      }
  }
}
```
```dart
// lib/.../test_config.dart  — call once in main() before runApp()
const _ch = MethodChannel('ytdash/testconfig');
final cfg = await _ch.invokeMapMethod<String, dynamic>('get');
```
(Plain `--dart-define` is compile-time and can't be set by Maestro per-run, so the MethodChannel is
the runtime path. `--dart-define-from-file` is fine for the *secrets* but not for per-run test mode.)

**React Native (Expo)** — extras aren't exposed to JS by default (RN's `Linking` only surfaces VIEW
intents, not extras). Add a tiny native/Expo module that reads them:
```kotlin
// android module
@ReactMethod fun getTestConfig(promise: Promise) {
  val e = currentActivity?.intent?.extras
  val map = Arguments.createMap().apply {
    putBoolean("uiTestMode", e?.getBoolean("uiTestMode") ?: false)
    putString("mockAuthEmail", e?.getString("mockAuthEmail"))
    putString("apiBaseUrl", e?.getString("apiBaseUrl"))
    putString("authorizedEmails", e?.getString("authorizedEmails"))
    putBoolean("captureExternalLinks", e?.getBoolean("captureExternalLinks") ?: false)
  }
  promise.resolve(map)
}
```
```ts
const cfg = await NativeModules.TestConfig.getTestConfig(); // read once in app root _layout
```
*Alternative (no native code):* drive the app with a deep link instead of launch args —
`- openLink: ytdash://test?uiTestMode=1&apiBaseUrl=...` in the flow + `useLocalSearchParams` in
`_layout`. This works but makes the RN flow *differ* from Android/Flutter, weakening the
single-harness fairness property. Prefer the native module.

**Verified working (rn-claude-flagship: 6/6 with the same flows).** Implemented as a local Expo
module (`npx create-expo-module --local`), Kotlin `Function("get")` reading
`appContext.currentActivity?.intent?.extras`. Two RN gotchas this surfaced:
- **Build `assembleRelease`, not debug, for harness runs.** A RN *debug* APK loads its JS from a
  Metro dev server and won't run standalone under Maestro; *release* embeds the bundle. (Expo's
  generated release buildType is debug-signed, so it's runnable without a keystore.)
- **Cleartext to the mock:** add `usesCleartextTraffic: true` via `expo-build-properties` so the
  app can reach `http://10.0.2.2:8080`.

## C. Map markers — the real cross-framework crux
**Map-rendered markers are generally NOT reachable by a black-box tool:**
- **osmdroid (Android)** draws markers on a `Canvas` → no accessibility nodes. The v3 Android app
  tapped markers by screen coordinate only.
- **Leaflet-in-WebView (RN)** renders markers in the WebView DOM → `testID` can't cross into it.
  (The v3 RN SPEC §13.3 documents the marker/gesture conflict explicitly.)
- **flutter_map (Flutter)** is the exception — markers are real widgets, so a child wrapped in
  `Semantics(identifier: 'map_marker')` IS reachable.

**Therefore the fair, uniform contract (constitution §5):** every build must expose `map_marker`
(and the detail it opens) on a **native, accessible affordance** — not rely on the rendered pin.
Cleanest uniform option: render, next to/over the map, an accessible element per located video
(e.g. an overlay button or a "markers" list row) carrying `map_marker`; tapping it selects that
video and shows `detail_bottom_sheet`. flutter_map can additionally tag the real marker. RN's
WebView signals marker taps to native via `postMessage` (its bottom sheet is native and can carry
`detail_bottom_sheet`/`detail_open_youtube_button` ids) — but to let Maestro *initiate* the tap,
the native marker affordance is still required.

`AC-MAP-01/02/03` are written against `map_marker` / `detail_bottom_sheet` /
`detail_open_youtube_button` precisely so this native affordance satisfies them on all three. If a
build can only do coordinate taps, record `map_marker_fallback_used=true` in the run manifest — an
unreachable marker is a real finding about that stack, not a flow to fudge.

**Verified working (rn-claude-flagship iteration 4: AC-MAP-01/02/03 PASS, 9/9 overall).** The RN
build renders a **real Leaflet-in-WebView map** (the idiomatic v3 stack — 5 OSM pins at the fixture
coords) AND a **native marker overlay**: a horizontal row of `map_marker` `Pressable`s, one per
located video, below the WebView. Confirmed by screenshot that the WebView pins render but only the
native chips are selectable by Maestro — exactly the §5 split this contract predicts. Tapping a
native `map_marker` sets `selected` → a native `detail_bottom_sheet` View (with
`detail_open_youtube_button`) overlays the map; the YouTube button feeds the same app-root
`external_open_url` banner as the list. Two implementation notes:
- **Lift `captured`/`external_open_url` to the app root**, not per-screen — the same banner must
  serve both the list (iteration 1) and the map sheet (iteration 4) without duplication.
- **The WebView's DOM markers also `postMessage` pin-tap → native `setSelected`**, so human taps on
  the real pins open the same native sheet. But Maestro can only *initiate* via the native chip —
  the postMessage path is the human path, the native chip is the harness path. Keep both.
- The native sheet is a plain absolutely-positioned `View` (not a separate window), so unlike the
  Compose popup footgun (§A / constitution §5a) its `testID`s are reachable with no extra work.

**Verified working (flutter-claude-flagship iteration 4: AC-MAP-01/02/03 PASS, 9/9 overall).**
flutter_map is the **exception** the contract predicts: markers are real widgets, so wrapping each
marker child in `Semantics(identifier: 'map_marker')` makes the *rendered pin itself* reachable — no
separate overlay strictly required. Confirmed empirically: AC-MAP-02's index-0 tap landed on a real
flutter_map marker (Berlin/"Tech Deep Dive"), not a fallback chip. We still render the
guaranteed-visible "Markers" `ActionChip` row (also `map_marker`) for parity and so the affordance
survives a pin scrolling off-viewport. `detail_bottom_sheet` is an `Align`+`Material` overlay in the
same semantics tree (Flutter routes share the tree, so even a real bottom-sheet route would be
reachable — unlike Compose popups). `map_marker_fallback_used=false`.

**Verified working (android-claude-flagship iteration 4: AC-MAP-01/02/03 PASS, 9/9 overall).**
osmdroid via `AndroidView` — markers are **Canvas-drawn, no a11y nodes → unreachable** (the rule, not
the exception). The reachable affordance is a Compose `AssistChip` row (one `map_marker` per located
video) in the MAIN composition; `detail_bottom_sheet` is an inline `Surface` (NOT `ModalBottomSheet`
— that hits the §5a separate-window trap). Confirmed by screenshot: the 5 osmdroid pins render but
the index-0 tap resolved to the native chip. Set `Configuration.getInstance().userAgentValue =
ctx.packageName` in the `AndroidView` factory or OSM tile fetches 403. `map_marker_fallback_used=false`.

**Net cross-engine result:** the §5 contract holds on all three map engines. Two of three (osmdroid
canvas, Leaflet/WebView DOM) genuinely *cannot* expose the rendered marker and require the native
affordance; flutter_map can, but the same affordance pattern works uniformly — which is exactly why
the one flow set drives all three unchanged.

## D. Cache / filter / sort — verified on all three (12/12 each)
All three builds now pass the full 12 ACs. Cache, filter, and sort were the last 3; none needed a
new selector, but two harness disciplines emerged that every build must follow:

1. **Persist the cache to disk, not just memory (AC-CACHE-01).** The offline relaunch is a *fresh
   process* (`clearState:false`), so an in-memory cache is gone. Each build persists the last good
   list and falls back to it on network error with **no blocking `error_view`**: RN
   `@react-native-async-storage/async-storage`, Flutter `shared_preferences`, Android
   `SharedPreferences` (JSON). The store is the agent's choice (v3 used sqlite/sqflite/Room) — only
   the *behavior* (replace-on-refresh + stale-fallback) is speced.

2. **Filter/sort options open in a panel that REPLACES the list while open.** The flows select the
   option by visible text (`(?i)tech`, `(?i)date.*(desc|newest)`). If the list stays on screen, that
   text collides with item titles ("Tech Talk One" contains "tech") and a black-box driver can't
   disambiguate by occlusion. Replacing the list while the panel is open removes the collision on all
   three.

3. **Sort option labels must END with the regex keyword.** Maestro `text:` is a full-string match
   (`matches()`), so `(?i)date.*(desc|newest)` matches "Date — newest" but **not** "Date (newest
   first)". Same anchoring the AC-LIST flows already encode with leading/trailing `.*`.

4. **Compose-only: put the list-item id on the title `Text`, not the clickable row.** AC-SORT-01
   asserts `id:video_list_item index:0 text:"ZZZ…"` — the id and the text must resolve to the *same*
   node. RN (`testID` on the row) and Flutter (`Semantics(identifier:)` on the row) **aggregate
   descendant text automatically**, so the row node carries its title. **Compose does not:** Maestro
   reads the *unmerged* semantics tree, where a clickable row's resource-id node has empty text and
   the title is a separate child — and neither `mergeDescendants` nor `contentDescription` surfaced
   text onto it. Tagging the title `Text` directly is deterministic; a tap on it still triggers the
   enclosing row's `clickable`, so AC-LIST-03 (tap row index 0) keeps passing. This is a genuine
   framework asymmetry — a good slide on "the same logical contract, three different a11y models."

---

## Reuse from the v3 repos (worth copying into each v4 build)
1. **Config CI pattern** — gitignored real config + a committed dummy/template (`test@example.com`
   / `DUMMY_API_KEY`) + a script that generates placeholders so CI builds without secrets. All
   three did this well; keep it, and **add the missing base-URL override** to the same mechanism.
2. **"Common Agent Mistakes" section** — every v3 SPEC had one and it paid off. Keep per-framework
   gotchas in each build's notes (e.g. RN: use Leaflet-in-WebView not react-native-maps; Flutter:
   Firebase `duplicate-app` try/catch; Android: Hilt/KSP + theme-crash).
3. **Cache contract** — 24h TTL, replace-on-refresh, **stale-cache fallback on network error**.
   Identical across all three v3 builds → a clean framework-neutral behavior to assert (AC-CACHE-01).
4. **Typed-error + sealed UI-state** (`Result`/`Either`/`Result<T>` + Loading/Empty/Content/Error)
   — all three converged on this; it's the state contract the harness asserts against.
5. **Geocoding resilience** (in-memory cache keyed to 3-decimal coords, rate-limit, backoff,
   platform→Nominatim fallback, OSM User-Agent) — non-obvious, easy to break; spec it once.
6. **Shared data contract** — same 4 channel IDs, 24h TTL, OSM User-Agent, YouTube endpoints/params
   → one mock server serves all three frameworks (which is exactly what `fixtures/` does).

## Reference stacks — what the v3 best runs actually chose
Not prescriptive (the v4 agent picks its own stack — that's what we measure), but a useful sanity
reference for what an idiomatic build looks like per framework:

| Concern | Android (Claude) | Flutter (Claude) | RN/Expo (Codex) |
|---|---|---|---|
| Language/SDK | Kotlin 2.2, minSdk 29, compile 36 | Dart 3.9, flutter_lints | Expo SDK 54, RN 0.81, Hermes on, New Arch on |
| UI | Compose + Material3 (+ some legacy Views — a wart) | Flutter widgets | RN + expo-router (file-based) |
| State | MVVM, `StateFlow` sealed UiState | `flutter_bloc` (freezed unions) | `zustand` (2 stores) |
| DI | Hilt/Dagger | `get_it` + `injectable` | manual singleton container |
| Net/JSON | Retrofit + OkHttp + kotlinx.serialization | `http` + `freezed`/json_serializable | `fetch` + `zod` |
| Errors | sealed `Result`/`Failure` | `dartz` `Either<Failure,T>` | `Result<T>` monad |
| Cache | Room (24h TTL, stale-fallback) | `sqflite` (24h TTL, stale-fallback) | `expo-sqlite` (24h TTL, stale-fallback) |
| Maps | **osmdroid** (native canvas) | **flutter_map** (widgets) | **WebView + Leaflet** (DOM) |
| Auth | Play Services Auth + Firebase | firebase_auth + google_sign_in | @react-native-google-signin + Firebase |
| Tests | ~none real (only a string util) | 3 BLoC tests, no widget/E2E | stub only (~0% real) |

Two things every v3 build got wrong and v4 must not: **(1) zero stable test IDs**, **(2) no
runtime base-URL / test-mode seam.** The constitution's §3 + §4 contracts exist precisely to close
these — that's the difference between "27 apps you tested by hand" and "36 apps an agent validates."
