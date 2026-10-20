"""Recycling-Symbol (drei Pfeile im Dreieck, gemeinfreies Symbol) plastisch in Grün."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from bevel import bevel
from rm import finish

S = 1200
C = np.array([S / 2, S / 2 + 60])
R = 400
verts = [C + R * np.array([np.cos(a), -np.sin(a)]) for a in np.radians([90, 210, 330])]


def chaikin(pts, it=5):
    for _ in range(it):
        new = []
        for i in range(len(pts)):
            p, q = pts[i], pts[(i + 1) % len(pts)]
            new += [0.75 * p + 0.25 * q, 0.25 * p + 0.75 * q]
        pts = new
    return pts


# Dreieck mit gerundeten Ecken als dichte Polylinie (Start: Mitte der linken Seite)
base = []
for i in range(3):
    a, b = np.array(verts[i], float), np.array(verts[(i + 1) % 3], float)
    for t_ in np.linspace(0, 1, 12, endpoint=False):
        base.append(a + (b - a) * t_)
pts = chaikin(base, 4)
# dichter abtasten
P = np.array(pts + [pts[0]])
seg = np.linalg.norm(np.diff(P, axis=0), axis=1)
cum = np.concatenate([[0], np.cumsum(seg)])
total = cum[-1]
t = np.linspace(0, total, 3000)
X = np.interp(t, cum, P[:, 0]); Y = np.interp(t, cum, P[:, 1])
path = np.stack([X, Y], -1)
# Seitenmitten finden (nächster Punkt zu den Seitenmittelpunkten)
mids = [(verts[i] + verts[(i + 1) % 3]) / 2 for i in range(3)]
mid_idx = sorted([int(np.argmin(np.linalg.norm(path - m, axis=1))) for m in mids])

mask = Image.new("L", (S, S), 0)
d = ImageDraw.Draw(mask)
W = 92
gap = 60
head_len, head_w = 150, 210
n = len(path)
for k in range(3):
    a = mid_idx[k]
    b = mid_idx[(k + 1) % 3] + (n if k == 2 else 0)
    idx = np.arange(a, b) % n
    pp = path[idx]
    # Länge entlang
    L = np.concatenate([[0], np.cumsum(np.linalg.norm(np.diff(pp, axis=0), axis=1))])
    start = gap / 2
    end = L[-1] - gap / 2 - head_len
    sel = (L >= start) & (L <= end)
    body = pp[sel]
    cl = Image.new("L", (S, S), 0)
    ImageDraw.Draw(cl).line([tuple(v) for v in body], fill=255, width=3)
    from scipy.ndimage import distance_transform_edt
    dist = distance_transform_edt(np.array(cl) < 128)
    band = Image.fromarray(((dist < W / 2) * 255).astype(np.uint8), "L")
    mask.paste(255, (0, 0), band)
    # Pfeilspitze
    tip_i = np.searchsorted(L, L[-1] - gap / 2)
    base_pt = body[-1]
    tip = pp[min(tip_i, len(pp) - 1)]
    dirv = (tip - base_pt) / np.linalg.norm(tip - base_pt)
    nrm = np.array([-dirv[1], dirv[0]])
    d.polygon([tuple(base_pt + nrm * head_w / 2), tuple(base_pt - nrm * head_w / 2), tuple(tip)], fill=255)
    # runde Enden am Anfang
mask = mask.filter(ImageFilter.GaussianBlur(1.5)).point(lambda v: 255 if v > 127 else 0)
img = bevel(mask, (90, 210, 110), depth=26, gloss=0.55, power=30, color2=(20, 140, 80))
finish(img).save(sys.argv[1])
print("ok")
