"""Haarreif mit Katzenohren (Samt, rosa Innenohr, Glitzersteine) – eigenes Design."""
import sys
import numpy as np
from PIL import Image, ImageDraw
from rm import (nz, length, smin, sd_box2, sd_tri2, rot_x, rot_y, rot_z, render, Studio, to_lin, to_srgb, finish)

BAND, EAR, INNER, GEM = 0, 1, 2, 3
R = 1.0
EARS = [np.radians(118), np.radians(62)]


def local(p):
    return rot_x(rot_y(p, 0.28), 0.25)


def ear_frame(q, th):
    """Koordinaten im Ohr-System: u tangential, v radial nach außen, w = z"""
    c = np.array([R * np.cos(th), R * np.sin(th)])
    rx, ry = np.cos(th), np.sin(th)
    tx, ty = -np.sin(th), np.cos(th)
    dx, dy = q[:, 0] - c[0], q[:, 1] - c[1]
    return dx * tx + dy * ty, dx * rx + dy * ry, q[:, 2]


def scene(p):
    q = local(p)
    rr = np.hypot(q[:, 0], q[:, 1]) - R
    band = sd_box2(rr, q[:, 2], 0.025, 0.07) - 0.025
    ang = np.arctan2(q[:, 1], q[:, 0])
    band = np.maximum(band, -(q[:, 1] + 0.35))
    ears, inners = [], []
    for th in EARS:
        u, v, w = ear_frame(q, th)
        t2 = sd_tri2(u, v, (-0.38, -0.04), (0.38, -0.04), (0.04, 0.64))
        ears.append(np.hypot(np.maximum(t2 + 0.07, 0), np.maximum(np.abs(w) - 0.02, 0)) + np.minimum(np.maximum(t2 + 0.07, np.abs(w) - 0.02), 0) - 0.07)
        ti = sd_tri2(u, v, (-0.17, 0.1), (0.17, 0.1), (0.03, 0.42))
        inners.append(np.maximum(ti + 0.02, np.abs(w - 0.08) - 0.012) - 0.03)
    ear = np.minimum(ears[0], ears[1])
    inner = np.minimum(inners[0], inners[1])
    gems = []
    for a in np.radians(np.arange(25, 160, 15)):
        c = np.array([R * np.cos(a), R * np.sin(a), 0.1])
        gems.append(length(q - c) - 0.03)
    gem = np.min(np.stack(gems, -1), -1)
    D = np.stack([smin(band, ear, 0.03), np.full(p.shape[0], 9.0), inner, gem], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.3)
    col = np.zeros((p.shape[0], 3))
    b = mat == BAND
    # Samt: matt, helle Ränder (Velvet-Effekt)
    rim = (1 - st.ndv[b]) ** 2.5
    col[b] = to_lin((34, 30, 40)) * (0.35 + 0.8 * st.dif[b])[:, None] + to_lin((120, 100, 150)) * rim[:, None] * 0.6
    i = mat == INNER
    col[i] = st.plastic(i, to_lin((255, 150, 200)), spec=0.6, power=40) * 1.25
    g = mat == GEM
    sparkle = (np.sin(p[g, 0] * 300) * np.sin(p[g, 1] * 300) > 0.2)
    col[g] = st.metal(g, to_lin((230, 230, 255)), 1.4) * (1 + sparkle * 0.8)[:, None]
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0, -0.05, -1), extent=1.2, center=(0, 0.55, 0), k=0.7)
    # kleines Funkeln
    can = Image.new("RGBA", (im.width + 160, im.height + 160), (0, 0, 0, 0))
    can.alpha_composite(im, (80, 80))
    d = ImageDraw.Draw(can)
    for (x, y, r) in ((can.width * 0.5, 70, 34), (can.width - 90, can.height * 0.45, 26), (90, can.height * 0.5, 22)):
        d.polygon([(x, y - r), (x + r * 0.28, y - r * 0.28), (x + r, y), (x + r * 0.28, y + r * 0.28), (x, y + r),
                   (x - r * 0.28, y + r * 0.28), (x - r, y), (x - r * 0.28, y - r * 0.28)], fill=(220, 190, 255, 255))
    finish(can).save(sys.argv[1])
    print("ok")
