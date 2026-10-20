"""Erzeugt einen realistisch beleuchteten Handball (kein Foto verfügbar)."""
import sys

import numpy as np
from PIL import Image

N = 1400          # intern groß, dann verkleinern (Kantenglättung)
R = 0.92

y, x = np.mgrid[-1:1:N * 1j, -1:1:N * 1j]
r2 = (x / R) ** 2 + (y / R) ** 2
inside = r2 <= 1.0
z = np.sqrt(np.clip(1 - r2, 0, 1))
nx, ny, nz = x / R, y / R, z

# Kugel etwas drehen, damit das Muster schräg liegt
a, b = 1.25, 0.5
cx_, sx_ = np.cos(a), np.sin(a)
X = nx
Y = ny * cx_ - nz * sx_
Z = ny * sx_ + nz * cx_
cb, sb = np.cos(b), np.sin(b)
X, Z = X * cb + Z * sb, -X * sb + Z * cb

phi = np.arctan2(Y, X)
h = 0.5 * np.cos(2 * phi)
d = Z - h

blue = np.array([24, 58, 150], float)
yellow = np.array([250, 196, 30], float)
white = np.array([245, 245, 240], float)
seam = np.array([30, 30, 34], float)

col = np.where((d > 0)[..., None], blue, yellow)
band = (np.abs(d) > 0.018) & (np.abs(d) < 0.13)
col = np.where(band[..., None], white, col)
# zweite, schmale Ziernaht
col = np.where(((np.abs(d) > 0.13) & (np.abs(d) < 0.145))[..., None], seam * 0.9 + 40, col)
col = np.where((np.abs(d) <= 0.018)[..., None], seam, col)

# genoppte Oberfläche: feines Rauschen auf die Normale
rng = np.random.default_rng(7)
noise = rng.normal(0, 1, (N, N))
from scipy.ndimage import gaussian_filter  # noqa: E402

bump = gaussian_filter(noise, 1.6)
bump = bump / np.abs(bump).max()
gx, gy = np.gradient(bump)
k = 0.9
bnx, bny, bnz = nx - k * gx, ny - k * gy, nz
ln = np.sqrt(bnx ** 2 + bny ** 2 + bnz ** 2) + 1e-9
bnx, bny, bnz = bnx / ln, bny / ln, bnz / ln

L = np.array([-0.45, -0.6, 0.66])
L = L / np.linalg.norm(L)
diff = np.clip(bnx * L[0] + bny * L[1] + bnz * L[2], 0, 1)
# Spiegelung (Blinn)
H = L + np.array([0, 0, 1.0])
H = H / np.linalg.norm(H)
spec = np.clip(bnx * H[0] + bny * H[1] + bnz * H[2], 0, 1) ** 38
rim = np.clip(1 - nz, 0, 1) ** 2.2

shade = 0.28 + 0.78 * diff
img = col * shade[..., None] + 255 * 0.32 * spec[..., None] - 60 * rim[..., None]
img = np.clip(img, 0, 255).astype(np.uint8)

alpha = (inside * 255).astype(np.uint8)
im = Image.fromarray(np.dstack([img, alpha]), "RGBA").resize((700, 700), Image.LANCZOS)
im.save(sys.argv[1] if len(sys.argv) > 1 else "handball.png")
print("ok")
