"""Kleiner Pappstreifen (Kraftpapier, ausgerissene Kanten) als Hintergrund für Sticker-Beschriftungen."""
import re
import random
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

OUT = Path(__file__).parent / "out"
DEFS = re.search(r"<defs>(.*?)</defs>", (Path(__file__).parent / "mountain.py").read_text(), re.S).group(1)
W, H = 480, 130
rnd = random.Random(7)


def torn(x0, y0, x1, y1, amp=4, step=14):
    pts = []
    x = x0
    while x < x1:
        pts.append((x, y0 + rnd.uniform(-amp, amp)))
        x += step
    pts.append((x1, y0))
    y = y0
    while y < y1:
        pts.append((x1 + rnd.uniform(-amp, amp), y))
        y += step
    pts.append((x1, y1))
    x = x1
    while x > x0:
        pts.append((x, y1 + rnd.uniform(-amp, amp)))
        x -= step
    pts.append((x0, y1))
    y = y1
    while y > y0:
        pts.append((x0 + rnd.uniform(-amp, amp), y))
        y -= step
    return " ".join(f"{a:.1f},{b:.1f}" for a, b in pts)


shape = f'<polygon points="{torn(18, 22, W - 18, H - 26)}"/>'
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs>{DEFS}</defs>
<g filter="url(#cutSoft)">
  <g fill="#D3A06A">{shape}</g>
  <g fill="url(#corr)">{shape}</g>
  <clipPath id="c">{shape}</clipPath>
  <g clip-path="url(#c)">
    <rect width="{W}" height="{H}" filter="url(#blotch)" opacity="0.45"/>
    <rect width="{W}" height="{H}" filter="url(#fibers)" opacity="0.5"/>
  </g>
</g></svg>'''
(OUT / "label_strip.svg").write_text(svg)
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": W, "height": H})
    pg.goto((OUT / "label_strip.svg").as_uri())
    pg.screenshot(path=str(OUT / "label_strip.png"), omit_background=True)
    b.close()
im = Image.open(OUT / "label_strip.png").convert("RGBA")
im = im.crop(im.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox())
im.save(OUT / "label_strip.webp", "WEBP", quality=90, method=6)
print(im.size)
