"""Handmikrofon mit Metallkorb und schwebenden Noten (selbst erzeugt)."""
import sys
import numpy as np
from PIL import Image, ImageDraw
from rm import nz, length, smin, sd_cyl_y, sd_torus_y, render, Studio, to_lin, to_srgb, finish, smoothstep

HEAD, RING, BODY, SWITCH = 0, 1, 2, 3
HC, HR = np.array([0, 1.62, 0]), 0.44


def scene(p):
    y = p[:, 1]
    head = np.maximum(length(p - HC) - HR, 1.24 - y)
    ring = smin(sd_cyl_y(p, 1.1, 1.32, 0.31) - 0.015, sd_torus_y(p - np.array([0, 1.3, 0]), 0.31, 0.03), 0.02)
    r = 0.16 + 0.12 * np.clip((y + 0.45) / 1.6, 0, 1) ** 1.3
    body = np.maximum(length(p[:, [0, 2]]) - r, np.maximum(-0.45 - y, y - 1.12)) - 0.02
    body = smin(body, length(p - np.array([0, -0.45, 0])) - 0.17, 0.08)
    switch = length((p - np.array([0, 0.62, 0.235])) * np.array([1.6, 0.55, 2.2])) - 0.07
    D = np.stack([head, ring, body, switch], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.35)
    col = np.zeros((p.shape[0], 3))
    h = mat == HEAD
    q = p[h] - HC
    th = np.arctan2(q[:, 0], q[:, 2])
    a, b = th * 0.44 * 34, q[:, 1] * 34
    holes = (np.sin(a + b) * np.sin(a - b)) > 0.25
    metal = st.metal(h, to_lin((205, 208, 214)), 0.9)
    dark = to_lin((18, 18, 22)) * (0.3 + 0.5 * st.dif[h])[:, None]
    col[h] = np.where(holes[:, None], dark * 0.6 + metal * 0.12, metal)
    col[mat == RING] = st.metal(mat == RING, to_lin((220, 222, 228)))
    b_ = mat == BODY
    col[b_] = st.plastic(b_, to_lin((26, 26, 30)), spec=0.5, power=40, fres=0.2) + st.env(b_, 0.06)[:, None]
    s = mat == SWITCH
    col[s] = st.plastic(s, to_lin((40, 210, 190)), spec=0.6)
    return to_srgb(col)


def note(d, x, y, s, col, double=False):
    def head(cx, cy):
        d.ellipse((cx - 0.55 * s, cy - 0.38 * s, cx + 0.55 * s, cy + 0.38 * s), fill=col)
    head(x, y)
    d.rectangle((x + 0.42 * s, y - 2.3 * s, x + 0.58 * s, y), fill=col)
    if double:
        head(x + 1.6 * s, y - 0.4 * s)
        d.rectangle((x + 2.02 * s, y - 2.7 * s, x + 2.18 * s, y - 0.4 * s), fill=col)
        d.polygon([(x + 0.42 * s, y - 2.3 * s), (x + 2.18 * s, y - 2.7 * s), (x + 2.18 * s, y - 2.25 * s), (x + 0.42 * s, y - 1.85 * s)], fill=col)
    else:
        d.polygon([(x + 0.58 * s, y - 2.3 * s), (x + 1.3 * s, y - 1.5 * s), (x + 1.2 * s, y - 1.2 * s), (x + 0.58 * s, y - 1.8 * s)], fill=col)


if __name__ == "__main__":
    mic = render(scene, shade, 640, view_dir=(0.2, -0.25, -1), extent=1.32, center=(0, 0.82, 0), k=0.8)
    mic = mic.rotate(-28, resample=Image.BICUBIC, expand=True)
    S = 1000
    can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    can.alpha_composite(mic, (330, 260))
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    note(d, 370, 420, 56, (40, 200, 185, 255), double=True)
    note(d, 300, 600, 42, (255, 110, 165, 255))
    note(d, 520, 250, 38, (255, 190, 50, 255))
    can.alpha_composite(layer)
    finish(can).save(sys.argv[1])
    print("ok")
