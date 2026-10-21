"""Zwei Pappkarten mit Piktogramm-Figuren (blau / pink) im Stil der kumi+-Pappe."""
import re
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

W, H = 600, 700
OUT = Path(__file__).parent / "out"
OUT.mkdir(exist_ok=True)
src = (Path(__file__).parent / "mountain.py").read_text()
DEFS = re.search(r"<defs>(.*?)</defs>", src, re.S).group(1)


def male():
    return """
    <circle cx="300" cy="175" r="64"/>
    <rect x="222" y="256" width="156" height="226" rx="34"/>
    <rect x="176" y="262" width="40" height="190" rx="20"/>
    <rect x="384" y="262" width="40" height="190" rx="20"/>
    <rect x="232" y="452" width="62" height="200" rx="26"/>
    <rect x="306" y="452" width="62" height="200" rx="26"/>
    """


def female():
    return """
    <circle cx="300" cy="175" r="64"/>
    <path d="M 250 256 L 350 256 Q 372 256 378 278 L 432 492 Q 436 508 420 508 L 180 508 Q 164 508 168 492 L 222 278 Q 228 256 250 256 Z"/>
    <rect x="166" y="262" width="40" height="176" rx="20" transform="rotate(14 186 262)"/>
    <rect x="394" y="262" width="40" height="176" rx="20" transform="rotate(-14 414 262)"/>
    <rect x="244" y="490" width="50" height="164" rx="22"/>
    <rect x="306" y="490" width="50" height="164" rx="22"/>
    """


def card(fig, color, edge):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>{DEFS}
  <clipPath id="round"><rect width="{W}" height="{H}" rx="44"/></clipPath>
</defs>
<g clip-path="url(#round)">
  <rect width="{W}" height="{H}" fill="#C78A4D"/>
  <rect width="{W}" height="{H}" fill="url(#corr)"/>
  <rect width="{W}" height="{H}" filter="url(#blotch)" opacity="0.5"/>
  <rect width="{W}" height="{H}" filter="url(#fibers)" opacity="0.55"/>
  <g filter="url(#cut)">
    <g fill="{edge}">{fig}</g>
    <g fill="{color}" filter="url(#paint)" transform="translate(300 400) scale(0.955) translate(-300 -400)">{fig}</g>
    <g fill="url(#corrLight)">{fig}</g>
  </g>
  <rect width="{W}" height="{H}" fill="url(#vignette)"/>
</g>
</svg>'''


with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": W, "height": H}, device_scale_factor=1.0)
    for name, fig, col, edge in (("gender_male", male(), "#3E92E6", "#D9AE78"), ("gender_female", female(), "#F06BAA", "#D9AE78")):
        f = OUT / f"{name}.svg"
        f.write_text(card(fig, col, edge))
        pg.goto(f.as_uri())
        pg.screenshot(path=str(OUT / f"{name}.png"), omit_background=True)
    b.close()
for name in ("gender_male", "gender_female"):
    im = Image.open(OUT / f"{name}.png").convert("RGBA")
    im.resize((480, 560), Image.LANCZOS).save(OUT / f"{name}.webp", "WEBP", quality=88, method=6)
both = Image.new("RGBA", (1240, 720), (14, 14, 18, 255))
for i, name in enumerate(("gender_male", "gender_female")):
    both.alpha_composite(Image.open(OUT / f"{name}.png").convert("RGBA"), (10 + i * 620, 10))
both.save(OUT / "gender_preview.png")
print("ok")
