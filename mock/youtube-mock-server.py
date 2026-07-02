#!/usr/bin/env python3
"""
YouTube-Data-API-v3-shaped mock for the v4 experiment (stdlib only).

Serves the deterministic 8-video fixture in the SAME shapes as the real API (see youtube-api.md),
so the only mock↔real difference is the base URL + key. Endpoints:

  GET /youtube/v3/search?channelId=..&part=snippet&type=video&maxResults=50
      -> searchListResponse; items have id.videoId + snippet. Filtered to the channel's videos
         (a video belongs to a channel whose label == the video's fixture `category`).
         There is NO catch-all: an empty/unknown channelId returns []. The app must iterate the
         configured source channels and merge — exactly as the real API requires.
  GET /youtube/v3/videos?id=ID1,ID2&part=snippet,contentDetails,recordingDetails
      -> videoListResponse; id is a string; recordingDetails.location for located videos.
  GET /youtube/v3/channels?id=..&part=contentDetails
      -> contentDetails.relatedPlaylists.uploads = "UP_<channelId>"
  GET /youtube/v3/playlistItems?playlistId=UP_<channelId>&part=snippet
      -> playlistItemListResponse (same set as search for that channel)
  GET /health            -> ok
  GET /thumb             -> 1x1 PNG  (legacy; thumbnails point at i.ytimg.com by default)
  GET /videos            -> the simplified {items:[...]} used by the slice builds (back-compat)

Channels: pass --channels "id:label,id:label,..." (the experiment passes config.yaml's channels).
Default = one synthetic channel per distinct fixture category.

Run:
  python3 youtube-mock-server.py --port 8090
  python3 youtube-mock-server.py --port 8090 --channels "UC_x5:tech,UCBR8:music,UCYfd:news"
Reach from the Android emulator at http://10.0.2.2:<port> .
"""
import argparse, base64, json, os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

HERE = os.path.dirname(os.path.abspath(__file__))
# videos.json is co-located in a run workspace; fall back to the scaffold fixtures/ for standalone.
FIXTURE = next(
    p for p in (
        os.path.join(HERE, "videos.json"),
        os.path.join(HERE, "..", "fixtures", "videos.json"),
    ) if os.path.exists(p)
)
PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
)


def load_fixture(path=None):
    with open(path or FIXTURE, encoding="utf-8") as f:
        return json.load(f)["items"]


def thumbs(vid):
    base = f"https://i.ytimg.com/vi/{vid}"
    return {
        "default": {"url": f"{base}/default.jpg", "width": 120, "height": 90},
        "medium": {"url": f"{base}/mqdefault.jpg", "width": 320, "height": 180},
        "high": {"url": f"{base}/hqdefault.jpg", "width": 480, "height": 360},
    }


def snippet(v, channel_id="MOCKCH", channel_title=None):
    return {
        "publishedAt": v.get("publishedAt", ""),
        "channelId": channel_id,
        "title": v.get("title", ""),
        "description": v.get("description", ""),
        # channelTitle = the configured label, so the app derives the category from it. The app sees
        # the config label (e.g. "cronicas"), NOT the fixture's internal bucket name (e.g. "tech").
        "channelTitle": channel_title if channel_title is not None else f"{v.get('category','')} channel".strip(),
        "thumbnails": thumbs(v["id"]),
    }


class Mock:
    def __init__(self, channels, page_size=2, fixture=None):
        self.items = load_fixture(fixture)
        self.page_size = max(1, page_size)  # small → forces apps to follow nextPageToken
        # The fixture's deterministic test buckets, in first-appearance order.
        cats = list(dict.fromkeys(v.get("category", "") for v in self.items))
        if not channels:
            channels = [(f"MOCKCH_{c}", c) for c in cats]
        self.channels = channels
        self.label_of = {cid: lbl for cid, lbl in channels}
        # Map each CONFIGURED channel to a fixture bucket BY ORDER, so ANY real channel set (from
        # config.yaml) works against the fixed test data. channel[i] ← cats[i]; extra channels get
        # nothing. This decouples the test data from the user's real channel labels.
        self.cat_of = {cid: (cats[i] if i < len(cats) else None) for i, (cid, _l) in enumerate(channels)}

    def videos_for_channel(self, channel_id):
        # NO catch-all: an empty/unknown channelId returns nothing. Real YouTube has no
        # cross-channel 'ALL' query, so the app MUST iterate the configured source channels and
        # merge. Refusing a catch-all here stops agents overfitting to a mock-only shortcut.
        cat = self.cat_of.get(channel_id)
        if cat is None:
            return []
        return [v for v in self.items if v.get("category") == cat]

    def _page(self, vids, page_token):
        try:
            offset = int(page_token) if page_token else 0
        except ValueError:
            offset = 0
        page = vids[offset:offset + self.page_size]
        nxt = offset + self.page_size
        return page, (str(nxt) if nxt < len(vids) else None)

    def search_response(self, channel_id, page_token=""):
        vids = self.videos_for_channel(channel_id)
        label = self.label_of.get(channel_id)
        page, nxt = self._page(vids, page_token)
        resp = {
            "kind": "youtube#searchListResponse",
            "pageInfo": {"totalResults": len(vids), "resultsPerPage": self.page_size},
            "items": [
                {
                    "kind": "youtube#searchResult",
                    "id": {"kind": "youtube#video", "videoId": v["id"]},
                    "snippet": snippet(v, channel_id, label),
                }
                for v in page
            ],
        }
        if nxt is not None:
            resp["nextPageToken"] = nxt
        return resp

    def playlist_response(self, playlist_id, page_token=""):
        channel_id = playlist_id[3:] if playlist_id.startswith("UP_") else playlist_id
        vids = self.videos_for_channel(channel_id)
        label = self.label_of.get(channel_id)
        page, nxt = self._page(vids, page_token)
        resp = {
            "kind": "youtube#playlistItemListResponse",
            "items": [
                {
                    "kind": "youtube#playlistItem",
                    "snippet": {**snippet(v, channel_id, label), "resourceId": {"kind": "youtube#video", "videoId": v["id"]}},
                }
                for v in page
            ],
        }
        if nxt is not None:
            resp["nextPageToken"] = nxt
        return resp

    def videos_response(self, ids):
        idset = set(ids)
        out = []
        for v in self.items:
            if v["id"] not in idset:
                continue
            item = {
                "kind": "youtube#video",
                "id": v["id"],
                "snippet": {**snippet(v), "categoryId": "28"},
                "contentDetails": {"duration": "PT10M00S"},
            }
            loc = v.get("location")
            if loc:
                item["recordingDetails"] = {"location": {"latitude": loc["lat"], "longitude": loc["lng"]}}
            out.append(item)
        return {"kind": "youtube#videoListResponse", "items": out}

    def channels_response(self, ids):
        return {
            "kind": "youtube#channelListResponse",
            "items": [
                {"kind": "youtube#channel", "id": cid,
                 "contentDetails": {"relatedPlaylists": {"uploads": f"UP_{cid}"}}}
                for cid in ids
            ],
        }

    def simple_videos(self, base_url):
        items = json.loads(json.dumps(self.items))
        for it in items:
            if isinstance(it.get("thumbnailUrl"), str):
                it["thumbnailUrl"] = it["thumbnailUrl"].replace("{BASE}", base_url)
        return {"items": items}


def make_handler(mock):
    class H(BaseHTTPRequestHandler):
        def _send(self, code, body, ctype="application/json"):
            self.send_response(code)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def _json(self, obj, code=200):
            self._send(code, json.dumps(obj).encode("utf-8"))

        def do_GET(self):
            u = urlparse(self.path)
            q = parse_qs(u.query)
            path = u.path
            host = self.headers.get("Host", "localhost")
            if path == "/youtube/v3/search":
                self._json(mock.search_response((q.get("channelId") or [""])[0], (q.get("pageToken") or [""])[0]))
            elif path == "/youtube/v3/videos":
                ids = (q.get("id") or [""])[0].split(",") if q.get("id") else [v["id"] for v in mock.items]
                self._json(mock.videos_response([i for i in ids if i]))
            elif path == "/youtube/v3/channels":
                self._json(mock.channels_response((q.get("id") or [""])[0].split(",")))
            elif path == "/youtube/v3/playlistItems":
                self._json(mock.playlist_response((q.get("playlistId") or [""])[0], (q.get("pageToken") or [""])[0]))
            elif path == "/videos":
                self._json(mock.simple_videos(f"http://{host}"))
            elif path == "/thumb":
                self._send(200, PNG_1X1, "image/png")
            elif path == "/health":
                self._send(200, b"ok", "text/plain")
            else:
                self._json({"error": {"code": 404, "message": "not found"}}, 404)

        def log_message(self, *a):  # quiet
            pass

    return H


def parse_channels(s):
    if not s:
        return None
    out = []
    for pair in s.split(","):
        if ":" in pair:
            cid, lbl = pair.split(":", 1)
            out.append((cid.strip(), lbl.strip()))
    return out or None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8090)
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("--channels", default="", help='"id:label,id:label"')
    ap.add_argument("--page-size", type=int, default=2, help="items per search/playlist page (small forces pagination)")
    ap.add_argument("--fixture", default="", help="path to a fixture json (default: videos.json / ../fixtures)")
    args = ap.parse_args()
    mock = Mock(parse_channels(args.channels), page_size=args.page_size, fixture=(args.fixture or None))
    srv = ThreadingHTTPServer((args.host, args.port), make_handler(mock))
    print(f"YouTube-shaped mock on http://{args.host}:{args.port}  (emulator: http://10.0.2.2:{args.port})")
    print(f"channels: {mock.channels}  page_size={mock.page_size}")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        srv.shutdown()


if __name__ == "__main__":
    main()
