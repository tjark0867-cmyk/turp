"""Leuchtende Energiekugel mit Aura und Blitzen (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from rm import finish, glow_layer

S = 1200
C = S / 2
yy, xx = np.mgrid[0:S, 0:S].astype(float)
r = np.hypot(xx - C, yy - C)
ang = np.arctan2(yy - C, xx - C)
rng = np.random.default_rng(11)
# Aura: gezackte Flammen-Zacken
spikes = 330 + 70 * np.abs(np.sin(ang * 7 + 0.4)) ** 3 + 40 * np.sin(ang * 13 + 1.1) + 25 * np.sin(ang * 23)
aura = np.clip((spikes - r) / 60, 0, 1)
core = np.clip(1 - r / 250, 0, 1)
img = np.zeros((S, S, 4))
blue = np.array([40, 110, 255]); cyan = np.array([90, 230, 255]); white = np.array([255, 255, 255])
t = np.clip(r / 250, 0, 1)[..., None]
ball = white * (1 - t) ** 2 + cyan * 2 * t * (1 - t) + blue * t ** 2
a_ball = np.clip((250 - r) / 6, 0, 1)
img[..., :3] = ball * a_ball[..., None] + (cyan * 0.6 + blue * 0.4) * (1 - a_ball[..., None])
img[..., 3] = np.maximum(a_ball, aura * 0.75) * 255
# Wirbel im Kern
swirl = (np.sin(ang * 3 + r / 28) > 0.6) & (r < 240) & (r > 60)
img[swirl, :3] = img[swirl, :3] * 0.8 + white * 0.2
im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
m = Image.fromarray((aura * 255).astype(np.uint8), "L")
can.alpha_composite(glow_layer(m, (60, 150, 255), 40, 1.0))
can.alpha_composite(im)
# Blitze
bolts = Image.new("L", (S, S), 0)
bd = ImageDraw.Draw(bolts)
for k in range(6):
    a0 = k / 6 * 2 * np.pi + rng.uniform(-0.3, 0.3)
    pts = []
    rr = 200
    a = a0
    while rr < 470:
        pts.append((C + rr * np.cos(a), C + rr * np.sin(a)))
        rr += rng.uniform(35, 60)
        a += rng.uniform(-0.25, 0.25)
    bd.line(pts, fill=255, width=9, joint="curve")
can.alpha_composite(glow_layer(bolts, (150, 220, 255), 10, 2.0))
wb = Image.new("RGBA", (S, S), (240, 250, 255, 0))
wb.putalpha(bolts)
can.alpha_composite(wb)
finish(can).save(sys.argv[1])
print("ok")
