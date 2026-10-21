"""Goldenes 3D-Plus als transparentes PNG."""
from pathlib import Path
from PIL import Image
from playwright.sync_api import sync_playwright

OUT = Path(__file__).parent / "out"
S = 240
c, a, L, depth = 116, 27, 92, 16  # Mitte, halbe Balkenbreite, Armlänge, Tiefe


def plus(dx=0, dy=0, shrink=0):
    aa, LL = a - shrink, L - shrink
    p = [(c - aa, c - LL), (c + aa, c - LL), (c + aa, c - aa), (c + LL, c - aa), (c + LL, c + aa), (c + aa, c + aa),
         (c + aa, c + LL), (c - aa, c + LL), (c - aa, c + aa), (c - LL, c + aa), (c - LL, c - aa), (c - aa, c - aa)]
    return " ".join(f"{x + dx:.1f},{y + dy:.1f}" for x, y in p)


extrude = "".join(
    f'<polygon points="{plus(i * 0.55, i * 0.9)}" fill="url(#side)" stroke="url(#side)" stroke-width="18" stroke-linejoin="round"/>'
    for i in range(depth, 0, -1))
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{S}" height="{S}" viewBox="0 0 {S} {S}">
<defs>
  <linearGradient id="face" x1="0.15" y1="0" x2="0.85" y2="1">
    <stop offset="0" stop-color="#FFF7C9"/>
    <stop offset="0.28" stop-color="#FFE071"/>
    <stop offset="0.55" stop-color="#F5B82E"/>
    <stop offset="0.80" stop-color="#E39A12"/>
    <stop offset="1" stop-color="#C47A06"/>
  </linearGradient>
  <linearGradient id="side" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#B9770A"/>
    <stop offset="0.5" stop-color="#8E5604"/>
    <stop offset="1" stop-color="#5E3600"/>
  </linearGradient>
  <linearGradient id="bevel" x1="0.1" y1="0.05" x2="0.9" y2="0.95">
    <stop offset="0" stop-color="#FFFDF0"/>
    <stop offset="0.45" stop-color="#FFD54F"/>
    <stop offset="0.6" stop-color="#D99212"/>
    <stop offset="1" stop-color="#8A5200"/>
  </linearGradient>
  <linearGradient id="rim" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#FFFBE6"/>
    <stop offset="0.5" stop-color="#FFD24D" stop-opacity="0.5"/>
    <stop offset="1" stop-color="#A86500"/>
  </linearGradient>
  <radialGradient id="shine" cx="0.36" cy="0.30" r="0.36">
    <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.85"/>
    <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
  </radialGradient>
  <mask id="faceMask"><polygon points="{plus(0, 0, 7)}" fill="#fff" stroke="#fff" stroke-width="18" stroke-linejoin="round"/></mask>
  <clipPath id="faceClip"><polygon points="{plus()}" stroke-width="18" stroke-linejoin="round"/></clipPath>
  <filter id="drop" x="-20%" y="-20%" width="150%" height="150%">
    <feDropShadow dx="4" dy="9" stdDeviation="7" flood-color="#2b1500" flood-opacity="0.55"/>
  </filter>
</defs>
<g filter="url(#drop)">
  {extrude}
  <!-- Vorderseite mit Fase: außen kräftiges Licht/Schatten, innen glatte Fläche -->
  <polygon points="{plus()}" fill="url(#bevel)" stroke="url(#bevel)" stroke-width="18" stroke-linejoin="round"/>
  <polygon points="{plus(0, 0, 7)}" fill="url(#face)" stroke="url(#face)" stroke-width="18" stroke-linejoin="round"/>
  <!-- Glanz -->
  <rect width="{S}" height="{S}" fill="url(#shine)" mask="url(#faceMask)"/>
  <!-- Funkeln -->
  <g transform="translate({c + a + 4} {c - L + 2})" fill="#FFFFFF">
    <path d="M0 -14 L3 -3 L14 0 L3 3 L0 14 L-3 3 L-14 0 L-3 -3 Z" opacity="0.95"/>
  </g>
</g>
</svg>'''
(OUT / "plus.svg").write_text(svg)
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": S, "height": S}, device_scale_factor=2)
    pg.goto((OUT / "plus.svg").as_uri())
    pg.screenshot(path=str(OUT / "plus.png"), omit_background=True)
    b.close()
im = Image.open(OUT / "plus.png").convert("RGBA")
bbox = im.split()[3].point(lambda v: 255 if v > 6 else 0).getbbox()
im = im.crop((max(0, bbox[0] - 6), max(0, bbox[1] - 6), min(im.width, bbox[2] + 6), min(im.height, bbox[3] + 6)))
im.save(OUT / "plus-trim.png")
bg = Image.new("RGBA", (im.width + 80, im.height + 80), (60, 40, 70, 255)); bg.alpha_composite(im, (40, 40)); bg.save(OUT / "plus-preview.png")
print(im.size)
