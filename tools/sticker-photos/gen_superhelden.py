"""Roter Heldenumhang, der im Wind weht, mit goldener Spange (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageDraw
from cloth import mask_from, fabric, catmull
from bevel import bevel
from rm import finish

S = 1300
# Umriss: Kragen oben links, weht nach rechts unten aus
outline = [(300, 250), (420, 205), (540, 240), (700, 330), (880, 420), (1060, 470), (1190, 560),
           (1140, 640), (1180, 760), (1080, 820), (1110, 960), (960, 990), (900, 1110), (760, 1060),
           (600, 1120), (520, 980), (380, 900), (300, 720), (270, 520), (250, 360)]
m = mask_from((S, S), outline)
folds = [
    (380, 300, 1150, 600, 40, 26), (380, 330, 1120, 800, 45, -30), (360, 380, 1060, 980, 45, 28),
    (340, 420, 880, 1080, 40, -26), (320, 470, 660, 1060, 38, 22), (300, 520, 470, 930, 35, -18),
]
cape = fabric(m, (226, 34, 48), folds, depth=70, sheen=0.18, color_dark=(170, 18, 34))
# Innenseite (dunkler) am oberen Rand sichtbar
inner = mask_from((S, S), [(300, 250), (420, 205), (540, 240), (700, 330), (880, 420), (1060, 470), (1190, 560),
                           (1100, 540), (900, 470), (700, 390), (520, 300), (400, 270)])
inn = fabric(inner, (130, 14, 30), [], depth=20, sheen=0.05)
can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
can.alpha_composite(cape)
can.alpha_composite(inn)
# Kragen-Spange
cl = Image.new("L", (S, S), 0)
ImageDraw.Draw(cl).ellipse((280, 200, 420, 340), fill=255)
can.alpha_composite(bevel(cl, (255, 210, 80), depth=30, gloss=0.8, power=40, color2=(200, 140, 30)))
st = Image.new("L", (S, S), 0)
pts = []
for i in range(10):
    rr = 48 if i % 2 == 0 else 20
    a = np.radians(-90 + i * 36)
    pts.append((350 + rr * np.cos(a), 270 + rr * np.sin(a)))
ImageDraw.Draw(st).polygon(pts, fill=255)
can.alpha_composite(bevel(st, (255, 245, 200), depth=8, gloss=0.6))
finish(can).save(sys.argv[1])
print("ok")
