"""Schwarz lackiertes Klavier mit Tasten (selbst erzeugt)."""
import sys
import numpy as np
from rm import (rot_x, nz, length, smin, sd_round_box, rot_y, render, Studio, to_lin, to_srgb, finish, smoothstep)

LACQ, WHITE, BLACK, GOLD, FELT, SHEET = 0, 1, 2, 3, 4, 5
X0, KW = -0.95, 1.9 / 22
BLACK_AFTER = {0, 1, 3, 4, 5}


def local(p):
    return rot_y(p, 0.55)


def scene(p):
    q = local(p)
    cab = sd_round_box(q - np.array([0, 1.25, -0.12]), (1.02, 0.72, 0.26), 0.03)
    lid = sd_round_box(q - np.array([0, 1.99, -0.1]), (1.06, 0.03, 0.3), 0.015)
    shelf = sd_round_box(q - np.array([0, 0.78, 0.2]), (1.02, 0.06, 0.32), 0.02)
    lower = sd_round_box(q - np.array([0, 0.38, -0.12]), (0.98, 0.38, 0.24), 0.02)
    legs = np.minimum(sd_round_box(q - np.array([-0.94, 0.38, 0.42]), (0.06, 0.38, 0.06), 0.02),
                      sd_round_box(q - np.array([0.94, 0.38, 0.42]), (0.06, 0.38, 0.06), 0.02))
    cheeks = np.minimum(sd_round_box(q - np.array([-1.0, 0.93, 0.25]), (0.04, 0.1, 0.27), 0.02),
                        sd_round_box(q - np.array([1.0, 0.93, 0.25]), (0.04, 0.1, 0.27), 0.02))
    fall = sd_round_box(q - np.array([0, 1.02, 0.08]), (0.96, 0.1, 0.02), 0.01)
    lacq = np.minimum.reduce([cab, lid, shelf, lower, legs, cheeks, fall])
    whites = sd_round_box(q - np.array([0, 0.875, 0.32]), (0.955, 0.035, 0.2), 0.008)
    b = np.round((q[:, 0] - X0) / KW)
    j = (b - 1) % 7
    exists = np.isin(j, list(BLACK_AFTER)) & (b >= 1) & (b <= 21)
    xb = X0 + b * KW
    bk = sd_round_box(np.stack([q[:, 0] - xb, q[:, 1] - 0.925, q[:, 2] - 0.24], -1), (0.022, 0.03, 0.12), 0.006)
    blacks = np.where(exists, bk, 1.0)
    pedals = np.minimum.reduce([sd_round_box(q - np.array([dx, 0.06, 0.2]), (0.04, 0.012, 0.09), 0.01) for dx in (-0.12, 0, 0.12)])
    felt = sd_round_box(q - np.array([0, 0.9, 0.105]), (0.955, 0.012, 0.012), 0.004)
    sq = rot_x(q - np.array([0, 1.28, 0.2]), 0.28)
    sheet = sd_round_box(sq, (0.34, 0.24, 0.006), 0.004)
    D = np.stack([lacq, whites, blacks, pedals, felt, sheet], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.35)
    col = np.zeros((p.shape[0], 3))
    l = mat == LACQ
    fr_ = 0.04 + 0.96 * (1 - st.ndv[l]) ** 5
    col[l] = st.plastic(l, to_lin((10, 10, 13)), spec=1.4, power=140, fres=0.0) + (st.env(l) * (0.03 + 0.55 * fr_))[:, None]
    # Zierrahmen auf der Front
    qq = local(p[l])
    front = local(st.n[l])[:, 2] > 0.9
    frame = front & (qq[:, 1] > 1.05) & (np.abs(np.maximum(np.abs(qq[:, 0]) / 0.86, np.abs(qq[:, 1] - 1.42) / 0.42) - 1) < 0.012)
    col[np.where(l)[0][frame]] += to_lin((90, 90, 100))
    w = mat == WHITE
    q = local(p[w])
    fr = ((q[:, 0] - X0) / KW) % 1
    gap = (fr < 0.05) | (fr > 0.95)
    wc = np.tile(to_lin((248, 246, 240)), (w.sum(), 1))
    wc[gap] = to_lin((150, 150, 150))
    col[w] = st.plastic(w, wc, spec=0.5, power=60)
    b = mat == BLACK
    col[b] = st.glossy(b, to_lin((12, 12, 14)), 0.4)
    g = mat == GOLD
    col[g] = st.metal(g, to_lin((255, 200, 90)), 1.0)
    f = mat == FELT
    col[f] = st.plastic(f, to_lin((160, 30, 40)), spec=0.05)
    sh_ = mat == SHEET
    sq = rot_x(local(p[sh_]) - np.array([0, 1.28, 0.2]), 0.28)
    paper = np.tile(to_lin((250, 248, 240)), (sh_.sum(), 1))
    yy = (sq[:, 1] + 0.2) / 0.11
    staff = (np.abs(((sq[:, 1] + 0.2) / 0.022) % 1 - 0.5) < 0.12) & ((yy % 1) < 0.95 * 0.55) & (np.abs(sq[:, 0]) < 0.3) & (np.abs(sq[:, 1]) < 0.2)
    paper[staff] = to_lin((60, 60, 60))
    nx = (sq[:, 0] + 0.3) / 0.075
    notes = (np.hypot((nx % 1 - 0.5) * 0.075 / 0.014, ((sq[:, 1] + 0.2 - (np.floor(nx) % 4) * 0.011) % 0.11 - 0.03) / 0.011) < 1) & (np.abs(sq[:, 0]) < 0.28) & (np.abs(sq[:, 1]) < 0.19)
    paper[notes] = to_lin((20, 20, 20))
    col[sh_] = st.plastic(sh_, paper, spec=0.05)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.3, -1), extent=1.25, center=(0.05, 1.0, 0), k=0.8)
    finish(im).save(sys.argv[1])
    print("ok")
