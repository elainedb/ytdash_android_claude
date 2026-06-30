# Maestro flows — the validation harness

> **NOT a builder input.** These flows are the oracle. Builders see only `memory/` + `specs/`.
> Builders satisfy the *acceptance criteria*; these flows verify them. Keeping the harness hidden
> stops agents from optimizing to the test.

## Why Maestro (recap from playbook §3)
Maestro drives the **compiled APK** externally — no per-framework SDK or instrumentation — and
selects by the stable IDs the constitution mandates:
- **Flutter** → Semantics `identifier` (Flutter 3.19+), exposed to Maestro as `id`.
- **React Native** → `testID`, exposed as `id`.
- **Compose/native** → `testTag` + `testTagsAsResourceId = true`, exposed as resource-id/`id`.

So **one** flow set runs unchanged on all three frameworks. That single shared harness is the
fairness lever; do not fork it per framework.

## The contract these flows assume (from `constitution.md §4`)
The app honors UI-test-mode launch extras. Every flow launches with:
- `uiTestMode=true`
- `mockAuthEmail=<email>` — tapping `login_google_button` signs in as this email (no Google dialog)
- `apiBaseUrl=<mock>` — point at your mock server (set it in `MOCK_API_BASE` below)
- `authorizedEmails=<csv>` — the whitelist for the run
- `captureExternalLinks=true` — "open YouTube" surfaces `external_open_url` (text = the URL) instead of launching

## Run it
```bash
# one config / one build (target your device explicitly — --device is REQUIRED when more than one
# emulator/phone is attached; MOCK_API_BASE is 10.0.2.2:<port> on an emulator, 127.0.0.1:<port> on a
# physical device reached via `adb reverse`):
maestro --device <your-device-id> test \
  -e APP_ID=com.example.ytdash \
  -e MOCK_API_BASE=http://10.0.2.2:8080 \
  -e AUTHORIZED_EMAIL=allow@example.com \
  -e UNAUTHORIZED_EMAIL=deny@example.com \
  --format junit --output results/<config>.xml \
  flows/

# the JUnit XML → % ACs passing (the primary functional metric)
```
Run on a clean emulator/device with the **same image** for every build. Run **3×** per config and
keep min/median/max (per playbook §5). The mock server must serve the fixture from `spec.md §Data`.

> ⚠️ **Invocation gotcha (verified on Maestro 2.6.1):** pass the `-e` flags **directly** (or via a
> bash array `ARGS=(-e K=V ...); maestro test "${ARGS[@]}" flow.yaml`). Do **not** collapse them
> into a single unquoted shell string (`ENV="-e K=V -e ..."; maestro test $ENV ...`) — that path
> silently fails arg-parsing and every `${APP_ID}` resolves to `undefined` ("Unable to launch app
> undefined").

## Mock server (you run this; ~10 lines)
Any tool works (WireMock, `json-server`, MSW, a tiny Express app). It must:
- serve the 8-video fixture at the videos endpoint your `apiBaseUrl` points to,
- be reachable from the device (`10.0.2.2` is the host loopback from an Android emulator),
- return identical bytes every run (this is what kills the v1–v3 YouTube-quota flakiness).

## Findings from the first real run (android-claude-flagship, Maestro 2.6.1)
These shaped the flows as written — keep them in mind when validating the other builds:
1. **Login is inlined in every flow, not delegated to a subflow.** Maestro 2.6.1 does **not**
   resolve a `runFlow`-invoked subflow's `appId:` header against the caller's env (even when
   passed explicitly via `env:`), so a shared `login-as` subflow launched as "app undefined".
   Each flow therefore does its own top-level `launchApp` (whose `appId` resolves correctly from
   the flow header). The login block is identical across flows; only `mockAuthEmail` differs.
2. **Compose popups break `testTagsAsResourceId`.** `DropdownMenu`, `AlertDialog`, `Dialog`, and
   `ModalBottomSheet` render in **separate composition windows**, so a root-level
   `testTagsAsResourceId = true` does **not** reach testTags inside them — Maestro can't see those
   `id:`s. The Android build keeps harness-asserted elements (logout, captured URL, map bottom
   sheet) in the **main composition** (or sets `testTagsAsResourceId` on each popup's own root).
   The Flutter/RN builds will have their own analogue (e.g. Flutter dialogs are separate routes) —
   this is a real per-framework thing the constitution now calls out (§5a).

## Known cross-framework caveat: map markers
Native overlay markers and WebView/canvas markers expose to accessibility differently. The
constitution requires markers to carry `map_marker` in the a11y tree (constitution §5). If a
given build genuinely can't make individual markers accessible, `AC-MAP-02` may need the
cluster/list affordance fallback. **Record any such fallback in the run manifest** — an
unreachable marker is a real finding about that framework/stack choice, not a flow bug to paper over.

## Flakiness discipline
- Flows use `extendedWaitUntil` with explicit timeouts, not fixed sleeps.
- A criterion that passes 2/3 runs is reported as flaky, not averaged away.
- If a flow fails, capture `maestro test --debug-output` and the screen recording before re-running.
