"""Pappe-Symbole für den Pappe-Modus: ausgeschnittene, bemalte Pappstücke mit Filzstift-Details.

Untere Leiste (je an = Mint, aus = Creme): Lounge (Globus), Entdecken (Kartenstapel),
Freunde (Smiley), Chats (Sprechblase). Dazu Filter, Melden-Fähnchen und runde Knopf-Scheiben.
"""
import math
import random
import re
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

OUT = Path(__file__).parent / "out"
OUT.mkdir(exist_ok=True)
DEFS = re.search(r"<defs>(.*?)</defs>", (Path(__file__).parent / "mountain.py").read_text(), re.S).group(1)
S = 240
C = S / 2
TEAL, CREAM, CORAL, SUN, INK = "#2DE1C2", "#FBF4E6", "#FF4D6D", "#FFC94D", "#3B2410"
EDGE = "#C99A62"


def painted(shape, color, s=0.93, cx=C, cy=C):
    """Pappstück: Kraft-Schnittkante, Farbe mit Pinselstruktur, Wellen, leichter Glanz oben."""
    return f'''<g fill="{EDGE}">{shape}</g>
    <g fill="{color}" filter="url(#paint)" transform="translate({cx} {cy}) scale({s}) translate({-cx} {-cy})">{shape}</g>
    <g fill="url(#corrLight)">{shape}</g>
    <g fill="#FFFFFF" opacity="0.10" transform="translate({cx} {cy - 3}) scale({s * 0.97}) translate({-cx} {-cy})">{shape}</g>'''


def marker(d, w=11, fill="none"):
    """Filzstift-Strich (leicht unruhig durch den cutSoft-Filter)."""
    return (f'<path d="{d}" fill="{fill}" stroke="{INK}" stroke-width="{w}" stroke-linecap="round" '
            f'stroke-linejoin="round" filter="url(#cutSoft)" opacity="0.92"/>')


def dot(x, y, r):
    return f'<ellipse cx="{x}" cy="{y}" rx="{r}" ry="{r * 1.12}" fill="{INK}" filter="url(#cutSoft)" opacity="0.92"/>'


def wobble_circle(cx, cy, r, seed, n=60, amp=2.2):
    rnd = random.Random(seed)
    pts = []
    for i in range(n):
        a = 2 * math.pi * i / n
        rr = r + rnd.uniform(-amp, amp)
        pts.append(f"{cx + rr * math.cos(a):.1f},{cy + rr * math.sin(a):.1f}")
    return f'<polygon points="{" ".join(pts)}"/>'


# ---------------------------------------------------------------- Symbole
def globe(col):
    shape = wobble_circle(C, C, 88, 1)
    det = (marker(f"M {C} {C-84} C {C-52} {C-50}, {C-52} {C+50}, {C} {C+84}") +
           marker(f"M {C} {C-84} C {C+52} {C-50}, {C+52} {C+50}, {C} {C+84}") +
           marker(f"M {C-80} {C+1} Q {C} {C-4} {C+80} {C+1}") +
           marker(f"M {C-70} {C-44} Q {C} {C-30} {C+70} {C-44}", 9) +
           marker(f"M {C-70} {C+44} Q {C} {C+58} {C+70} {C+44}", 9))
    return f'<g filter="url(#cut)">{painted(shape, col)}</g>{det}'


def cards(col):
    back = f'<rect x="{C-78}" y="{C-82}" width="112" height="152" rx="16" transform="rotate(-16 {C-22} {C})"/>'
    front = f'<rect x="{C-40}" y="{C-74}" width="116" height="156" rx="16" transform="rotate(9 {C+18} {C+4})"/>'
    back_col = CREAM if col == TEAL else "#EAD5AF"
    # kleine Person auf der vorderen Karte
    det = (f'<g transform="rotate(9 {C+18} {C+4})">' +
           dot(C + 18, C - 26, 20) +
           f'<path d="M {C-16} {C+46} C {C-16} {C+6}, {C+52} {C+6}, {C+52} {C+46} Z" fill="{INK}" '
           f'filter="url(#cutSoft)" opacity="0.92"/>' + '</g>')
    return (f'<g filter="url(#cut)">{painted(back, back_col, cx=C-22)}</g>'
            f'<g filter="url(#cut)">{painted(front, col, cx=C+18, cy=C+4)}</g>{det}')


def smiley(col):
    shape = wobble_circle(C, C, 88, 4)
    det = (dot(C - 32, C - 22, 11) + dot(C + 32, C - 22, 11) +
           marker(f"M {C-46} {C+22} Q {C} {C+70} {C+46} {C+22}", 12))
    return f'<g filter="url(#cut)">{painted(shape, col)}</g>{det}'


def bubble(col):
    shape = (f'<path d="M {C-92} {C-58} Q {C-92} {C-80} {C-70} {C-80} L {C+70} {C-80} Q {C+92} {C-80} {C+92} {C-58} '
             f'L {C+92} {C+30} Q {C+92} {C+52} {C+70} {C+52} L {C-16} {C+52} L {C-58} {C+90} L {C-50} {C+52} '
             f'L {C-70} {C+52} Q {C-92} {C+52} {C-92} {C+30} Z"/>')
    det = dot(C - 40, C - 14, 12) + dot(C, C - 14, 12) + dot(C + 40, C - 14, 12)
    return f'<g filter="url(#cut)">{painted(shape, col)}</g>{det}'


def sliders(col):
    card = f'<rect x="{C-86}" y="{C-80}" width="172" height="160" rx="20" transform="rotate(-4 {C} {C})"/>'
    det = ''
    knobs = ''
    for i, (y, kx, kc) in enumerate(((C - 44, C + 26, TEAL), (C, C - 30, CORAL), (C + 44, C + 8, SUN))):
        det += marker(f"M {C-58} {y} Q {C} {y + 3} {C+58} {y - 1}", 10)
        knobs += f'<g filter="url(#cut)">{painted(wobble_circle(kx, y, 18, 10 + i), kc, s=0.86, cx=kx, cy=y)}</g>'
    return f'<g filter="url(#cut)">{painted(card, col)}</g>{det}{knobs}'


def flag(col):
    pole = f'<rect x="{C-70}" y="{C-92}" width="20" height="190" rx="9"/>'
    cloth = (f'<path d="M {C-52} {C-84} C {C-10} {C-104}, {C+30} {C-58}, {C+84} {C-80} '
             f'L {C+84} {C+8} C {C+30} {C+30}, {C-10} {C-14}, {C-52} {C+6} Z"/>')
    return (f'<g filter="url(#cut)">{painted(pole, "#B98447", cx=C-60)}</g>'
            f'<g filter="url(#cut)">{painted(cloth, col, cx=C+16, cy=C-38)}</g>')


def disc(col):
    return f'<g filter="url(#cut)">{painted(wobble_circle(C, C - 2, 104, 21, n=80, amp=3.2), col, s=0.95)}</g>'


jobs = {
    "nav_lounge_on": globe(TEAL), "nav_lounge_off": globe(CREAM),
    "nav_discover_on": cards(TEAL), "nav_discover_off": cards(CREAM),
    "nav_friends_on": smiley(TEAL), "nav_friends_off": smiley(CREAM),
    "nav_chats_on": bubble(TEAL), "nav_chats_off": bubble(CREAM),
    "pap_filter": sliders(CREAM), "pap_filter_on": sliders(TEAL),
    "pap_flag": flag(CORAL),
    "disc_cream": disc(CREAM),
}

with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": S, "height": S})
    for name, body in jobs.items():
        f = OUT / f"{name}.svg"
        f.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{S}" height="{S}" viewBox="0 0 {S} {S}"><defs>{DEFS}</defs>{body}</svg>')
        pg.goto(f.as_uri())
        pg.screenshot(path=str(OUT / f"{name}.png"), omit_background=True)
        Image.open(OUT / f"{name}.png").convert("RGBA").save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)
    b.close()

# Vorschau auf Pappe
names = list(jobs)
prev = Image.new("RGBA", (S * 6, S * 2), (190, 138, 82, 255))
for i, n in enumerate(names):
    prev.alpha_composite(Image.open(OUT / f"{n}.png"), ((i % 6) * S, (i // 6) * S))
prev.save(OUT / "ui_icons_preview.png")
print("ok", len(jobs))
