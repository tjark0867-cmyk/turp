"""Pappe-Grafiken für die Sprachauswahl: Kraftpapier-Hintergrund, bemalte Schilder (weiß/türkis)."""
import re
import random
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

OUT = Path(__file__).parent / "out"
DEFS = re.search(r"<defs>(.*?)</defs>", (Path(__file__).parent / "mountain.py").read_text(), re.S).group(1)


def torn(rnd, x0, y0, x1, y1, amp=4.5, step=16):
    pts = []
    for x in range(int(x0), int(x1), step):
        pts.append((x, y0 + rnd.uniform(-amp, amp)))
    for y in range(int(y0), int(y1), step):
        pts.append((x1 + rnd.uniform(-amp, amp), y))
    for x in range(int(x1), int(x0), -step):
        pts.append((x, y1 + rnd.uniform(-amp, amp)))
    for y in range(int(y1), int(y0), -step):
        pts.append((x0 + rnd.uniform(-amp, amp), y))
    return " ".join(f"{a:.1f},{b:.1f}" for a, b in pts)


def tag(color, seed, W=560, H=170):
    rnd = random.Random(seed)
    shape = f'<polygon points="{torn(rnd, 20, 22, W - 20, H - 28)}"/>'
    c = (W / 2, H / 2)
    return W, H, f'''<g filter="url(#cut)">
      <g fill="#D9AE78">{shape}</g>
      <g fill="{color}" filter="url(#paint)" transform="translate({c[0]} {c[1]}) scale(0.975 0.93) translate({-c[0]} {-c[1]})">{shape}</g>
      <g fill="url(#corrLight)">{shape}</g>
    </g>'''


def bg():
    W, H = 720, 1280
    return W, H, f'''<rect width="{W}" height="{H}" fill="#C78A4D"/>
    <rect width="{W}" height="{H}" fill="url(#corr)"/>
    <rect width="{W}" height="{H}" filter="url(#blotch)" opacity="0.5"/>
    <rect width="{W}" height="{H}" filter="url(#fibers)" opacity="0.55"/>
    <rect width="{W}" height="{H}" fill="url(#vignette)"/>'''


jobs = {
    "kraft_bg": bg(),
    "tag_white": tag("#FBF8F2", 3),
    "tag_teal": tag("#2DE1C2", 5),
    "tag_button": tag("#2DE1C2", 9, W=760, H=190),
}
with sync_playwright() as p:
    b = p.chromium.launch()
    for name, (W, H, body) in jobs.items():
        f = OUT / f"{name}.svg"
        f.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs>{DEFS}</defs>{body}</svg>')
        pg = b.new_page(viewport={"width": W, "height": H})
        pg.goto(f.as_uri())
        pg.screenshot(path=str(OUT / f"{name}.png"), omit_background=True)
        pg.close()
        im = Image.open(OUT / f"{name}.png").convert("RGBA" if name != "kraft_bg" else "RGB")
        if name != "kraft_bg":
            im = im.crop(im.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox())
        im.save(OUT / f"{name}.webp", "WEBP", quality=86, method=6)
        print(name, im.size)
    b.close()
