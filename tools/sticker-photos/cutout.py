"""Lädt die ausgewählten Fotos in guter Größe und stellt das Motiv frei (rembg).

picks.json: { key: { title, thumb, url, width, license, artist, page }, ... }
Ergebnis: out/cutouts/<key>.png (transparent, zugeschnitten) und out/credits.json
"""
import io
import json
import sys
import time
import urllib.request
from pathlib import Path

from PIL import Image
from rembg import new_session, remove

UA = "kumi-sticker-search/1.0 (https://github.com/tjark0867-cmyk/turp; interest stickers)"
HERE = Path(__file__).parent
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "out")
MODEL = sys.argv[2] if len(sys.argv) > 2 and sys.argv[2] else "isnet-general-use"
ONLY = set(sys.argv[3].split(",")) if len(sys.argv) > 3 and sys.argv[3] else None
SIZE = 1000


def get(url, tries=4):
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.read()
        except Exception:  # noqa: BLE001
            if i == tries - 1:
                raise
            time.sleep(3 + i * 3)
    return b""


def big_url(p):
    thumb = p.get("thumb", "")
    if (p.get("width") or 0) > SIZE and "/320px-" in thumb:
        return thumb.replace("/320px-", f"/{SIZE}px-")
    return p.get("url") or thumb


def main():
    picks = json.loads((HERE / "picks.json").read_text())
    session = new_session(MODEL)
    (OUT / "cutouts").mkdir(parents=True, exist_ok=True)
    credits = {}
    for key, p in picks.items():
        if ONLY and key not in ONLY:
            continue
        try:
            img = Image.open(io.BytesIO(get(big_url(p)))).convert("RGB")
            img.thumbnail((SIZE, SIZE))
            cut = remove(img, session=session, post_process_mask=True)
            box = cut.getchannel("A").point(lambda a: 255 if a > 20 else 0).getbbox()
            if box:
                cut = cut.crop(box)
            cut.save(OUT / "cutouts" / f"{key}.png")
            credits[key] = {k: p.get(k, "") for k in ("title", "license", "artist", "page")}
            print(f"{key}: ok {cut.size}")
        except Exception as e:  # noqa: BLE001
            print(f"{key}: Fehler {e}")
        time.sleep(0.3)
    (OUT / "credits.json").write_text(json.dumps(credits, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
