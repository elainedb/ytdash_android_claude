# YouTube Data API v3 — the signatures the mock must mirror

The app fetches videos from a fixed set of **source channels**, shows them in a list/cache, and puts
the geolocated ones on a map. To make the **mock ↔ real swap a runtime change (no rebuild)**, the
mock serves the *same endpoints and JSON shapes* as the real YouTube Data API v3. Both `apiBaseUrl`
and `apiKey` arrive as UI-test-mode launch extras (constitution §4):
- `apiBaseUrl` = **host root** — `http://10.0.2.2:<port>` (mock) or `https://www.googleapis.com`
  (real). The app appends `/youtube/v3/<endpoint>` itself; do NOT include `/youtube/v3` in the base.
- `apiKey` = read at runtime; the same build talks to mock or real by swapping these two extras.

**There is NO catch-all / "all channels" query** — not here, not in the real API. The app MUST
iterate the configured source channels (`config/channels.json`) and merge/dedupe by videoId. The
mock enforces this: an empty/unknown `channelId` returns `[]`. (A v4 dry-run caught an agent build
overfitting to a mock-only `channelId=ALL` shortcut that broke against real YouTube — hence this
rule.)

## The 3 endpoints the app uses

### 1. Channel uploads — list a channel's recent videos
Two equivalent idioms; pick one in your plan. The mock serves both.

**search.list** (simplest):
```
GET /youtube/v3/search
    ?key={API_KEY}
    &channelId={CHANNEL_ID}
    &part=snippet
    &order=date
    &type=video
    &maxResults=50
```
Response:
```json
{
  "kind": "youtube#searchListResponse",
  "nextPageToken": "CAUQAA",
  "items": [
    {
      "kind": "youtube#searchResult",
      "id": { "kind": "youtube#video", "videoId": "VIDEO_ID_1" },
      "snippet": {
        "publishedAt": "2026-03-01T10:00:00Z",
        "channelId": "UC_x5XG1OV2P6uZZ5FSM9Ttw",
        "title": "Tech Talk One",
        "description": "A talk about tech.",
        "channelTitle": "Tech Channel",
        "thumbnails": {
          "default": { "url": "https://i.ytimg.com/vi/VIDEO_ID_1/default.jpg", "width": 120, "height": 90 },
          "medium":  { "url": "https://i.ytimg.com/vi/VIDEO_ID_1/mqdefault.jpg", "width": 320, "height": 180 },
          "high":    { "url": "https://i.ytimg.com/vi/VIDEO_ID_1/hqdefault.jpg", "width": 480, "height": 360 }
        }
      }
    }
  ]
}
```
> `search.list` does NOT return location or duration. For those you must call `videos.list` (below).
> `search.list` also costs 100 quota units/call — a real reason builds cache aggressively.

**playlistItems.list** (cheaper, 1 unit): first `channels.list?part=contentDetails&id=…` →
`contentDetails.relatedPlaylists.uploads`, then
`GET /youtube/v3/playlistItems?part=snippet&playlistId={uploadsId}&maxResults=50`. Same `snippet`
shape; `snippet.resourceId.videoId` holds the id.

### 2. Video details + location — videos.list
```
GET /youtube/v3/videos
    ?key={API_KEY}
    &id={ID1,ID2,ID3}          # comma-joined, up to 50
    &part=snippet,contentDetails,recordingDetails
```
Response (note `id` is a STRING here, and **location lives in `recordingDetails`**):
```json
{
  "kind": "youtube#videoListResponse",
  "items": [
    {
      "kind": "youtube#video",
      "id": "VIDEO_ID_1",
      "snippet": { "publishedAt": "2026-03-01T10:00:00Z", "title": "Tech Talk One",
                   "description": "A talk about tech.", "channelTitle": "Tech Channel",
                   "categoryId": "28", "thumbnails": { "...": {} } },
      "contentDetails": { "duration": "PT12M30S" },
      "recordingDetails": { "location": { "latitude": 48.8566, "longitude": 2.3522 } }
    }
  ]
}
```
> Only videos whose owner set a recording location have `recordingDetails.location`. In the fixture,
> 5 of 8 do (the map markers); 3 don't. `videos.list` is 1 quota unit/call.

### 3. channels.list — (only if using the playlistItems idiom)
```
GET /youtube/v3/channels?key={API_KEY}&id={CHANNEL_ID}&part=contentDetails
→ items[0].contentDetails.relatedPlaylists.uploads   # the uploads playlist id
```

## Mapping real → the app's domain model (and to the deterministic fixture)

| App field        | Real source                                   | Fixture field |
|------------------|-----------------------------------------------|---------------|
| `id`             | `search:id.videoId` / `videos:id`             | `id`          |
| `title`          | `snippet.title`                               | `title`       |
| `description`    | `snippet.description`                          | `description` |
| `publishedAt`    | `snippet.publishedAt` (ISO-8601, sort key)     | `publishedAt` |
| `category`/label | the **source channel's label** (config), not categoryId | `category` |
| `lat`,`lng`      | `recordingDetails.location.{latitude,longitude}` | `location.{lat,lng}` |
| `thumbnailUrl`   | `snippet.thumbnails.medium.url`                | `thumbnailUrl`|
| `youtubeUrl`     | `https://www.youtube.com/watch?v={id}`         | derived       |

**"Category" = source-channel label.** AC-FILTER-01 filters by the human label (`tech`/`music`/
`news`) attached to each *source channel* in `config.yaml`, not by YouTube's numeric `categoryId`.
This is how v3 modeled it and keeps filter meaningful with real data.

**Reverse geocoding (place names on the map)** is OSM/Nominatim, not YouTube:
`GET https://nominatim.openstreetmap.org/reverse?lat=..&lon=..&format=json` with a proper
`User-Agent`. Cache by 3-decimal coords; rate-limit. (Constitution/cross-framework-setup spec this.)

## Why the deterministic fixture matters
The scored 12-AC suite asserts fixture facts ("ZZZ Newest Clip" newest, `VIDEO_ID_1` first, exactly
3 `tech`, 5 located). Real YouTube data changes, so **the functional score is computed against the
mock**; the real-API step is a smoke check (list populates, map has markers, deep-link opens). The
mock (`youtube-mock-server.py`) returns the fixture in the exact shapes above so the *same parsing
code* handles both.
