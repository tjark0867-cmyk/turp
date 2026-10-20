"""Futuristischer Energieschild aus leuchtenden Sechsecken (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageFilter
from rm import finish, glow_layer

S = 1200
C = S / 2
yy, xx = np.mgrid[0:S, 0:S].astype(float)
x, y = xx - C, yy - C
r = np.hypot(x, y)
R = 470
# Wölbung: Punkte zur Mitte hin "näher" -> Waben leicht vergrößert
k = 1 / (1 + 0.35 * (1 - (r / R) ** 2).clip(0, 1))
u, v = x * k, y * k
# Hex-Gitter (spitz oben), Zellgröße s
s = 52


def hexdist(u, v):
    q = (np.sqrt(3) / 3 * u - v / 3) / s
    rr = (2 / 3 * v) / s
    cx, cz = q, rr
    cy = -cx - cz
    rx, ry, rz = np.round(cx), np.round(cy), np.round(cz)
    dx, dy, dz = np.abs(rx - cx), np.abs(ry - cy), np.abs(rz - cz)
    fix_x = (dx > dy) & (dx > dz)
    fix_y = ~fix_x & (dy > dz)
    rx = np.where(fix_x, -ry - rz, rx)
    ry = np.where(fix_y, -rx - rz, ry)
    rz = np.where(~fix_x & ~fix_y, -rx - ry, rz)
    # Zentrum der Zelle
    hx = s * np.sqrt(3) * (rx + rz / 2)
    hy = s * 1.5 * rz
    px, py = np.abs(u - hx), np.abs(v - hy)
    d = np.maximum(px, px * 0.5 + py * np.sqrt(3) / 2) / (np.sqrt(3) / 2 * s)  # 0 Mitte, 1 Rand
    return d, rx, rz


d, cxi, czi = hexdist(u, v)
edge = np.clip((d - 0.84) / 0.08, 0, 1)
rnd = (np.sin(cxi * 12.9898 + czi * 78.233) * 43758.5453) % 1
inside = r < R
fill_a = (0.28 + 0.25 * rnd + 0.35 * (r / R) ** 3) * inside
line_a = edge * inside
rim = np.clip(1 - np.abs(r - R) / 18, 0, 1)
col = np.zeros((S, S, 4))
c_fill = np.array([70, 170, 255]); c_line = np.array([180, 240, 255]); c_rim = np.array([230, 250, 255])
a = np.clip(fill_a * (1 - line_a) + line_a * 0.95, 0, 1)
rgb = c_fill * (1 - line_a[..., None]) + c_line * line_a[..., None]
rgb = rgb * (1 - rim[..., None]) + c_rim * rim[..., None]
a = np.maximum(a, rim)
# Glanz oben links
gl = np.clip(1 - np.hypot(x + 180, y + 200) / 260, 0, 1) ** 2 * inside
rgb = rgb + 255 * gl[..., None] * 0.35
col[..., :3] = np.clip(rgb, 0, 255)
col[..., 3] = np.clip(a + gl * 0.3, 0, 1) * 255
im = Image.fromarray(col.astype(np.uint8), "RGBA")
# leichte Perspektive
im = im.transform((S, S), Image.AFFINE, (1.08, 0.08, -70, 0.0, 1.0, 0), resample=Image.BICUBIC)
can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
can.alpha_composite(glow_layer(im.getchannel("A"), (80, 180, 255), 30, 0.9))
can.alpha_composite(im)
# Griff-Andeutung entfällt; Zentrum: leuchtender Kern-Ring
finish(can).save(sys.argv[1])
print("ok")
