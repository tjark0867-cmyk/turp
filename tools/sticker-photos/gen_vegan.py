"""Rundes Vegan-Siegel mit Keimling (eigenes Design, kein offizielles Label)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from bevel import bevel
from rm import finish

S = 1200
C = S / 2
font = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf", 190)

# Grundscheibe mit Wellenrand
ang = np.linspace(0, 2 * np.pi, 720, endpoint=False)
r = 520 + 16 * np.cos(ang * 24)
disc = Image.new("L", (S, S), 0)
ImageDraw.Draw(disc).polygon([(C + rr * np.cos(a), C + rr * np.sin(a)) for rr, a in zip(r, ang)], fill=255)
img = bevel(disc, (120, 210, 90), depth=40, gloss=0.35, power=25, color2=(40, 150, 70))

# Ring
ring = Image.new("L", (S, S), 0)
d = ImageDraw.Draw(ring)
d.ellipse((C - 440, C - 440, C + 440, C + 440), outline=255, width=16)
img.alpha_composite(bevel(ring, (240, 255, 230), depth=6, gloss=0.4))

# Keimling: Stängel + zwei Blätter
sprout = Image.new("L", (S, S), 0)
d = ImageDraw.Draw(sprout)
d.line([(C, C + 60), (C, C - 130)], fill=255, width=34)


def leaf(cx, cy, length, angle, width):
    t = np.linspace(0, 1, 80)
    xs = t * length
    ys = np.sin(t * np.pi) * width
    pts = list(zip(xs, ys)) + list(zip(xs[::-1], -ys[::-1]))
    ca, sa = np.cos(angle), np.sin(angle)
    return [(cx + x * ca - y * sa, cy + x * sa + y * ca) for x, y in pts]


d.polygon(leaf(C, C - 120, 300, np.radians(-35), 95), fill=255)
d.polygon(leaf(C, C - 90, 260, np.radians(-150), 82), fill=255)
img.alpha_composite(bevel(sprout, (250, 255, 240), depth=14, gloss=0.5, color2=(220, 240, 210)))
# Blattadern
vein = Image.new("RGBA", (S, S), (0, 0, 0, 0))
dv = ImageDraw.Draw(vein)
for (cx, cy, L, a) in ((C, C - 120, 280, -35), (C, C - 90, 240, -150)):
    ca, sa = np.cos(np.radians(a)), np.sin(np.radians(a))
    dv.line([(cx + 20 * ca, cy + 20 * sa), (cx + L * ca, cy + L * sa)], fill=(120, 190, 100, 200), width=8)
img.alpha_composite(vein)

# Schrift
txt = Image.new("L", (S, S), 0)
ImageDraw.Draw(txt).text((C, C + 210), "VEGAN", font=font, anchor="mm", fill=255)
img.alpha_composite(bevel(txt, (255, 255, 250), depth=8, gloss=0.5, color2=(225, 240, 220)))
finish(img).save(sys.argv[1])
print("ok")
