"""Papp-Grafiken fürs Onboarding: Namensschild, Geburtstagstorte, Kofferanhänger (transparent, ausgeschnitten)."""
import re
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

OUT = Path(__file__).parent / "out"
OUT.mkdir(exist_ok=True)
DEFS = re.search(r"<defs>(.*?)</defs>", (Path(__file__).parent / "mountain.py").read_text(), re.S).group(1)
FONT = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"
STYLE = f"<style>@font-face {{ font-family: P; src: url('file://{FONT}'); }} text {{ font-family: P; }}</style>"


def svg(w, h, body):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">{STYLE}
<defs>{DEFS}</defs>{body}</svg>'''


def painted(shape, color, cx, cy, edge="#D9AE78", s=0.965):
    """Pappe-Unterlage + bemalte Fläche (Pappe schimmert am Rand durch) + Wellenstruktur"""
    return f'''<g fill="{edge}">{shape}</g>
    <g fill="{color}" filter="url(#paint)" transform="translate({cx} {cy}) scale({s}) translate({-cx} {-cy})">{shape}</g>
    <g fill="url(#corrLight)">{shape}</g>'''


def nametag():
    W, H = 640, 440
    card = '<rect x="20" y="20" width="600" height="400" rx="34"/>'
    head = '<path d="M 20 54 Q 20 20 54 20 L 586 20 Q 620 20 620 54 L 620 168 L 20 168 Z"/>'
    white = '<rect x="46" y="186" width="548" height="212" rx="18"/>'
    body = f'''<g filter="url(#cut)">{painted(card, "#E9DCC6", 320, 220)}</g>
    <g filter="url(#cutSoft)">{painted(head, "#E5484D", 320, 94, edge="#B9373B", s=0.985)}</g>
    <g filter="url(#cutSoft)"><g fill="#FBF8F2">{white}</g><g fill="url(#corrLight)" opacity="0.5">{white}</g></g>
    <text x="320" y="112" text-anchor="middle" font-size="86" fill="#FFF7F0" letter-spacing="6">HALLO</text>
    <text x="320" y="155" text-anchor="middle" font-size="30" fill="#FFE3DE">ich bin</text>'''
    return W, H, body


def cake():
    W, H = 600, 560
    plate = '<ellipse cx="300" cy="480" rx="270" ry="46"/>'
    bottom = '<rect x="70" y="300" width="460" height="170" rx="26"/>'
    top = '<rect x="150" y="196" width="300" height="122" rx="22"/>'
    drip_b = "M 70 330 Q 70 300 100 300 L 500 300 Q 530 300 530 330 L 530 352 " + "".join(
        f"Q {530 - i * 46 - 12} {388 if i % 2 == 0 else 362} {530 - i * 46 - 23} 352 " for i in range(20)) + "Z"
    drip_b = re.sub(r"Q (\S+) (\S+) (\S+) 352 ", lambda m: f"Q {m.group(1)} {m.group(2)} {m.group(3)} 352 ", drip_b)
    # Tropfen der Glasur: abwechselnd lang/kurz
    drips_b = "".join(f'<path d="M {x} 340 q 12 0 12 14 l 0 {L} a 12 12 0 0 1 -24 0 l 0 -{L} q 0 -14 12 -14 z"/>'
                      for x, L in zip(range(96, 520, 44), [30, 12, 40, 18, 26, 10, 36, 16, 28, 14]))
    icing_b = '<rect x="64" y="296" width="472" height="56" rx="26"/>'
    icing_t = '<rect x="144" y="192" width="312" height="44" rx="20"/>'
    drips_t = "".join(f'<path d="M {x} 228 q 10 0 10 12 l 0 {L} a 10 10 0 0 1 -20 0 l 0 -{L} q 0 -12 10 -12 z"/>'
                      for x, L in zip(range(170, 440, 38), [22, 10, 28, 14, 20, 8, 24]))
    candles = "".join(f'<rect x="{x - 13}" y="98" width="26" height="104" rx="8"/>' for x in (230, 300, 370))
    stripes = "".join(f'<path d="M {x - 13} {y} l 26 -16 l 0 12 l -26 16 z"/>' for x in (230, 300, 370) for y in (128, 160, 192))
    flames = "".join(f'<path d="M {x} 40 C {x + 22} 66 {x + 20} 92 {x} 96 C {x - 20} 92 {x - 22} 66 {x} 40 Z"/>' for x in (230, 300, 370))
    inner = "".join(f'<path d="M {x} 62 C {x + 10} 76 {x + 10} 90 {x} 92 C {x - 10} 90 {x - 10} 76 {x} 62 Z"/>' for x in (230, 300, 370))
    sprinkle_cols = ["#2DE1C2", "#FFC94D", "#9C8CFF", "#FF6F91", "#4DB8FF"]
    sprinkles = "".join(
        f'<rect x="{x}" y="{y}" width="16" height="6" rx="3" fill="{sprinkle_cols[i % 5]}" transform="rotate({(i * 47) % 180} {x + 8} {y + 3})"/>'
        for i, (x, y) in enumerate([(110, 400), (170, 430), (230, 395), (290, 440), (350, 405), (410, 435), (465, 400),
                                   (140, 380), (380, 380), (200, 270), (260, 290), (320, 265), (390, 285)]))
    body = f'''<g filter="url(#cutSoft)">{painted(plate, "#EDEFF2", 300, 480)}</g>
    <g filter="url(#cut)">{painted(bottom, "#F48FB1", 300, 385)}</g>
    <g filter="url(#cutSoft)"><g fill="#FFFDF8">{icing_b}{drips_b}</g><g fill="url(#corrLight)" opacity="0.35">{icing_b}{drips_b}</g></g>
    <g filter="url(#cut)">{painted(top, "#FFD98A", 300, 257)}</g>
    <g filter="url(#cutSoft)"><g fill="#FFFDF8">{icing_t}{drips_t}</g><g fill="url(#corrLight)" opacity="0.35">{icing_t}{drips_t}</g></g>
    <g filter="url(#cutSoft)">{painted(candles, "#7FE3D0", 300, 150)}<g fill="#FFFFFF" opacity="0.85">{stripes}</g></g>
    <g filter="url(#cutSoft)"><g fill="#FFB238">{flames}</g><g fill="#FFF3A0">{inner}</g></g>
    <g filter="url(#cutSoft)">{sprinkles}</g>'''
    return W, H, body


def tag():
    W, H = 640, 400
    shape = '<path d="M 130 40 L 590 40 Q 616 40 616 66 L 616 334 Q 616 360 590 360 L 130 360 L 36 270 L 36 130 Z"/>'
    label = '<rect x="196" y="78" width="384" height="244" rx="16"/>'
    hole = '<circle cx="96" cy="200" r="24"/>'
    body = f'''<path d="M 96 200 C 60 150, 30 120, 10 60 C 4 40, 30 20, 50 34" fill="none" stroke="#C0392B" stroke-width="9" stroke-linecap="round"/>
    <path d="M 96 200 C 60 150, 30 120, 10 60 C 4 40, 30 20, 50 34" fill="none" stroke="#FFFFFF" stroke-width="9" stroke-linecap="round" stroke-dasharray="14 14"/>
    <g filter="url(#cut)">
      <mask id="m"><rect width="{W}" height="{H}" fill="#fff"/><circle cx="96" cy="200" r="16" fill="#000"/></mask>
      <g mask="url(#m)">{painted(shape, "#C98B4E", 326, 200, edge="#A8703A", s=0.985)}</g>
    </g>
    <circle cx="96" cy="200" r="22" fill="none" stroke="#C9CDD4" stroke-width="10"/>
    <circle cx="96" cy="200" r="22" fill="none" stroke="#8C9099" stroke-width="2"/>
    <g filter="url(#cutSoft)"><g fill="#FBF8F2">{label}</g><g fill="url(#corrLight)" opacity="0.4">{label}</g></g>
    <path d="M 96 200 C 120 230, 150 230, 170 210" fill="none" stroke="#C0392B" stroke-width="9" stroke-linecap="round"/>'''
    return W, H, body


with sync_playwright() as p:
    b = p.chromium.launch()
    for name, fn in (("onb_nametag", nametag), ("onb_cake", cake), ("onb_tag", tag)):
        W, H, body = fn()
        f = OUT / f"{name}.svg"
        f.write_text(svg(W, H, body))
        pg = b.new_page(viewport={"width": W, "height": H}, device_scale_factor=1.0)
        pg.goto(f.as_uri())
        pg.wait_for_timeout(300)
        pg.screenshot(path=str(OUT / f"{name}.png"), omit_background=True)
        pg.close()
        im = Image.open(OUT / f"{name}.png").convert("RGBA")
        im.save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)
    b.close()
prev = Image.new("RGBA", (1960, 600), (14, 14, 18, 255))
x = 20
for name in ("onb_nametag", "onb_cake", "onb_tag"):
    im = Image.open(OUT / f"{name}.png")
    prev.alpha_composite(im, (x, 20))
    x += im.width + 20
prev.save(OUT / "onb_preview.png")
print("ok")
