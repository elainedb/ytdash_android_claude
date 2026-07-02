# Implementation Plan: ytdash (Android)

**Spec**: `spec/spec.md` + `spec/acceptance-criteria.md` (frozen) | **Constitution**: `spec/constitution.md`

## Summary

A single-Activity, all-Compose Android app (Kotlin) that signs a user in (Google / Credential
Manager, with a UI-test-mode mock bypass), aggregates videos from a configured set of YouTube
channels (paginated, deduped), caches them in Room as the single source of truth, and lets the
user filter/sort the list and view geolocated videos on an OpenStreetMap (osmdroid) map. Built
directly against the constitution's selector/UI-test-mode/map contracts so the shared Maestro
flow set drives it unmodified.

## Technical Context

**Language/Version**: Kotlin 2.2.21, JDK 17

**Primary Dependencies**: Jetpack Compose + Material3, Hilt 2.57.2 (DI), Retrofit2 + OkHttp +
kotlinx.serialization (network), Room 2.8.4 (cache), Navigation-Compose, osmdroid 6.1.20 (map),
Coil3 (image loading), Credential Manager + Google ID (real auth)

**Storage**: Room (SQLite) — single source of truth for the video list

**Testing**: JUnit4 + Truth for domain-layer unit tests (`app/src/test`); an instrumented Room
persistence test (`app/src/androidTest`, runs on-device since Robolectric isn't in this
environment's dependency cache); Maestro end-to-end flows (`flows/`) as the acceptance oracle

**Target Platform**: Android, minSdk 26 / targetSdk 35 / compileSdk 35

**Project Type**: mobile-app (single Gradle module, `:app`)

**Performance/Constraints**: no network/disk work on the main thread (Retrofit + Room are both
suspend-based); offline-capable via the Room cache

**Scale/Scope**: 3 screens (login, home/list, map), 4 configured source channels, single APK

## Stack rationale (what's being evaluated)

- **Compose + Material3, single Activity + Navigation-Compose.** The app is 3 screens with no
  need for deep-linking or multi-module isolation — Navigation-Compose is the idiomatic minimum
  that still gives a real back stack (login → home → map) instead of hand-rolled screen state.
- **MVVM with sealed `UiState` per screen, `StateFlow`-driven.** Matches constitution §1.3
  (unidirectional, observable state: Loading/Content/Error, and Empty is represented as an
  empty-content case of `Content` — see `HomeUiState`). No business logic in Composables; all of
  it lives in `ViewModel`s and `domain/usecase/*`.
- **Hilt for DI** (constitution §1.2, dependency inversion): `domain/repository/*` are interfaces,
  bound to `data/repository/*Impl` via `@Binds` in `RepositoryModule`. ViewModels depend only on
  the domain interfaces and use cases, never on Retrofit/Room types directly.
- **Retrofit + OkHttp + kotlinx.serialization**, with the base URL and API key resolved **per
  request** by an OkHttp interceptor reading a single mutable `RuntimeConfig` (constitution §4).
  This is the key design choice that makes `apiBaseUrl`/`apiKey` genuinely runtime-configurable
  (not just at Retrofit-construction time): `MainActivity.onCreate` parses the launch intent's
  extras into `RuntimeConfig` *before* `setContent`, and every subsequent API call picks it up —
  so the exact same installed APK talks to the mock or the real YouTube API with no rebuild.
- **Room as the single source of truth** (constitution §1.5): the UI never renders "network
  result" directly — `VideoRepository.observeVideos()` reads Room; `refresh()` fetches the
  network and replaces the table on success. On network failure, `refresh()` returns
  `Result.failure` but the Room-backed flow keeps emitting the last good rows — this is what
  makes AC-CACHE-01 (offline relaunch, no rebuild needed) fall out of the architecture rather
  than needing special-cased offline logic.
- **osmdroid for the map** (native canvas — the same choice flagged in
  `cross-framework-setup.md` as the "rule, not the exception" for a11y). Since osmdroid markers
  have no accessibility nodes, `map_marker` is exposed via a **separate native `AssistChip` row**
  (one chip per located video) rendered in the same Compose tree as the map — this is the
  constitution §5 "native accessible affordance" requirement, not a workaround bolted on after
  the fact.
- **No `ModalBottomSheet`/`DropdownMenu`/`Dialog` for anything the harness asserts on**
  (constitution §5a): `detail_bottom_sheet` is a plain inline `Surface` overlaid via `Box`
  alignment in the *same* composition as the map, and `logout_button` is a direct `TopAppBar`
  action (no overflow menu) — both sidestep the Compose popup / separate-composition-window trap
  that breaks `testTagsAsResourceId` propagation.
- **`video_list_item` tag lives on the row's title `Text`, not the row container** — Maestro
  reads Compose's *unmerged* semantics tree, where a clickable row's own resource-id node carries
  no text; tagging the title directly is what lets `AC-SORT-01` read `index:0`'s text. A tap on
  the title still bubbles to the row's `clickable` (untouched children don't consume touches in
  Compose), so `AC-LIST-03`'s tap-index-0 still opens the row.
- **Filter/sort panels replace the list while open**, and their option labels are plain,
  undecorated text (category label verbatim; sort labels end in "Newest"/"Oldest"/etc.) — this
  matches the exact full-string `text:` regex matching the flows use
  (`(?i)${FILTER_LABEL}`, `(?i)date.*(desc|newest)`) and removes the title/option-text collision
  the cross-framework doc calls out.
- **`ExternalLinkViewModel` is Activity-scoped**, shared by both the home list and the map
  bottom sheet via a single `hiltViewModel()` call at the `MainActivity` root, passed down as a
  parameter — so `external_open_url`/`external_open_error` is one banner, not two.
- **No hardcoded channel/category shortcuts** (anti-overfit): the source-channel list is read
  from `config/channels.json` (copied into `assets/` at build time by a Gradle task, not
  inlined in Kotlin), and `category` is taken from each API response's `snippet.channelTitle` —
  never a fixture-specific string — so the same code aggregates any hidden channel set.

## Project Structure

```text
app/src/main/java/com/example/ytdash/
├── MainActivity.kt, YtDashApp.kt
├── core/
│   ├── testmode/TestConfig.kt        # parses launch-intent extras (constitution §4)
│   ├── config/RuntimeConfig.kt       # mutable runtime base-url/key/whitelist/test-mode state
│   ├── di/                           # Hilt modules: Network, Database, Repository bindings
│   └── link/                         # app-root external-open capture/real-launch + banner state
├── domain/
│   ├── model/                        # Video, GeoLocation, SortOption (pure Kotlin)
│   ├── repository/                   # VideoRepository, AuthRepository interfaces
│   └── usecase/                      # IsAuthorizedEmail, SortVideos, FilterVideos (unit-tested)
├── data/
│   ├── remote/                       # YouTubeApiService + DTOs (mirrors spec/youtube-api.md)
│   ├── local/                        # Room VideoEntity/VideoDao/AppDatabase
│   ├── channels/                     # reads config/channels.json from assets
│   ├── repository/                   # VideoRepositoryImpl (pagination+merge), AuthRepositoryImpl
│   └── mapper/                       # Entity <-> domain Video
└── presentation/
    ├── login/, home/, map/           # Screen + ViewModel + UiState per feature
    ├── navigation/                   # single NavHost (login -> home -> map)
    └── common/                       # LoadingView/ErrorView/ExternalLinkBanner (shared states)

app/src/test/                          # domain unit tests (whitelist, sort, filter)
app/src/androidTest/                   # Room persistence test
```

**Structure Decision**: single Gradle module (`:app`); layered by responsibility
(`domain`/`data`/`presentation`) rather than by feature module, matching the app's size (3
screens) — a multi-module split would add build complexity with no testability benefit here.

## Constitution Check

- Layered separation / dependency inversion: ✅ ViewModels → domain interfaces → Hilt-bound impls.
- Unidirectional observable state: ✅ sealed `*UiState` per screen, `StateFlow`, no logic in
  Composables.
- No main-thread blocking work: ✅ Retrofit/Room are both suspend/Flow-based.
- Single source of truth: ✅ Room; network only ever writes to it, UI only ever reads from it.
- Explicit error handling: ✅ `error_view`/`error_retry_button` on the list, `login_error_message`
  on login, `external_open_error` on failed external launch — no bare try/catch-and-ignore.
- Selector/UI-test-mode/map contracts: ✅ see stack rationale above; every ID in constitution §3
  is wired to exactly the composable named in this plan.

No violations to record in Complexity Tracking.
