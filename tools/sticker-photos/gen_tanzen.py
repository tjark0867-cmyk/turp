"""Tanzende Figur als leuchtende Silhouette mit bunten Bewegungsbögen (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.ndimage import distance_transform_edt
from bevel import bevel
from rm import finish

S = 1300
# Gelenkpunkte einer Tanzpose (Bildkoordinaten)
J = dict(head=(640, 250), neck=(625, 345), hip=(600, 640),
         elbowR=(760, 300), handR=(840, 160),       # Arm hoch
         elbowL=(470, 420), handL=(360, 340),       # Arm zur Seite
         kneeR=(760, 760), footR=(900, 700),        # Bein gekickt
         kneeL=(560, 860), footL=(600, 1060))
bones = [("neck", "hip", 62), ("neck", "elbowR", 34), ("elbowR", "handR", 30), ("neck", "elbowL", 34), ("elbowL", "handL", 30),
         ("hip", "kneeR", 44), ("kneeR", "footR", 38), ("hip", "kneeL", 44), ("kneeL", "footL", 38)]
yy, xx = np.mgrid[0:S, 0:S].astype(float)
d = np.full((S, S), 1e9)
for a, b, r in bones:
    ax, ay = J[a]; bx, by = J[b]
    dx, dy = bx - ax, by - ay
    t = np.clip(((xx - ax) * dx + (yy - ay) * dy) / (dx * dx + dy * dy), 0, 1)
    dist = np.hypot(xx - (ax + t * dx), yy - (ay + t * dy)) - r
    k = 30
    h = np.clip(0.5 + 0.5 * (dist - d) / k, 0, 1)
    d = dist * (1 - h) + d * h - k * h * (1 - h) if np.isfinite(d).all() else dist
hx, hy = J["head"]
dh = np.hypot(xx - hx, yy - hy) - 72
d = np.minimum(d, dh)
mask = Image.fromarray(((d < 0) * 255).astype(np.uint8), "L")
fig = bevel(mask, (255, 90, 160), depth=30, gloss=0.5, power=30, color2=(140, 70, 255))
can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
# Bewegungsbögen
arcs = Image.new("RGBA", (S, S), (0, 0, 0, 0))
ad = ImageDraw.Draw(arcs)
for i, (col, box, a0, a1) in enumerate([((40, 210, 195, 255), (330, 60, 1010, 740), 200, 330),
                                        ((255, 200, 60, 255), (520, 430, 990, 900), 255, 385),
                                        ((120, 140, 255, 255), (230, 230, 760, 760), 150, 250)]):
    ad.arc(box, a0, a1, fill=col, width=34)
can.alpha_composite(arcs)
sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
sh.putalpha(mask.filter(ImageFilter.GaussianBlur(14)).point(lambda v: int(v * 0.35)))
can.alpha_composite(sh, (8, 14)) if False else can.alpha_composite(Image.new("RGBA", (S, S)), (0, 0))
can.alpha_composite(fig)
# Funken
sp = ImageDraw.Draw(can)
for (x, y, r, c) in [(960, 220, 30, (255, 220, 90, 255)), (300, 640, 24, (40, 210, 195, 255))]:
    sp.polygon([(x, y - r), (x + r * 0.3, y - r * 0.3), (x + r, y), (x + r * 0.3, y + r * 0.3), (x, y + r),
                (x - r * 0.3, y + r * 0.3), (x - r, y), (x - r * 0.3, y - r * 0.3)], fill=c)
finish(can).save(sys.argv[1])
print("ok")
