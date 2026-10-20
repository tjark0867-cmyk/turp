"""Leuchtender Zaubertrank im Rundkolben mit Korken (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smin, smoothstep, sd_torus_y, render, ambient_occlusion, to_lin, to_srgb)

RB, CY = 0.62, 0.62          # Kolbenkugel
LEVEL = 0.82                 # Flüssigkeitsstand
GLASS, CORK = 0, 1


def sd_cyl(p, y0, y1, r):
    q = length(p[:, [0, 2]]) - r
    return np.maximum(q, np.maximum(y0 - p[:, 1], p[:, 1] - y1))


def scene(p):
    body = length(p - np.array([0, CY, 0])) - RB
    neck = sd_cyl(p, 1.0, 1.62, 0.17)
    lip = sd_torus_y(p - np.array([0, 1.6, 0]), 0.185, 0.035)
    glass = smin(body, neck, 0.09)
    glass = np.minimum(glass, lip)
    t = np.clip((p[:, 1] - 1.52) / 0.32, 0, 1)
    cork = np.maximum(length(p[:, [0, 2]]) - (0.16 + 0.03 * t), np.maximum(1.52 - p[:, 1], p[:, 1] - 1.86)) - 0.012
    D = np.stack([glass, cork], -1)
    return D.min(-1), D.argmin(-1)


def in_liquid(q):
    return (length(q - np.array([0, CY, 0])) < RB - 0.03) & (q[:, 1] < LEVEL)


def shade(p, n, mat, d, right, upv):
    V = -d
    L = nz(-0.55 * right + 0.8 * upv + 0.45 * V)
    col = np.zeros((p.shape[0], 3))
    g = mat == GLASS
    # Dicke der Flüssigkeit entlang des Sehstrahls
    steps = 40
    thick = np.zeros(g.sum())
    pg = p[g]
    for i in range(steps):
        q = pg + d * (i + 0.5) * (2.4 * RB / steps)
        thick += in_liquid(q) * (2.4 * RB / steps)
    liq = 1 - np.exp(-thick * 9)
    # leuchtende Flüssigkeit: innen hell, außen satt
    core = smoothstep(0.2, 1.1, thick)
    lc = to_lin((80, 8, 150)) * (1 - core[:, None]) + to_lin((235, 80, 255)) * core[:, None]
    lc *= 1.1
    # Oberfläche der Flüssigkeit sichtbar machen (heller Meniskus)
    surf = np.zeros(g.sum())
    for i in range(steps):
        q = pg + d * (i + 0.5) * (2.4 * RB / steps)
        surf = np.maximum(surf, ((np.abs(q[:, 1] - LEVEL) < 0.012) & (length(q - np.array([0, CY, 0])) < RB - 0.03)).astype(float))
    glass_c = to_lin((232, 240, 246))
    ng = n[g]
    fres = 0.06 + 0.94 * (1 - np.clip((ng * V).sum(-1), 0, 1)) ** 4
    R = d - 2 * (d * ng).sum(-1, keepdims=True) * ng
    rx = (R * right).sum(-1)
    ry = (R * upv).sum(-1)
    # Fenster-Spiegelung (weiches Rechteck) oben links + schmale Kante rechts
    env = (smoothstep(-0.75, -0.6, rx) * (1 - smoothstep(-0.38, -0.25, rx)) *
           smoothstep(0.25, 0.4, ry) * (1 - smoothstep(0.75, 0.85, ry)) * 1.6 +
           smoothstep(0.62, 0.7, rx) * (1 - smoothstep(0.74, 0.8, rx)) * smoothstep(0.1, 0.3, ry) * 0.7)
    H = nz(L + V)
    spec = np.clip((ng * H).sum(-1), 0, 1) ** 120 * 3
    base = glass_c * (1 - liq[:, None]) * (0.75 + 0.25 * (ng[:, 1] * 0.5 + 0.5))[:, None] + lc * liq[:, None]
    base += to_lin((255, 220, 255)) * surf[:, None] * 0.6
    dark_edge = smoothstep(0.55, 0.95, fres) * 0.55
    base = base * (1 - dark_edge[:, None]) + to_lin((120, 130, 150)) * dark_edge[:, None] * (1 - liq[:, None])
    col[g] = base + ((env * 0.8 + spec) * (1 - 0.35 * liq))[:, None] + (fres * 0.25 * (1 - 0.7 * liq))[:, None]
    c = mat == CORK
    ao = ambient_occlusion(scene, p[c], n[c], 0.3)
    x, y, z = p[c, 0], p[c, 1], p[c, 2]
    nse = np.sin(x * 173 + y * 91) * np.sin(y * 157 - z * 113) * np.sin(z * 131 + x * 71)
    grain = 0.9 + 0.12 * nse - 0.35 * (nse > 0.55)
    rng = np.random.default_rng(1)
    speck = rng.uniform(0.85, 1.1, c.sum())
    dif = np.clip((n[c] * L).sum(-1), 0, 1)
    col[c] = to_lin((176, 128, 82)) * (grain * speck * (0.3 + 1.0 * dif) * ao)[:, None]
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.15, -0.3, -1), extent=1.0, center=(0, 0.92, 0), k=0.8)
    im.save(sys.argv[1])
    print("ok", im.size)


def magic(path):
    """Leuchten und Bläschen in der Flüssigkeit (2D-Nachbearbeitung)."""
    from PIL import Image, ImageDraw, ImageFilter, ImageChops
    im = Image.open(path).convert("RGBA")
    a = np.array(im).astype(float)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    liquid = (r > 90) & (b > 140) & (g < 120) & (a[..., 3] > 200)
    ys, xs = np.where(liquid)
    cx, cy = xs.mean(), ys.mean() + (ys.max() - ys.min()) * 0.12
    w, h = im.size
    yy, xx = np.mgrid[0:h, 0:w]
    rr = np.hypot((xx - cx) / (xs.max() - xs.min()), (yy - cy) / (ys.max() - ys.min()))
    glow = np.clip(1 - rr / 0.45, 0, 1) ** 1.6 * liquid
    a[..., 0] = np.clip(r + glow * 70, 0, 255)
    a[..., 1] = np.clip(g + glow * 90, 0, 255)
    a[..., 2] = np.clip(b + glow * 40, 0, 255)
    out = Image.fromarray(a.astype(np.uint8), "RGBA")
    rng = np.random.default_rng(5)
    bub = Image.new("RGBA", out.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(bub)
    for _ in range(9):
        i = rng.integers(len(xs))
        x, y = xs[i], ys[i]
        if y < ys.min() + 25:
            continue
        rad = rng.uniform(5, 15)
        d.ellipse((x - rad, y - rad, x + rad, y + rad), outline=(255, 225, 255, 200), width=2)
        d.ellipse((x - rad * 0.5, y - rad * 0.6, x - rad * 0.1, y - rad * 0.2), fill=(255, 255, 255, 220))
    m = Image.fromarray((liquid * 255).astype(np.uint8), "L").filter(ImageFilter.MinFilter(9))
    bub.putalpha(ImageChops.multiply(bub.getchannel("A"), m))
    out.alpha_composite(bub)
    out.save(path)


if __name__ == "__main__":
    magic(sys.argv[1])
