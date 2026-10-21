"""Wisch-Zeichen aus Pappe: rotes X (weiter) und türkiser Haken (Anfrage) – transparent, ausgeschnitten."""
import re
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

OUT = Path(__file__).parent / "out"
OUT.mkdir(exist_ok=True)
DEFS = re.search(r"<defs>(.*?)</defs>", (Path(__file__).parent / "mountain.py").read_text(), re.S).group(1)
S = 420


def painted(shape, color, edge="#D9AE78", s=0.94):
    c = S / 2
    return f'''<g fill="{edge}">{shape}</g>
    <g fill="{color}" filter="url(#paint)" transform="translate({c} {c}) scale({s}) translate({-c} {-c})">{shape}</g>
    <g fill="url(#corrLight)">{shape}</g>
    <g fill="#FFFFFF" opacity="0.10" transform="translate({c} {c}) scale({s * 0.98}) translate({-c} {-c - 6})">{shape}</g>'''


# X aus zwei abgerundeten Balken
bar = 'x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}"'
X = (f'<rect x="{S/2-46}" y="40" width="92" height="{S-80}" rx="40" transform="rotate(45 {S/2} {S/2})"/>'
     f'<rect x="{S/2-46}" y="40" width="92" height="{S-80}" rx="40" transform="rotate(-45 {S/2} {S/2})"/>')
# Haken als dicker Pfad mit runden Enden
CHECK = (f'<path d="M 78 222 L 170 314 L 344 108" fill="none" stroke="currentColor" stroke-width="88" '
         f'stroke-linecap="round" stroke-linejoin="round"/>')


CHECK_SHAPE = '''<rect x="20.2" y="213.0" width="209.6" height="88" rx="40.0" transform="rotate(45.0 125.0 257.0)"/><rect x="80.7" y="159.0" width="348.6" height="88" rx="40.0" transform="rotate(-48.1 255.0 203.0)"/>'''


def check_shape(color):
    return CHECK_SHAPE


def svg(body):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{S}" height="{S}" viewBox="0 0 {S} {S}"><defs>{DEFS}</defs>{body}</svg>'''


def mark_x():
    return svg(f'<g filter="url(#cut)">{painted(X, "#FF4D6D")}</g>')


def mark_check():
    return svg(f'<g filter="url(#cut)" transform="translate(0 10)">{painted(CHECK_SHAPE, "#2DE1C2")}</g>')


with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": S, "height": S})
    for name, fn in (("swipe_no", mark_x), ("swipe_yes", mark_check)):
        f = OUT / f"{name}.svg"
        f.write_text(fn())
        pg.goto(f.as_uri())
        pg.screenshot(path=str(OUT / f"{name}.png"), omit_background=True)
        Image.open(OUT / f"{name}.png").convert("RGBA").save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)
    b.close()
prev = Image.new("RGBA", (S * 2 + 30, S + 20), (60, 52, 46, 255))
prev.alpha_composite(Image.open(OUT / "swipe_no.png"), (10, 10))
prev.alpha_composite(Image.open(OUT / "swipe_yes.png"), (S + 20, 10))
prev.save(OUT / "swipe_preview.png")
print("ok")
