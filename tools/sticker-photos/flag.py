"""Wehende Regenbogenflagge (selbst erzeugt)."""
import sys
import numpy as np
from PIL import Image

W, H = 1200, 820
cols = np.array([[228, 3, 3], [255, 140, 0], [255, 237, 0], [0, 128, 38], [36, 64, 142], [115, 41, 130]], float)
y, x = np.mgrid[0:H, 0:W].astype(float)
u = x / W
amp = 34
wave = amp * np.sin(u * 2 * np.pi * 1.35 + 0.4) * (0.35 + 0.65 * u)
v = (y - 90 - wave) / (H - 180)          # Position im Stoff (0..1)
inside = (v >= 0) & (v <= 1)
idx = np.clip((v * 6).astype(int), 0, 5)
col = cols[idx]
slope = np.gradient(wave, axis=1)
shade = 1 + slope * 0.035 + 0.05 * np.cos(u * 2 * np.pi * 1.35 + 0.4)
img = np.clip(col * shade[..., None], 0, 255)
# leichte Stoffstruktur
rng = np.random.default_rng(3)
img = np.clip(img * (1 + rng.normal(0, 0.025, (H, W)))[..., None], 0, 255)
alpha = (inside * 255).astype(np.uint8)
im = Image.fromarray(np.dstack([img.astype(np.uint8), alpha]), "RGBA").crop((0, 40, W, H - 40))
im = im.resize((800, int(800 * im.height / im.width)), Image.LANCZOS)
im.save(sys.argv[1])
print("ok")
