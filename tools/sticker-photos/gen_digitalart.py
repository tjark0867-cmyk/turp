"""Zeichen-Tablet mit Stift und halbfertigem bunten Bild (eigenes Design)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_slab, sd_capsule, rot_x, rot_y, rot_z, render, Studio, to_lin, to_srgb, finish, smoothstep)

BODY, SCREEN, PEN, TIP = 0, 1, 2, 3
W, H, T = 1.1, 0.8, 0.035
P0, P1 = np.array([0.15, -0.55, 0.16]), np.array([1.2, 0.55, 0.32])


def local(p):
    return rot_z(rot_x(rot_y(p, 0.3), -0.55), 0.05)


def scene(p):
    q = local(p)
    body = sd_slab(q, W, H, T, 0.12, 0.025)
    screen = np.maximum(sd_slab(q - np.array([0, 0, 0.01]), W - 0.05, H - 0.05, T, 0.08, 0.003), 0.004 - q[:, 2])
    pen = sd_capsule(p, P0 + (P1 - P0) * 0.08, P1, 0.042)
    tip = sd_capsule(p, P0, P0 + (P1 - P0) * 0.08, 0.022)
    D = np.stack([body, screen, pen, tip], -1)
    return D.min(-1), D.argmin(-1)


def painting(u, v):
    """Bunter Pinselstrich-Sonnenuntergang mit Werkzeugleiste."""
    t = (v + 1) / 2
    col = to_lin((255, 170, 120)) * (1 - t[:, None]) + to_lin((120, 110, 240)) * t[:, None]
    # Pinselstriche (wellig)
    for i, (c, y0) in enumerate([((255, 110, 140), -0.15), ((255, 200, 90), -0.35), ((60, 200, 190), -0.6), ((40, 110, 170), -0.85)]):
        band = v < y0 + 0.08 * np.sin(u * 4 + i * 1.7) + 0.03 * np.sin(u * 17 + i)
        col[band] = to_lin(c)
    sun = np.hypot((u - 0.2) * 1.37, v - 0.2) < 0.26
    col[sun] = to_lin((255, 240, 180))
    # unfertiger Teil rechts: weiße Leinwand mit Skizzenlinie
    blank = u > 0.55 + 0.06 * np.sin(v * 9)
    col[blank] = to_lin((246, 244, 240))
    sketch = blank & (np.abs(v - (-0.2 + 0.25 * np.sin(u * 5))) < 0.012)
    col[sketch] = to_lin((120, 120, 130))
    # Werkzeugleiste links
    bar = u < -0.86
    col[bar] = to_lin((40, 40, 48))
    for k, c in enumerate([(255, 80, 100), (255, 200, 60), (60, 210, 190), (90, 140, 255), (250, 250, 250)]):
        dot = np.hypot((u + 0.93) * 1.37, v - (0.6 - k * 0.28)) < 0.07
        col[dot] = to_lin(c)
    return col


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.3)
    col = np.zeros((p.shape[0], 3))
    b = mat == BODY
    col[b] = st.metal(b, to_lin((180, 184, 192)), 0.8)
    s = mat == SCREEN
    q = local(p[s])
    u, v = q[:, 0] / (W - 0.05), q[:, 1] / (H - 0.05)
    tex = painting(u, v)
    tex[(np.abs(u) > 0.975) | (np.abs(v) > 0.965)] = to_lin((8, 8, 10))
    f_ = 0.04 + 0.96 * (1 - st.ndv[s]) ** 5
    col[s] = tex * 1.1 + (st.env(s) * (0.02 + 0.3 * f_))[:, None]
    pn = mat == PEN
    col[pn] = st.plastic(pn, to_lin((245, 245, 248)), spec=0.8, power=80)
    tp = mat == TIP
    col[tp] = st.plastic(tp, to_lin((60, 60, 66)), spec=0.5)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0, -0.15, -1), extent=1.35, center=(0.1, 0, 0), k=0.8)
    finish(im).save(sys.argv[1])
    print("ok")
