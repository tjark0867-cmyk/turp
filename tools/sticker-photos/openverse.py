"""Sucht freie Fotos über Openverse (Creative Commons) – ohne Wikimedia.

Quellen u. a. StockSnap, Rawpixel (CC0-Stockfotos), Flickr. Nur Lizenzen, die
kommerzielle Nutzung und Bearbeitung erlauben (CC0, Public Domain, CC BY, CC BY-SA).
Ergebnis wie search.py: out/candidates/<key>/<n>.jpg + out/candidates.json
"""
import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://api.openverse.org/v1/images/"
UA = "kumi-sticker-search/1.0 (https://github.com/tjark0867-cmyk/turp; interest stickers)"
PER_KEY = 12
HERE = Path(__file__).parent
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "out")
ONLY = set(sys.argv[2].split(",")) if len(sys.argv) > 2 and sys.argv[2] else None


def get(url, tries=4):
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=40) as r:
                return r.read()
        except Exception:  # noqa: BLE001
            if i == tries - 1:
                raise
            time.sleep(3 + i * 4)
    return b""


def search(q):
    params = {
        "q": q,
        "license": "cc0,pdm,by,by-sa",
        "license_type": "commercial,modification",
        "excluded_source": "wikimedia,wikimedia_audio",
        "mature": "false",
        "page_size": 20,
    }
    data = json.loads(get(API + "?" + urllib.parse.urlencode(params)))
    out = []
    for r in data.get("results", []):
        if (r.get("width") or 0) and (r.get("width") or 0) < 600:
            continue
        lic = (r.get("license") or "").lower()
        ver = r.get("license_version") or ""
        out.append({
            "title": r.get("title") or r.get("id"),
            "thumb": r.get("thumbnail") or r.get("url"),
            "url": r.get("url"),
            "width": r.get("width"),
            "height": r.get("height"),
            "license": ("CC0" if lic == "cc0" else "Public domain" if lic == "pdm" else f"CC {lic.upper()} {ver}").strip(),
            "artist": (r.get("creator") or "")[:200],
            "page": r.get("foreign_landing_url") or "",
            "source": r.get("source") or r.get("provider") or "",
            "id": r.get("id"),
        })
    return out


def main():
    queries = json.loads((HERE / "ov_queries.json").read_text())
    result = {}
    for key, qs in queries.items():
        if ONLY and key not in ONLY:
            continue
        seen, cands = set(), []
        for q in qs:
            try:
                for c in search(q):
                    if c["id"] in seen:
                        continue
                    seen.add(c["id"])
                    cands.append(c)
                    if len(cands) >= PER_KEY:
                        break
            except Exception as e:  # noqa: BLE001
                print(f"{key}: Suche '{q}' fehlgeschlagen: {e}")
            if len(cands) >= PER_KEY:
                break
            time.sleep(1.2)
        folder = OUT / "candidates" / key
        folder.mkdir(parents=True, exist_ok=True)
        kept = []
        for c in cands:
            try:
                (folder / f"{len(kept)}.jpg").write_bytes(get(c["thumb"]))
                kept.append(c)
            except Exception as e:  # noqa: BLE001
                print(f"{key}: Vorschau fehlgeschlagen: {e}")
            time.sleep(0.3)
        result[key] = kept
        print(f"{key}: {len(kept)}")
    (OUT / "candidates.json").write_text(json.dumps(result, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
