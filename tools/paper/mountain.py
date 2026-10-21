"""Bastel-Berglandschaft (Kraftpapier, gemalte Pappe) als SVG -> PNG/WebP."""
import random
import sys
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

W, H = 1240, 620
OUT = Path(__file__).parent / "out"
OUT.mkdir(exist_ok=True)
rnd = random.Random(4)


def pts(p):
    return " ".join(f"{x:.1f},{y:.1f}" for x, y in p)


def jag(points, amp=4):
    """Kante leicht unregelmäßig machen (zusätzliche Zwischenpunkte)."""
    out = []
    for (x1, y1), (x2, y2) in zip(points, points[1:]):
        out.append((x1, y1))
        n = max(1, int(((x2 - x1) ** 2 + (y2 - y1) ** 2) ** 0.5 / 18))
        for i in range(1, n):
            t = i / n
            out.append((x1 + (x2 - x1) * t + rnd.uniform(-amp, amp), y1 + (y2 - y1) * t + rnd.uniform(-amp, amp)))
    out.append(points[-1])
    return out


def tree(cx, base, h, w, tiers=6):
    """Tanne aus Pappe: übereinanderliegende Zickzack-Stufen."""
    shapes = []
    trunk = f'<rect x="{cx - w * 0.07:.1f}" y="{base - h * 0.14:.1f}" width="{w * 0.14:.1f}" height="{h * 0.16:.1f}" fill="#7A5230"/>'
    shapes.append(trunk)
    for i in range(tiers):
        t = i / (tiers - 1)
        top = base - h + h * 0.13 * i
        bottom = top + h * 0.30
        half = w * (0.22 + 0.30 * t)
        # untere Kante als Zickzack
        teeth = 3 + i
        poly = [(cx, top)]
        for k in range(teeth * 2 + 1):
            x = cx + half - (2 * half) * k / (teeth * 2)
            y = bottom - (h * 0.035 if k % 2 else 0)
            poly.append((x, y))
        poly = poly[:1] + list(reversed(poly[1:]))
        shade = ["#4E8A4B", "#5A9856", "#62A35C", "#6AAD62", "#71B567", "#78BC6C"][i % 6]
        shapes.append(f'<polygon points="{pts(poly)}" fill="{shade}"/>')
        # helle Kante rechts oben (Licht von links -> rechts dunkler)
        shapes.append(f'<polygon points="{pts([(cx, top), (cx + half, bottom), (cx + half * 0.55, bottom - h * 0.02)])}" fill="#000" opacity="0.10"/>')
    return "\n".join(shapes)


def cloud(cx, cy, s):
    bumps = [(-0.55, 0.10, 0.32), (-0.25, -0.12, 0.42), (0.12, -0.20, 0.48), (0.48, -0.02, 0.36), (0.72, 0.14, 0.24), (-0.80, 0.20, 0.20)]
    circles = "".join(f'<circle cx="{cx + dx * s:.1f}" cy="{cy + dy * s:.1f}" r="{r * s:.1f}"/>' for dx, dy, r in bumps)
    base = f'<rect x="{cx - 0.95 * s:.1f}" y="{cy + 0.05 * s:.1f}" width="{1.85 * s:.1f}" height="{0.36 * s:.1f}" rx="{0.18 * s:.1f}"/>'
    return circles + base


mountains = {
    "back": [(-10, 620), (-10, 470), (90, 430), (170, 450), (300, 380), (390, 415), (470, 360), (560, 400), (700, 360),
             (800, 395), (900, 340), (1010, 385), (1120, 350), (1250, 400), (1250, 620)],
    "left": [(-30, 620), (40, 500), (100, 440), (150, 415), (180, 392), (205, 372), (232, 390), (262, 412), (300, 440),
             (350, 480), (400, 530), (450, 620)],
    "right": [(760, 620), (830, 530), (880, 470), (925, 430), (955, 405), (985, 382), (1012, 398), (1045, 420),
              (1090, 452), (1150, 480), (1250, 505), (1250, 620)],
    "main": [(230, 620), (300, 545), (350, 500), (400, 470), (445, 445), (490, 405), (530, 365), (565, 330),
             (595, 308), (622, 290), (648, 310), (668, 298), (694, 322), (730, 352), (775, 385), (820, 410),
             (870, 445), (930, 495), (1000, 555), (1060, 620)],
}
# Schattenseite (rechts vom Grat) und Schneekappen
shade = {
    "main": [(622, 290), (648, 310), (668, 298), (694, 322), (730, 352), (775, 385), (820, 410), (870, 445), (930, 495),
             (1000, 555), (1060, 620), (640, 620), (630, 520), (612, 430), (618, 360)],
    "left": [(205, 372), (232, 390), (262, 412), (300, 440), (350, 480), (400, 530), (450, 620), (230, 620), (215, 500), (200, 420)],
    "right": [(985, 382), (1012, 398), (1045, 420), (1090, 452), (1150, 480), (1250, 505), (1250, 620), (1000, 620), (992, 500), (980, 430)],
}
snow = {
    "main": [(520, 376), (548, 345), (575, 322), (600, 304), (622, 290), (648, 310), (668, 298), (694, 322), (722, 345),
             (738, 360), (722, 372), (708, 362), (700, 388), (686, 368), (672, 405), (660, 378), (646, 420), (634, 384),
             (620, 410), (606, 380), (594, 398), (580, 372), (566, 392), (552, 372), (538, 388)],
    "left": [(160, 410), (180, 392), (205, 372), (232, 390), (255, 406), (244, 418), (232, 410), (224, 436), (212, 414),
             (200, 438), (190, 414), (176, 426)],
    "right": [(940, 415), (962, 398), (985, 382), (1012, 398), (1036, 414), (1024, 424), (1012, 418), (1004, 444),
              (992, 420), (980, 446), (970, 420), (954, 430)],
    "back": [(280, 392), (300, 380), (322, 390), (312, 398), (300, 392), (290, 402)],
    "back2": [(880, 352), (900, 340), (922, 352), (910, 360), (900, 354), (890, 364)],
}
colors = {"back": "#A3ACB3", "left": "#7F878F", "right": "#7C848C", "main": "#6E767E"}

layers = []
for name in ["back", "left", "right", "main"]:
    p = jag(mountains[name], 3)
    sid = name if name != "back" else "back"
    snow_parts = [snow[name]] + ([snow["back2"]] if name == "back" else [])
    snow_svg = "".join(f'<polygon points="{pts(jag(s, 2))}" fill="url(#snowfill)"/>' for s in snow_parts)
    shade_svg = f'<polygon points="{pts(shade[name])}" fill="#1C2228" opacity="0.32"/>' if name in shade else ""
    layers.append(f'''
  <g filter="url(#cut)">
    <clipPath id="c_{name}"><polygon points="{pts(p)}"/></clipPath>
    <g clip-path="url(#c_{name})">
      <rect width="{W}" height="{H}" fill="{colors[name]}"/>
      <rect width="{W}" height="{H}" filter="url(#streakDark)" opacity="0.55"/>
      <rect width="{W}" height="{H}" filter="url(#streakLight)" opacity="0.35"/>
      {shade_svg}
      {snow_svg}
    </g>
  </g>''')

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <!-- Kraftpapier: Fasern und Wellpappen-Streifen -->
  <filter id="fibers" x="0" y="0" width="100%" height="100%">
    <feTurbulence type="fractalNoise" baseFrequency="0.9 0.35" numOctaves="3" seed="2"/>
    <feColorMatrix type="matrix" values="0 0 0 0 0.35  0 0 0 0 0.20  0 0 0 0 0.08  1.4 0 0 0 -0.62"/>
  </filter>
  <filter id="blotch" x="0" y="0" width="100%" height="100%">
    <feTurbulence type="fractalNoise" baseFrequency="0.006" numOctaves="2" seed="9"/>
    <feColorMatrix type="matrix" values="0 0 0 0 0.45  0 0 0 0 0.26  0 0 0 0 0.10  1.2 0 0 0 -0.45"/>
  </filter>
  <pattern id="corr" width="15" height="10" patternUnits="userSpaceOnUse">
    <rect width="3" height="10" fill="#7a4a1e" opacity="0.10"/>
    <rect x="7" width="2" height="10" fill="#fff" opacity="0.05"/>
  </pattern>
  <pattern id="corrLight" width="11" height="10" patternUnits="userSpaceOnUse">
    <rect width="2" height="10" fill="#8a6a45" opacity="0.13"/>
  </pattern>
  <radialGradient id="vignette" cx="0.5" cy="0.45" r="0.75">
    <stop offset="0.6" stop-color="#000" stop-opacity="0"/>
    <stop offset="1" stop-color="#3a2108" stop-opacity="0.35"/>
  </radialGradient>
  <!-- gemalte Pinselstriche (senkrecht) -->
  <filter id="streakDark" x="0" y="0" width="100%" height="100%">
    <feTurbulence type="fractalNoise" baseFrequency="0.09 0.006" numOctaves="3" seed="5"/>
    <feColorMatrix type="matrix" values="0 0 0 0 0.12  0 0 0 0 0.14  0 0 0 0 0.16  2.2 0 0 0 -1.0"/>
  </filter>
  <filter id="streakLight" x="0" y="0" width="100%" height="100%">
    <feTurbulence type="fractalNoise" baseFrequency="0.07 0.005" numOctaves="3" seed="11"/>
    <feColorMatrix type="matrix" values="0 0 0 0 0.92  0 0 0 0 0.94  0 0 0 0 0.96  2.4 0 0 0 -1.25"/>
  </filter>
  <linearGradient id="snowfill" x1="0" y1="0" x2="0.4" y2="1">
    <stop offset="0" stop-color="#FFFFFF"/>
    <stop offset="1" stop-color="#DCE3E8"/>
  </linearGradient>
  <!-- ausgeschnittene Pappe: unregelmäßige Kante, dunkler Rand, Schatten -->
  <filter id="cut" x="-5%" y="-10%" width="110%" height="120%">
    <feTurbulence type="fractalNoise" baseFrequency="0.05" numOctaves="2" seed="3" result="n"/>
    <feDisplacementMap in="SourceGraphic" in2="n" scale="5" xChannelSelector="R" yChannelSelector="G" result="shape"/>
    <feMorphology in="shape" operator="erode" radius="1.6" result="inner"/>
    <feComposite in="shape" in2="inner" operator="out" result="ring"/>
    <feFlood flood-color="#3b2a1a" flood-opacity="0.75"/>
    <feComposite in2="ring" operator="in" result="edge"/>
    <feGaussianBlur in="shape" stdDeviation="5" result="b"/>
    <feOffset in="b" dx="3" dy="6" result="o"/>
    <feFlood flood-color="#2a1606" flood-opacity="0.45"/>
    <feComposite in2="o" operator="in" result="shadow"/>
    <feMerge><feMergeNode in="shadow"/><feMergeNode in="shape"/><feMergeNode in="edge"/></feMerge>
  </filter>
  <filter id="cutSoft" x="-10%" y="-20%" width="120%" height="140%">
    <feTurbulence type="fractalNoise" baseFrequency="0.06" numOctaves="2" seed="8" result="n"/>
    <feDisplacementMap in="SourceGraphic" in2="n" scale="4" xChannelSelector="R" yChannelSelector="G" result="shape"/>
    <feGaussianBlur in="shape" stdDeviation="4" result="b"/>
    <feOffset in="b" dx="2" dy="5" result="o"/>
    <feFlood flood-color="#2a1606" flood-opacity="0.40"/>
    <feComposite in2="o" operator="in" result="shadow"/>
    <feMerge><feMergeNode in="shadow"/><feMergeNode in="shape"/></feMerge>
  </filter>
  <!-- Farbe auf Pappe: an den Rändern schimmert die Pappe durch -->
  <filter id="paint" x="-5%" y="-5%" width="110%" height="110%">
    <feTurbulence type="fractalNoise" baseFrequency="0.35" numOctaves="3" seed="21" result="n"/>
    <feColorMatrix in="n" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  -6 0 0 0 4.5" result="holes"/>
    <feComposite in="SourceGraphic" in2="holes" operator="in"/>
  </filter>
</defs>

<!-- Hintergrund: Kraftpapier -->
<rect width="{W}" height="{H}" fill="#C78A4D"/>
<rect width="{W}" height="{H}" fill="url(#corr)"/>
<rect width="{W}" height="{H}" filter="url(#blotch)" opacity="0.5"/>
<rect width="{W}" height="{H}" filter="url(#fibers)" opacity="0.55"/>

<!-- Wolken: weiß bemalte Pappe -->
<g filter="url(#cutSoft)">
  <g fill="#D9AE78">{cloud(150, 115, 120)}</g>
  <g fill="#F6F3EE" filter="url(#paint)" transform="translate(150 115) scale(0.94) translate(-150 -115)">{cloud(150, 115, 120)}</g>
  <g fill="url(#corrLight)">{cloud(150, 115, 120)}</g>
</g>
<g filter="url(#cutSoft)">
  <g fill="#D9AE78">{cloud(1085, 150, 95)}</g>
  <g fill="#F6F3EE" filter="url(#paint)" transform="translate(1085 150) scale(0.94) translate(-1085 -150)">{cloud(1085, 150, 95)}</g>
  <g fill="url(#corrLight)">{cloud(1085, 150, 95)}</g>
</g>

{"".join(layers)}

<!-- grüner Hügel vorne rechts -->
<g filter="url(#cutSoft)">
  <path d="M 1010 620 C 1040 560, 1120 540, 1250 548 L 1250 620 Z" fill="#7DBE4E"/>
  <path d="M 1010 620 C 1040 560, 1120 540, 1250 548 L 1250 620 Z" fill="url(#corrLight)"/>
</g>
<g filter="url(#cutSoft)">
  <path d="M -10 620 C 30 585, 120 572, 260 590 L 300 620 Z" fill="#6FB247"/>
</g>

<!-- Tannen aus Pappe -->
<g filter="url(#cutSoft)">{tree(150, 612, 250, 150)}</g>
<g filter="url(#cutSoft)">{tree(250, 616, 170, 105, 5)}</g>
<g filter="url(#cutSoft)">{tree(1130, 600, 200, 125)}</g>
<g opacity="0.5">
  <g fill="url(#corrLight)">
    <rect x="70" y="360" width="160" height="260" opacity="0"/>
  </g>
</g>

<rect width="{W}" height="{H}" fill="url(#vignette)"/>
</svg>'''

(OUT / "mountain.svg").write_text(svg)
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": W, "height": H}, device_scale_factor=1.5)
    pg.goto((OUT / "mountain.svg").as_uri())
    pg.screenshot(path=str(OUT / "mountain.png"))
    b.close()
img = Image.open(OUT / "mountain.png").convert("RGB")
img.resize((W, H), Image.LANCZOS).save(OUT / "mountain-preview.png")
print("ok", img.size)
