# Acceptance Criteria — YouTube Dashboard ("ytdash")

> **SHARED INPUT** and the **single source of truth** for both automated validation
> (`flows/AC-*.yaml`) and the judge's functional rubric (`eval/rubric.md`). Each criterion has a
> stable ID, a Given/When/Then, the selectors it touches, the Maestro flow that checks it, and
> the fixture facts it relies on. Functional score = **% of these criteria that pass** (measured
> identically on every build).

Legend: IDs from the constitution **selector contract** are in `code`. Fixture facts come from
`spec.md §Data`.

| AC-ID | Given / When / Then | Selectors | Flow | Fixture dependency |
|---|---|---|---|---|
| **AC-LOGIN-01** | Given an authorized email, when the user taps `login_google_button`, then `screen_home` and `video_list` appear. | `login_google_button`, `screen_home`, `video_list` | `AC-LOGIN-01.yaml` | `AUTHORIZED_EMAIL` on whitelist |
| **AC-LOGIN-02** | Given a non-authorized email, when the user signs in, then `login_error_message` appears and `screen_home` does **not**. | `login_google_button`, `login_error_message`, `screen_home` | `AC-LOGIN-02.yaml` | `UNAUTHORIZED_EMAIL` not on whitelist |
| **AC-LOGIN-03** | Given a signed-in user, when they tap `logout_button`, then `screen_login` (with `login_google_button`) reappears. | `logout_button`, `login_google_button` | `AC-LOGIN-03.yaml` | — |
| **AC-LIST-01** | Given login, when the home screen loads, then `video_list` shows at least one `video_list_item`, and the known title `"ZZZ Newest Clip"` is present. | `video_list`, `video_list_item` | `AC-LIST-01.yaml` | 8 videos incl. `"ZZZ Newest Clip"` |
| **AC-LIST-02** | Given a loaded list, when the user triggers `refresh_control`, then the list is still populated (re-fetch succeeds, no error state). | `refresh_control`, `video_list_item`, `error_view` | `AC-LIST-02.yaml` | mock server returns 200 |
| **AC-LIST-03** | Given a loaded list, when the user taps the first `video_list_item`, then the external open is captured: `external_open_url` shows `https://www.youtube.com/watch?v=VIDEO_ID_1`. | `video_list_item`, `external_open_url` | `AC-LIST-03.yaml` | first video → `VIDEO_ID_1`; `captureExternalLinks=true` |
| **AC-COUNT-01** | Given login, when the home screen loads, then the title's `video_count` shows the total number of loaded videos (`VIDEO_COUNT`, = 8 for the fixture). The mock paginates each channel, so this passes only if the build fetched **all pages** of every channel. | `video_count` | `AC-COUNT-01.yaml` | mock serves 8 videos across multiple pages |
| **AC-CACHE-01** | Given a list loaded once, when the network is disabled and the app is relaunched, then cached `video_list_item`s still appear and no `error_view` blocks the screen. | `video_list_item`, `error_view` | `AC-CACHE-01.yaml` | data persisted after first load |
| **AC-FILTER-01** | Given the list, when the user filters to the first source channel's category (`FILTER_LABEL`, from config), then only that bucket remains — a known in-bucket title (`Tech Talk One`) stays, a known out-of-bucket title (`ZZZ Newest Clip`) goes. | `filter_button`, `filter_apply_button`, `video_list_item` | `AC-FILTER-01.yaml` | displayed category = configured channel label; titles per fixture |
| **AC-SORT-01** | Given the list, when the user sorts by date descending, then the first `video_list_item` is `"ZZZ Newest Clip"`; sorting ascending puts `"AAA Oldest Clip"` first. | `sort_button`, `sort_apply_button`, `video_list_item` | `AC-SORT-01.yaml` | known newest/oldest titles |
| **AC-MAP-01** | Given login, when the user taps `map_nav_button`, then `screen_map` appears with at least one `map_marker` (fixture has 5 located videos). | `map_nav_button`, `screen_map`, `map_marker` | `AC-MAP-01.yaml` | 5 videos with `location` |
| **AC-MAP-02** | Given the map, when the user taps a `map_marker`, then `detail_bottom_sheet` appears containing `detail_open_youtube_button`. | `map_marker`, `detail_bottom_sheet`, `detail_open_youtube_button` | `AC-MAP-02.yaml` | located video has details |
| **AC-MAP-03** | Given the bottom sheet for the tapped marker, when the user taps `detail_open_youtube_button`, then `external_open_url` shows **exactly** the sheet's `detail_video_url` (i.e. the opened video corresponds to the tapped marker, whichever it is). | `detail_video_url`, `detail_open_youtube_button`, `external_open_url` | `AC-MAP-03.yaml` | `captureExternalLinks=true`; URL captured from the sheet |
| **AC-LINK-01** | Given login with `captureExternalLinks=false` (real launch), when the user taps the first `video_list_item`, then the external open succeeds — **no `external_open_error`** appears (catches a broken real-launch/deep-link, e.g. the RN `Linking` bug). | `video_list_item`, `external_open_error` | `AC-LINK-01.yaml` | emulator has a handler for `https` URLs; real launch path exercised |

## Scoring
- **Pass** = the flow's assertions all succeed within timeout, with no crash.
- **Functional score (per build)** = `passed_ACs / 14`.
- Report **per-config across the 3 runs** as `min / median / max` pass-rate (per playbook §5 —
  never a single number). A criterion that passes in 1 of 3 runs is a *flakiness* signal worth
  reporting, not an average to hide.
- Keep the v3 LLM "functional evaluation" only as a **secondary** narrative; this AC pass-rate is
  the primary, objective functional metric.

## Traceability
`AC-ID` → flow file (`flows/AC-<id>.yaml`) → judge rubric line (`eval/rubric.md`). One ID, three
places, same wording. If you add or change a criterion, change all three together.
