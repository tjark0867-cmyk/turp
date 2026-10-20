"""Goldene Sternschnuppe mit lila-goldenem Schweif und Funkeln (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.ndimage import distance_transform_edt
from gen_trophy import sd_star5
from rm import finish, glow_layer

S = 1400
cx, cy = 900, 470
yy, xx = np.mgrid[0:S, 0:S].astype(float)
R = 300
ang = np.radians(-14)
xr = (xx - cx) * np.cos(ang) - (yy - cy) * np.sin(ang)
yr = (xx - cx) * np.sin(ang) + (yy - cy) * np.cos(ang)
sd = sd_star5(xr / R, -yr / R, 1.0, 0.48) * R
inside = sd < 0
# facettierte Pyramide: Höhe = Abstand zum Rand
dist = distance_transform_edt(inside)
from scipy.ndimage import gaussian_filter
h = gaussian_filter(dist, 2.5)
gy, gx = np.gradient(h)
nx, ny, nz = -gx * 0.9, -gy * 0.9, np.ones_like(gx) * 0.6
ln = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
nx, ny, nz = nx / ln, ny / ln, nz / ln
L = np.array([-0.55, -0.65, 0.55]); L /= np.linalg.norm(L)
dif = np.clip(nx * L[0] + ny * L[1] + nz * L[2], 0, 1)
# Metall-Umgebung: Reflexion nach oben hell, unten dunkler
ry = -(2 * nz * ny)
env = 0.45 + 0.6 * np.clip(-ny * 1.4 + 0.2, 0, 1) + 0.9 * np.clip(dif - 0.55, 0, 1) * 2
gold = np.array([255, 196, 60]) / 255
rgb = gold[None, None] * (0.6 + 0.6 * dif[..., None]) * env[..., None]
rgb = np.clip(rgb ** 0.9 * 255, 0, 255)
star = Image.fromarray(np.dstack([rgb.astype(np.uint8), (inside * 255).astype(np.uint8)]), "RGBA")
star = star.filter(ImageFilter.SMOOTH)

can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
# Schweif
tail = Image.new("L", (S, S), 0)
td = ImageDraw.Draw(tail)
dirv = np.array([-0.78, 0.62]); dirv /= np.linalg.norm(dirv)
nrm = np.array([-dirv[1], dirv[0]])
for k, (w, ln_) in enumerate([(230, 760), (150, 860), (80, 940)]):
    base = np.array([cx, cy])
    pts = [tuple(base + nrm * w), tuple(base - nrm * w), tuple(base + dirv * ln_)]
    td.polygon(pts, fill=120 + 60 * k)
tail = tail.filter(ImageFilter.GaussianBlur(40))
grad = np.clip(1 - np.hypot(xx - cx, yy - cy) / 900, 0, 1)
tcol = np.zeros((S, S, 4))
c1, c2 = np.array([255, 210, 90]), np.array([160, 90, 255])
tcol[..., :3] = c1 * grad[..., None] + c2 * (1 - grad[..., None])
tcol[..., 3] = np.clip(np.array(tail) * (0.6 + 1.2 * grad), 0, 255)
can.alpha_composite(Image.fromarray(tcol.astype(np.uint8), "RGBA"))
can.alpha_composite(glow_layer(Image.fromarray((inside * 255).astype(np.uint8), "L"), (255, 220, 120), 40, 1.1))
can.alpha_composite(star)
# Funkeln
sp = Image.new("RGBA", (S, S), (0, 0, 0, 0))
sd_ = ImageDraw.Draw(sp)
rng = np.random.default_rng(3)
for i in range(9):
    t = rng.uniform(0.3, 0.95)
    p = np.array([cx, cy]) + dirv * t * 800 + nrm * rng.uniform(-170, 170)
    r = rng.uniform(16, 38) * (1.1 - t * 0.5)
    c = (255, 240, 190, 255) if i % 2 else (225, 190, 255, 255)
    sd_.polygon([(p[0], p[1] - r), (p[0] + r * 0.28, p[1] - r * 0.28), (p[0] + r, p[1]), (p[0] + r * 0.28, p[1] + r * 0.28),
                 (p[0], p[1] + r), (p[0] - r * 0.28, p[1] + r * 0.28), (p[0] - r, p[1]), (p[0] - r * 0.28, p[1] - r * 0.28)], fill=c)
can.alpha_composite(sp)
finish(can).save(sys.argv[1])
print("ok")
