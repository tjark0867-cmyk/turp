"""Nahtlose Papier-Struktur (RGBA) für den Pappe-Modus.

Wird über einfarbige Flächen und Buttons gelegt: Fasern, Flecken und ganz schwache
Wellen der Pappe. Transparent, damit sie auf jeder Farbe funktioniert. Alle Rauschfelder
sind periodisch (wrap), Fasern über den Rand werden gegenüber fortgesetzt → kachelbar.
"""
from pathlib import Path

import cv2
import numpy as np
from scipy import ndimage as ndi

OUT = Path(__file__).parent / "out"
OUT.mkdir(exist_ok=True)
N = 512


def noise(sigma, seed):
    r = np.random.default_rng(seed).standard_normal((N, N)).astype(np.float32)
    n = ndi.gaussian_filter(r, sigma, mode="wrap")
    return (n - n.mean()) / (n.std() + 1e-6)


x = np.arange(N, dtype=np.float32)[None, :].repeat(N, 0)
# + = dunkler, - = heller
d = 0.22 * noise(40, 1) + 0.30 * noise(9, 2) + 0.40 * noise(1.2, 3)
d += 0.45 * np.cos(2 * np.pi * x / (N / 8))           # Wellen, 8 pro Kachel
d += 0.18 * noise((0.6, 4), 4)                         # feine Faserrichtung

dark = np.clip(d, 0, None)
light = np.clip(-d, 0, None)
a = np.zeros((N, N), np.float32)
rgb = np.zeros((N, N, 3), np.float32)
brown = np.array([92, 58, 28], np.float32)
cream = np.array([255, 246, 228], np.float32)
a_dark = 0.075 * dark
a_light = 0.07 * light
a = a_dark + a_light
w = np.where(a > 0, a_dark / np.maximum(a, 1e-6), 0)[..., None]
rgb = brown * w + cream * (1 - w)


def fibers(seed, count, length, color, alpha, thick=1):
    r = np.random.default_rng(seed)
    m = np.zeros((N, N), np.uint8)
    for _ in range(count):
        x0, y0 = r.uniform(0, N), r.uniform(0, N)
        ang = r.normal(0, 0.45) + (np.pi / 2 if r.random() < 0.3 else 0)
        ln = r.uniform(*length)
        x1, y1 = x0 + np.cos(ang) * ln, y0 + np.sin(ang) * ln
        v = int(r.uniform(110, 255))
        for ox in (-N, 0, N):
            for oy in (-N, 0, N):
                cv2.line(m, (int((x0 + ox) * 4), int((y0 + oy) * 4)), (int((x1 + ox) * 4), int((y1 + oy) * 4)),
                         v, thick, cv2.LINE_AA, shift=2)
    return m.astype(np.float32) / 255 * alpha, np.array(color, np.float32)


for seed, cnt, ln, c, al in [
    (10, 260, (3, 11), (250, 236, 210), 0.55),
    (11, 300, (2, 8), (110, 70, 34), 0.35),
    (12, 420, (0.5, 1.5), (80, 50, 24), 0.55),
]:
    m, col = fibers(seed, cnt, ln, c, al)
    # über die bisherige Schicht legen (Porter-Duff "over")
    out_a = m + a * (1 - m)
    rgb = (col * m[..., None] + rgb * (a * (1 - m))[..., None]) / np.maximum(out_a, 1e-6)[..., None]
    a = out_a

img = np.dstack([np.clip(rgb, 0, 255), np.clip(a * 255, 0, 255)]).astype(np.uint8)
ok, buf = cv2.imencode(".webp", cv2.cvtColor(img, cv2.COLOR_RGBA2BGRA), [cv2.IMWRITE_WEBP_QUALITY, 82])
(OUT / "paper_tile.webp").write_bytes(buf.tobytes())
cv2.imwrite(str(OUT / "paper_tile.png"), cv2.cvtColor(img, cv2.COLOR_RGBA2BGRA))
print("paper_tile", img.shape, len(buf) // 1024, "KB")
