"""Sucht freie Fotos (gemeinfrei / CC) für jedes Interesse bei Wikimedia Commons.

Läuft im GitHub-Workflow "Sticker-Fotos". Ergebnis: out/candidates/<key>/<n>.jpg
(kleine Vorschauen) und out/candidates.json mit Lizenz und Urheber pro Bild.
"""
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://commons.wikimedia.org/w/api.php"
UA = "kumi-sticker-search/1.0 (https://github.com/tjark0867-cmyk/turp; interest stickers)"
PER_KEY = 8
THUMB = 320

HERE = Path(__file__).parent
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "out")
ONLY = set(sys.argv[2].split(",")) if len(sys.argv) > 2 and sys.argv[2] else None


def get(url, params=None, tries=4):
    if params:
        url = url + "?" + urllib.parse.urlencode(params)
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=30) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001
            if i == tries - 1:
                raise
            time.sleep(2 + i * 3)
    return b""


def clean_html(s):
    s = re.sub(r"<[^>]+>", "", s or "")
    return re.sub(r"\s+", " ", s).strip()


def license_ok(short):
    s = (short or "").strip().lower()
    if not s:
        return False
    if "nc" in s.split() or "-nc" in s or "nd" in s.split() or "-nd" in s:
        return False
    return s.startswith("cc0") or s.startswith("public domain") or s.startswith("pd") or s.startswith("cc by")


def search(query):
    data = json.loads(get(API, {
        "action": "query",
        "format": "json",
        "generator": "search",
        "gsrsearch": f"{query} filetype:bitmap",
        "gsrnamespace": 6,
        "gsrlimit": 25,
        "prop": "imageinfo",
        "iiprop": "url|size|mime|extmetadata",
        "iiurlwidth": THUMB,
    }))
    pages = sorted((data.get("query") or {}).get("pages", {}).values(), key=lambda p: p.get("index", 0))
    found = []
    for p in pages:
        ii = (p.get("imageinfo") or [{}])[0]
        meta = ii.get("extmetadata") or {}
        lic = (meta.get("LicenseShortName") or {}).get("value", "")
        if not license_ok(lic):
            continue
        if ii.get("mime") not in ("image/jpeg", "image/png"):
            continue
        if (ii.get("width") or 0) < 500 or (ii.get("height") or 0) < 400:
            continue
        found.append({
            "title": p.get("title", ""),
            "thumb": ii.get("thumburl", ""),
            "url": ii.get("url", ""),
            "width": ii.get("width"),
            "height": ii.get("height"),
            "license": lic,
            "artist": clean_html((meta.get("Artist") or {}).get("value", ""))[:200],
            "page": ii.get("descriptionurl", ""),
        })
    return found


def main():
    queries = json.loads((HERE / "queries.json").read_text())
    result = {}
    for key, qs in queries.items():
        if ONLY and key not in ONLY:
            continue
        seen, cands = set(), []
        for q in qs:
            try:
                for c in search(q):
                    if c["title"] in seen:
                        continue
                    seen.add(c["title"])
                    cands.append(c)
                    if len(cands) >= PER_KEY:
                        break
            except Exception as e:  # noqa: BLE001
                print(f"{key}: Suche '{q}' fehlgeschlagen: {e}")
            if len(cands) >= PER_KEY:
                break
            time.sleep(0.5)
        folder = OUT / "candidates" / key
        folder.mkdir(parents=True, exist_ok=True)
        kept = []
        for c in cands:
            try:
                (folder / f"{len(kept)}.jpg").write_bytes(get(c["thumb"]))
                kept.append(c)
            except Exception as e:  # noqa: BLE001
                print(f"{key}: Vorschau fehlgeschlagen: {e}")
            time.sleep(0.2)
        result[key] = kept
        print(f"{key}: {len(kept)}")
    (OUT / "candidates.json").write_text(json.dumps(result, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
