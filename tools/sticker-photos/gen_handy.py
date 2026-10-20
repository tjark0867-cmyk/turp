"""Smartphone quer mit buntem Match-3-Spiel auf dem Bildschirm (eigenes Design)."""
import sys
import numpy as np
from gen_trophy import sd_star5
from rm import (nz, length, sd_slab, sd_box2, rot_x, rot_y, rot_z, render, Studio, to_lin, to_srgb, finish, smoothstep)

BODY, SCREEN = 0, 1
W, H, T = 1.0, 0.48, 0.045


def local(p):
    return rot_z(rot_x(rot_y(p, 0.42), -0.32), 0.12)


def scene(p):
    q = local(p)
    body = sd_slab(q, W, H, T, 0.13, 0.03)
    screen = sd_slab(q - np.array([0, 0, 0.012]), W - 0.035, H - 0.035, T - 0.0, 0.1, 0.004)
    screen = np.maximum(screen, 0.003 - (q[:, 2]) )  # nur Vorderseite
    D = np.stack([body, np.maximum(screen, -body - 0.0) * 0 + screen], -1)
    return D.min(-1), D.argmin(-1)


GEMS = [(255, 70, 90), (60, 170, 255), (255, 200, 40), (80, 220, 120), (190, 90, 255), (255, 140, 40)]


def screen_tex(u, v):
    """u: -1..1 Breite, v: -1..1 Höhe."""
    t = (v + 1) / 2
    col = to_lin((52, 24, 100)) * (1 - t[:, None]) + to_lin((110, 50, 180)) * t[:, None]
    ax = u * W / H  # quadratische Einheiten
    # Spielfeld 7x4
    cols, rows = 7, 4
    cell = 0.42
    gx = (ax + cols * cell / 2) / cell
    gy = (v * 1.0 + 0.82 + 0.1) / cell * 1.0
    gyy = (v + 0.86) / (cell * 0.95 / 1.0) 
    ix, iy = np.floor(gx), np.floor((v + 0.9) / 0.4)
    inside = (ix >= 0) & (ix < cols) & (iy >= 0) & (iy < rows)
    fx = gx - ix - 0.5
    fy = (v + 0.9) / 0.4 - iy - 0.5
    board = (np.abs(ax) < cols * cell / 2 + 0.04) & (v > -0.94) & (v < 0.74)
    col[board] = col[board] * 0.55 + to_lin((30, 12, 60)) * 0.45
    rnd = ((np.sin(ix * 12.99 + iy * 78.23) * 43758.5) % 1)
    kind = np.floor(rnd * len(GEMS)).astype(int) % len(GEMS)
    shape = (kind % 3)
    r = np.hypot(fx, fy)
    gem = np.where(shape == 0, r < 0.36, np.where(shape == 1, np.abs(fx) + np.abs(fy) < 0.42, np.maximum(np.abs(fx), np.abs(fy)) < 0.3))
    g = inside & gem
    gc = np.array([to_lin(c) for c in GEMS])[kind[g]]
    shine = (1.25 - 0.9 * np.clip(fy[g] + 0.1, -0.5, 0.5))[:, None]
    hl = (np.hypot(fx[g] + 0.12, fy[g] - 0.14) < 0.1)[:, None]
    col[g] = gc * shine * 1.15 + hl * 0.9
    # Kopfzeile: drei Sterne + Punkte-Balken
    for i in range(3):
        sx = -0.32 + i * 0.32
        star = sd_star5(ax - sx, v - 0.85, 0.12, 0.45) < 0
        col[star] = to_lin((255, 210, 60)) * 1.3
    bar = (np.abs(v - 0.86) < 0.045) & (ax > 0.75) & (ax < 1.75)
    col[bar] = to_lin((255, 255, 255)) * 0.9
    fill = bar & (ax < 1.35)
    col[fill] = to_lin((40, 230, 200)) * 1.2
    return col


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.3)
    col = np.zeros((p.shape[0], 3))
    b = mat == BODY
    col[b] = st.metal(b, to_lin((60, 62, 70)), 0.7) + st.plastic(b, to_lin((20, 20, 24)), spec=0.4)
    s = mat == SCREEN
    q = local(p[s])
    u, v = q[:, 0] / (W - 0.035), q[:, 1] / (H - 0.035)
    tex = screen_tex(u, v)
    edge = (np.abs(u) > 0.985) | (np.abs(v) > 0.97)
    tex[edge] = to_lin((5, 5, 8))
    cam = np.hypot((u + 0.93) * W, v * H) < 0.022
    tex[cam] = to_lin((2, 2, 4))
    f_ = 0.04 + 0.96 * (1 - st.ndv[s]) ** 5
    col[s] = tex * 1.2 + (st.env(s) * (0.03 + 0.35 * f_))[:, None]
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0, -0.1, -1), extent=1.1, center=(0, 0, 0), k=0.8)
    finish(im).save(sys.argv[1])
    print("ok")
