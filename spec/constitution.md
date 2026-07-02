# Project Constitution — YouTube Dashboard ("ytdash")

> **SHARED INPUT.** This file is given **byte-identical** to every build configuration
> (every framework × agent × tier). It is the Spec-Kit `constitution` artifact: the
> non-negotiable principles. It is deliberately **framework-neutral** — it states *what
> qualities the result must have*, never *which library/stack to use*. Choosing the stack
> is the agent's job (the `plan` step) and is part of what we measure.

## 1. Architectural principles (framework-neutral)
1. **Layered separation.** Keep three concerns separable: (a) data access (network + local
   persistence), (b) domain/business logic (auth rules, sorting, filtering), (c) presentation
   (screens, view-state). A change in one layer must not force edits across the others.
2. **Dependency inversion.** Presentation depends on abstractions, not concrete data sources.
   Use whatever dependency-injection mechanism is idiomatic for the chosen stack.
3. **Unidirectional, observable state.** Screens render from explicit, observable view-state
   (loading / content / empty / error). No business logic in UI event handlers.
4. **No blocking work on the UI thread.** Network and disk access run off the main thread.
5. **Single source of truth for data.** The local store is the source of truth the UI reads
   from; the network refreshes the store. (This is what makes offline/cache behavior testable.)
6. **Explicit error handling.** Every failure point (auth, network, parse, persistence, map
   load) resolves to a visible error state with a retry affordance — never a silent failure or
   crash. (These are the real failure points; they must be handled, not hidden.)

## 2. Quality bar
- **Tests:** include unit tests for the domain layer (auth whitelist, sort, filter) and at
  least one persistence test (cache read/write). Aim for meaningful coverage of logic, not UI.
- **Static cleanliness:** the project must pass its ecosystem's standard linter with no errors
  (warnings acceptable but reported).
- **No secrets in source control.** The API key and authorized-email list are supplied at
  runtime / build-time configuration, never committed.
- **Configurable endpoints.** The API base URL must be overridable (build config or runtime),
  so it can point at a mock server. Do not hard-code the production host.

## 3. Selector contract (MANDATORY — this is how the app is validated)
Automated end-to-end validation drives the **compiled app** and selects elements by **stable,
language-independent identifiers**. Every interactive or asserted element listed below MUST
expose the exact logical ID string given here, using the idiomatic stable-identifier mechanism
for the chosen stack:

| Logical ID (use this exact string) | Element |
|---|---|
| `screen_login` | Root of the login screen |
| `login_google_button` | The "Sign in with Google" button |
| `login_error_message` | Error shown when a non-authorized email signs in |
| `screen_home` | Root of the main (post-login) screen |
| `video_list` | The scrollable list container |
| `video_list_item` | Each list row (all rows share this ID) |
| `video_count` | Element in the screen title showing the TOTAL number of loaded videos (its text contains the count) |
| `logout_button` | Logout control (may live behind a visible menu; the menu opener, if any, is `overflow_menu_button`) |
| `refresh_control` | Pull-to-refresh or refresh button |
| `filter_button` | Opens filtering UI |
| `filter_apply_button` | Confirms a filter (omit only if filtering applies instantly) |
| `sort_button` | Opens sorting UI |
| `sort_apply_button` | Confirms a sort (omit only if sorting applies instantly) |
| `map_nav_button` | Control that navigates to the map (button / FAB / menu item) |
| `screen_map` | Root of the map screen |
| `map_marker` | A tappable marker on the map (all markers share this ID; see §5) |
| `detail_bottom_sheet` | The bottom sheet shown after tapping a marker |
| `detail_video_url` | Element in the bottom sheet whose **text is the exact `youtube.com/watch?v=…` URL** of the selected video (so the opened URL can be checked to match the tapped marker) |
| `detail_open_youtube_button` | The "open in YouTube" action inside the bottom sheet |
| `loading_indicator` | Any blocking loading state |
| `error_view` | Any blocking error state |
| `error_retry_button` | Retry control inside `error_view` |
| `external_open_url` | (UI-test-mode, `captureExternalLinks=true`) element whose text = the URL that would be opened externally |
| `external_open_error` | Shown when an external open is **attempted and fails** (`captureExternalLinks=false`): the app surfaces this instead of crashing or silently doing nothing |

**Per-stack mechanism (idiomatic — agent chooses the framework, but MUST honor the ID string):**
- **Compose/native Android:** `Modifier.testTag("video_list_item")` **and** set
  `Modifier.semantics { testTagsAsResourceId = true }` on the tree so test tags surface as
  resource-ids to the device's accessibility/automation layer.
- **Flutter:** wrap with `Semantics(identifier: 'video_list_item', child: …)` — the stable
  `identifier` field (Flutter 3.19+), not `label`, so it's localization-independent.
- **React Native:** `testID="video_list_item"`.

> Rationale: with the same ID strings exposed via each framework's stable-identifier API, **one**
> Maestro flow set runs unchanged on all three frameworks. That single shared harness is a core
> fairness control — do not invent per-framework IDs.

## 4. UI Test Mode contract (MANDATORY — makes E2E deterministic)
The app MUST support a **UI test mode**, activated by launch-intent extras (Android), so the
automated harness can run without external/non-deterministic dependencies. Read these at launch:

| Extra (key) | Type | Effect |
|---|---|---|
| `uiTestMode` | bool | Master switch for all behavior below |
| `mockAuthEmail` | string | When set, tapping `login_google_button` skips the real Google account picker and signs in as this email (then normal whitelist logic runs). |
| `apiBaseUrl` | string | Overrides the API base URL (point at the mock server, or real YouTube). |
| `apiKey` | string | When set, used as the YouTube API key for requests. Lets the harness point a single build at the **real** API (real data) without rebuilding. Read it at RUNTIME from the extras — do NOT bake the key in at compile time only. |
| `authorizedEmails` | string (comma-sep) | Overrides the whitelist for the run. |
| `captureExternalLinks` | bool | When **true**, "open in YouTube" does **not** launch the external app; instead it renders `external_open_url` whose **text equals the target URL** (deterministic correctness check). When **false** (or in production), the app performs the **real** external launch — and if that launch throws/fails, it MUST surface `external_open_error` rather than crash or silently no-op. This is how a real-launch bug (e.g. a broken deep-link call) is caught. |

Outside UI test mode the app behaves normally (real Google sign-in, real external launch).
UI test mode must not weaken production behavior — it only swaps non-deterministic edges
(account picker, external launch, endpoint) for testable ones.

These extras arrive as Android **intent extras** on the launched Activity for *every* framework
(that's how Maestro `launchApp.arguments` are delivered). Each build must surface them to its app
layer: Android reads `intent.extras` directly; **Flutter** reads the host Activity intent via a
MethodChannel; **React Native** reads them via a small native module. Concrete recipes per
framework are in [`../cross-framework-setup.md`](../cross-framework-setup.md) §B. (The v3 reference
apps had *no* way to do this and hardcoded the API base URL — the single biggest reason they
couldn't be driven by an automated harness.)

## 5a. Overlays/popups must keep test identifiers reachable
Elements the harness asserts on MUST be reachable by the automation layer even when they live in a
popup/dialog/sheet/menu. Many UI frameworks render these in a **separate surface** where a
root-level "expose test tags as ids" setting does not apply (e.g. Compose `DropdownMenu`/`Dialog`/
`ModalBottomSheet`; Flutter dialogs as separate routes). For every such element in the selector
contract (notably `logout_button`, `detail_bottom_sheet`, `detail_open_youtube_button`,
`external_open_url`), ensure its identifier is exposed — either by keeping it in the main view tree
or by re-applying the framework's id-exposure mechanism on the popup's own root.

## 5. Map markers must be reachable (the cross-framework crux)
The map is OpenStreetMap-based. **Map-rendered markers are usually NOT reachable by a black-box
automation tool** — this is confirmed across the stacks: osmdroid draws markers on a Canvas (no
accessibility nodes), and a Leaflet-in-WebView map renders them in the WebView DOM (test ids don't
cross into it). Only widget-based maps (e.g. flutter_map) can tag the marker itself.

Therefore every build MUST expose `map_marker` (and the detail it opens) on a **native, accessible
affordance** — an overlay button or a "markers" list row per located video — not on the rendered
pin alone. Tapping it must select that video and show `detail_bottom_sheet` (with
`detail_open_youtube_button`). If a build can only tap markers by screen coordinate, set
`map_marker_fallback_used=true` in the run manifest — an unreachable marker is a real finding about
that stack, not a flow to fudge. Full per-framework detail in
[`../cross-framework-setup.md`](../cross-framework-setup.md) §C.

## 6. What this constitution intentionally does NOT say
It does not name a state-management library, a DI framework, an HTTP client, a database, a
navigation library, or a map widget. Those are the agent's decisions in the `plan` step, and
their quality is exactly what the experiment measures. Do not ask for clarification on the
stack — choose the idiomatic one and proceed.
