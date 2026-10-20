"""Glänzender pinker Herzballon (selbst erzeugt)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

N = 1200
y, x = np.mgrid[0:N, 0:N].astype(float)
u, v = (x - N / 2) / (N * 0.36), (N * 0.42 - y) / (N * 0.36)
# Herzform: (u²+v²-1)³ - u² v³ <= 0
f = (u ** 2 + v ** 2 - 1) ** 3 - u ** 2 * v ** 3
inside = f <= 0
# Pseudo-Normale aus dem geglätteten Feld für Wölbung
mask = inside.astype(float)
from scipy.ndimage import gaussian_filter, distance_transform_edt  # noqa: E402
dist = distance_transform_edt(inside)
hgt = np.sqrt(np.clip(dist / dist.max(), 0, 1))
hgt = gaussian_filter(hgt, 6)
gy, gx = np.gradient(hgt)
nx, ny = -gx * 90, -gy * 90
nz = np.ones_like(nx)
ln = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
nx, ny, nz = nx / ln, ny / ln, nz / ln
L = np.array([-0.5, -0.55, 0.67]); L /= np.linalg.norm(L)
diff = np.clip(nx * L[0] + ny * L[1] + nz * L[2], 0, 1)
H = L + np.array([0, 0, 1.0]); H /= np.linalg.norm(H)
spec = np.clip(nx * H[0] + ny * H[1] + nz * H[2], 0, 1) ** 60
base = np.array([236, 64, 150], float)
img = base * (0.35 + 0.75 * diff)[..., None] + 255 * 0.85 * spec[..., None]
img = np.clip(img, 0, 255).astype(np.uint8)
alpha = (inside * 255).astype(np.uint8)
im = Image.fromarray(np.dstack([img, alpha]), "RGBA")
# Knoten und Schnur
d = ImageDraw.Draw(im)
bottom = int(np.max(np.where(inside.any(axis=1))))
cx = N // 2
d.polygon([(cx - 22, bottom + 2), (cx + 22, bottom + 2), (cx, bottom - 20)], fill=(200, 40, 120, 255))
pts = [(cx + int(18 * np.sin(t / 30)), bottom + t) for t in range(0, 230, 4)]
d.line(pts, fill=(230, 230, 235, 255), width=5)
im = im.crop(im.getbbox()).resize((640, int(640 * im.getbbox()[3] / max(1, im.getbbox()[2]))), Image.LANCZOS) if False else im.crop(im.getbbox())
im.thumbnail((800, 800), Image.LANCZOS)
im.save(sys.argv[1])
print("ok")
