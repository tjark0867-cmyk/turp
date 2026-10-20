"""Cappuccino-Tasse mit Latte-Art-Herz auf Untertasse (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_torus_y, sd_torus_z, render, Studio, to_lin, to_srgb, finish, smoothstep)
from gen_kpop import sd_heart

CUP, INSIDE, COFFEE, SAUCER = 0, 1, 2, 3
Y0, Y1 = 0.12, 0.9


def r_cup(y):
    t = np.clip((y - Y0) / (Y1 - Y0), 0, 1)
    return 0.3 + 0.27 * (1 - (1 - t) ** 2.2)


def lathe(p, y0, y1, rf, k=0.7):
    q = length(p[:, [0, 2]])
    y = p[:, 1]
    return np.maximum((q - rf(np.clip(y, y0, y1))) * k, np.maximum(y0 - y, y - y1))


def scene(p):
    outer = lathe(p, Y0, Y1, r_cup) - 0.01
    inner = lathe(p, Y0 + 0.08, Y1 + 0.3, lambda y: r_cup(y) - 0.04)
    cup = np.maximum(outer, -inner)
    lip = sd_torus_y(p - np.array([0, Y1, 0]), r_cup(np.array([Y1]))[0] - 0.02, 0.022)
    hq = p - np.array([0.62, 0.55, 0])
    handle = np.maximum(sd_torus_z(hq, 0.17, 0.045), 0.0 - hq[:, 0] - 0.05)
    cupall = smin(np.minimum(cup, lip), handle, 0.03)
    coffee = np.maximum(length(p[:, [0, 2]]) - (r_cup(np.array([0.8]))[0] - 0.03), np.abs(p[:, 1] - 0.79) - 0.005)
    q = length(p[:, [0, 2]])
    dish = p[:, 1] - (0.03 + 0.05 * (q / 0.95) ** 2)
    saucer = np.maximum(np.maximum(np.abs(dish) - 0.025, q - 0.95), -p[:, 1] - 0.05) - 0.01
    D = np.stack([cupall, np.full(p.shape[0], 9.0), coffee, saucer], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.3)
    col = np.zeros((p.shape[0], 3))
    c = mat == CUP
    q = length(p[c][:, [0, 2]])
    inside = (q < r_cup(p[c, 1]) - 0.015) & (p[c, 1] > Y0 + 0.08)
    glaze = np.where(inside[:, None], to_lin((248, 246, 240)), to_lin((40, 190, 175)))
    col[c] = st.glossy(c, glaze, 0.3)
    s = mat == SAUCER
    col[s] = st.glossy(s, to_lin((40, 190, 175)), 0.3)
    k = mat == COFFEE
    x, z = p[k, 0], p[k, 2]
    rr = np.hypot(x, z) / 0.52
    crema = to_lin((196, 132, 80)) * (1 - 0.45 * smoothstep(0.7, 1.0, rr))[:, None]
    hd = sd_heart(x / 0.34, (-z + 0.25) / 0.34)
    foam = 1 - smoothstep(-0.04, 0.06, hd)
    line = (np.abs(x) < 0.01 + 0.012 * np.clip((z + 0.1) / 0.3, 0, 1)) & (z > -0.12) & (z < 0.33)
    foam = np.maximum(foam, line * 0.8)
    colk = crema * (1 - foam[:, None]) + to_lin((250, 240, 225)) * foam[:, None]
    col[k] = st.plastic(k, colk, spec=0.25, power=30)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.85, -1), extent=1.05, center=(0.05, 0.4, 0), k=0.7)
    finish(im).save(sys.argv[1])
    print("ok")
