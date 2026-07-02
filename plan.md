# Implementation Plan — YouTube Dashboard ("ytdash"), native Android

> Spec-Kit `/plan` artifact for the **frozen** spec (`spec/spec.md`, `spec/acceptance-criteria.md`)
> under `spec/constitution.md`. The spec is framework-neutral; the stack below is the engineering
> decision being evaluated.

## Stack decision & justification

| Concern | Choice | Why |
|---|---|---|
| Language / UI | **Kotlin + Jetpack Compose + Material3** | Idiomatic native Android; declarative UI maps cleanly to the "observable view-state" principle (constitution §1.3). |
| Build | AGP 9.0.1, Gradle 9.1, Kotlin 2.3.20, compileSdk 36, minSdk 29 | What `android create` scaffolds on this machine; JDK 17. |
| Architecture | **MVVM**, unidirectional `StateFlow<AppUiState>` | One observable state object per app; screens render from it, no logic in event handlers. |
| DI | **Hand-rolled `AppContainer`** (constructor injection against interfaces) | Preserves dependency inversion (§1.2) **without** an annotation processor. On a bleeding-edge AGP 9 / Kotlin 2.3.20 toolchain, Hilt/KSP version-coupling is the single biggest build-fragility risk; a plain container removes it while keeping the layer boundary. |
| Networking | **OkHttp + kotlinx.serialization** | kotlinx.serialization is a compiler plugin (already configured, no KSP); OkHttp is version-stable across AGP releases. |
| Persistence | **JSON file cache** (`FileVideoCache`) as the single source of truth | Satisfies §1.5 (UI reads the store; network refreshes it) with a pure, unit-testable codec (`VideoCacheCodec`). Room would re-introduce KSP coupling for no behavioral gain here. |
| Images | **Coil** (`coil-compose`) | Standard Compose image loader; non-fatal if a thumbnail 404s. |
| Map | **osmdroid** + a native accessible marker affordance | OpenStreetMap as required (§Iteration 4). osmdroid pins are Canvas-drawn → unreachable by Maestro, so per constitution §5 each located video also gets a native `map_marker` chip. |
| Auth | **Play Services Auth** (real) + mock bypass (test mode) | Real Google sign-in without Firebase/`google-services.json`; the whitelist is pure domain logic. |

## Layering (constitution §1)
- **data**: `remote/YouTubeApi` (+ DTOs), `cache/VideoCache`, `ChannelConfig`, `VideoRepository`.
- **domain**: `AuthService` (whitelist), `VideoOps` (sort/filter/categories) — pure, no Android.
- **presentation**: `AppViewModel` → `AppUiState`; `App`, `LoginScreen`, `HomeScreen`, `MapScreen`.
- **DI seam**: `di/AppContainer` builds concrete implementations from the runtime `TestConfig`.

## Contracts (the reason the harness can drive the build)
- **Selectors (§3):** `Modifier.testTag(...)` everywhere + `testTagsAsResourceId = true` on the single
  root `Box`. All asserted elements (filter/sort panels, map detail sheet, external-open banner) live
  in the **main composition** — no `DropdownMenu`/`Dialog`/`ModalBottomSheet` — so §5a's popup trap
  can't bite. The `video_list_item` id is placed on the **title `Text`** so `id`+`text` resolve to the
  same node (AC-SORT-01), while a tap still triggers the row's `clickable` (AC-LIST-03).
- **UI-test-mode (§4):** `TestConfig.fromIntent()` reads `uiTestMode`, `mockAuthEmail`, `apiBaseUrl`,
  `apiKey`, `authorizedEmails`, `captureExternalLinks` from intent extras (string or bool tolerant).
- **Map markers (§5):** native `map_marker` chip row over the osmdroid map; tapping selects the video
  and shows an inline `detail_bottom_sheet`. `map_marker_fallback_used=false` (id is a real a11y node).

## Data flow (anti-overfit)
Aggregate **every** configured channel from `config/channels.json` (bundled as an asset, re-synced at
build time): `search.list` **following `nextPageToken` to exhaustion**, dedupe by videoId, then
`videos.list` to enrich `recordingDetails.location`. `category` = the **source channel label** (not
`categoryId`). `video_count` = total loaded. Nothing is read from fixed indices/ids/counts — all from
the API responses — so the same code passes on the hidden held-out dataset.

## Cache / offline (AC-CACHE-01)
`refresh()` replaces the store on success; on network failure it falls back to the persisted list and
returns `StaleFallback` → the UI stays in `CONTENT` (no blocking `error_view`). A hard error is shown
only when there is nothing cached.

## Testing
JVM unit tests for the domain (whitelist, sort, filter, categories) and persistence (cache codec
round-trip). E2E via the Maestro `flows/` suite against the running mock.
