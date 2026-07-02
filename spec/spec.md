# Specification — YouTube Dashboard ("ytdash")

> **SHARED INPUT.** Given byte-identical to every build. This is the Spec-Kit `specify`
> artifact: it describes **what** to build and **why**, with **no technology nouns**. Acceptance
> is defined by [`acceptance-criteria.md`](./acceptance-criteria.md); each requirement links to
> its `AC-*` IDs. Build the whole thing; iterations exist only to mirror the talk's history and
> to structure the acceptance criteria.

## Purpose
A small but realistic mobile app that signs a user in, shows a list of YouTube videos fetched
from an API, caches them, lets the user filter/sort them, and plots the ones with a location on
a map. It is intentionally modest in scope but rich in *failure points* (auth, network, parse,
persistence, map), which is the point.

## Users & access
- A user signs in with their Google identity.
- Access is restricted to a **whitelist** of authorized emails. A signed-in user whose email is
  not on the whitelist is denied access and shown an error; they are not taken into the app.
  → `AC-LOGIN-01`, `AC-LOGIN-02`, `AC-LOGIN-03`

## Iteration 1 — Authentication & access control
- A login screen offers Google sign-in.
- On success with an authorized email, the user lands on the main screen.
- On success with a non-authorized email, the user stays out and sees an error.
- The user can sign out and return to the login screen.
  → `AC-LOGIN-01`, `AC-LOGIN-02`, `AC-LOGIN-03`

## Iteration 2 — Video list
- After login, the app aggregates videos from the **configured set of source channels** (authorized
  via an API key) and displays them in a scrollable list. Each row shows at least the video's title,
  a thumbnail, and its description/metadata.
- **Fetch ALL videos, not just the first page.** For every source channel the app must follow the
  API's pagination (page tokens) until exhausted — the list must contain every available video from
  every channel, deduplicated, not only the first page of each.
- **The screen title shows the total number of loaded videos** (e.g. the count across all channels),
  surfaced via the `video_count` identifier so it can be asserted.
- While loading, a loading state is shown; on failure, an error state with retry is shown.
- The user can refresh to re-fetch the latest data.
- Tapping a row opens **that specific video** in the external YouTube experience. If the external
  app cannot be opened, the app surfaces an error (`external_open_error`) — it must not crash or
  silently do nothing.
  → `AC-LIST-01`, `AC-LIST-02`, `AC-LIST-03`, `AC-COUNT-01`, `AC-LINK-01`

## Iteration 3 — Caching, filtering, sorting
- Fetched videos are persisted locally and are the source the list reads from. If the network
  is unavailable, the app shows the most recently cached videos rather than failing.
- The user can **filter** the videos (e.g., by a category/attribute present in the data) and the
  visible set updates accordingly.
- The user can **sort** the videos (e.g., by date and by title) and the order updates accordingly.
  → `AC-CACHE-01`, `AC-FILTER-01`, `AC-SORT-01`

## Iteration 4 — Map
- The user can navigate to a map screen showing an OpenStreetMap map with a marker for each
  video that has an associated location.
- Tapping a marker shows a bottom sheet with that video's details, including its watch URL
  (`detail_video_url`).
- The bottom sheet has an action that opens **the same video the marker is for** in the external
  YouTube experience (the opened URL must equal `detail_video_url`); on failure it surfaces
  `external_open_error`.
  → `AC-MAP-01`, `AC-MAP-02`, `AC-MAP-03`, `AC-LINK-01`

## Cross-cutting requirements
- Every screen has explicit **loading**, **empty**, and **error** states; errors offer retry.
- No silent failures or crashes at any failure point.
- All identifiers and the UI-test-mode behavior from the **constitution** must be honored (this
  is how the app is validated).

## Data (so the app + the acceptance criteria are deterministic)
The app reads its videos from a configurable API base URL (constitution §2, §4). For automated
validation, that URL points at a **mock server** serving a fixed fixture. The fixture and the
acceptance criteria are kept in lockstep. The fixture provides, at minimum:

- **8 videos**, each with: `id`, `title`, `description`, `publishedAt` (date), `category`
  (one of `music`, `news`, `tech`), `thumbnailUrl`, and an optional `location` (`lat`,`lng`).
- **5 of the 8** videos have a `location` (so the map shows exactly 5 markers).
- A known **newest** video titled `"ZZZ Newest Clip"` (latest `publishedAt`) and a known
  **oldest** titled `"AAA Oldest Clip"`, so sort assertions are unambiguous.
- Exactly **3** videos in category `tech`, so filter assertions are unambiguous.
- The first video's external link resolves to `https://www.youtube.com/watch?v=VIDEO_ID_1`.
- The **total loaded count is 8**; after load the title's `video_count` must show `8`. The mock
  serves each channel's videos across **multiple pages** (small page size), so a build that fails to
  follow pagination will load fewer than 8 and miss `video_count = 8` (and the newest/oldest sort
  items, which may sit on later pages). This is how "fetch all pages" is enforced. → `AC-COUNT-01`

> A reference fixture (`fixtures/videos.json`) and a one-command mock server are out of scope for
> the *builder* (they must not see the harness), but you, the experimenter, run them. See
> `flows/README.md` for the exact values the flows assert against. Keep the fixture identical
> across all builds — it is part of the shared, controlled environment.
>
> **Scoring is on a HIDDEN held-out dataset** with different ids, titles, counts, and locations than
> anything you can see. **Never hardcode fixture values or shortcut the data flow** (e.g. assuming a
> fixed list length, a known first id, or a single page/channel) — read everything from the API
> responses, follow pagination, and aggregate all configured channels. Building to the visible
> values will pass your local checks but fail scoring.

## Out of scope
Visual design, animations, theming, accessibility beyond the required identifiers, localization,
and any platform other than Android. The app must *work*, not be beautiful (per the talk's
long-standing "definition of done").
