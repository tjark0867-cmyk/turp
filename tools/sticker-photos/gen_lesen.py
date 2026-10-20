"""Aufgeschlagenes Buch mit gewölbten Seiten und umblätternder Seite (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_round_box, render, Studio, to_lin, to_srgb, finish, smoothstep, rot_y)

PAGES, COVER, TURN, RIBBON = 0, 1, 2, 3
W, D = 1.0, 0.68


def f(x):
    ax = np.abs(x)
    return 0.07 + 0.2 * (1 - np.exp(-ax * 7)) * (1 - 0.45 * (ax / W) ** 2)


def g(x):
    """umblätternde Seite: von der Mitte nach oben gebogen"""
    t = np.clip(x / 0.85, 0, 1)
    return f(0.02) + 0.04 + np.sin(t * np.pi * 0.62) * 0.62 * t ** 0.8


def local(p):
    return rot_y(p, 0.18)


def scene(p):
    q = local(p)
    x, y, z = q[:, 0], q[:, 1], q[:, 2]
    pages = np.maximum((y - f(x)) * 0.7, np.maximum(-y + 0.0, np.maximum(np.abs(x) - W, np.abs(z) - D)))
    cover = sd_round_box(q - np.array([0, -0.015, 0]), (W + 0.05, 0.02, D + 0.04), 0.012)
    xt = np.clip(x, 0.03, 0.85)
    turn = np.maximum((np.abs(y - g(xt)) - 0.006) * 0.45, np.maximum(np.abs(z) - (D - 0.01), np.maximum(0.03 - x, x - 0.85)))
    ribbon = sd_round_box(q - np.array([0.02, 0.0, D + 0.12]), (0.035, 0.012, 0.16), 0.008)
    D_ = np.stack([pages, cover, turn, ribbon], -1)
    return D_.min(-1), D_.argmin(-1)


def text_lines(x, z, side):
    """graue Textzeilen auf der Seite"""
    u = np.abs(x)
    in_margin = (u > 0.12) & (u < W - 0.1) & (np.abs(z) < D - 0.1)
    row = (z + D) / 0.055
    fr = row % 1
    ri = np.floor(row)
    rnd = (np.sin(ri * 91.7 + side * 13.1) * 43758.5) % 1
    end = W - 0.1 - rnd * 0.35 * (rnd > 0.7)
    words = (np.sin(u * 46 + ri * 3.1) > -0.75)
    return in_margin & (fr > 0.35) & (fr < 0.62) & (u < end) & words


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.4)
    col = np.zeros((p.shape[0], 3))
    pg = mat == PAGES
    q = local(p[pg])
    ln = local(st.n[pg])
    paper = np.tile(to_lin((250, 246, 236)), (pg.sum(), 1))
    top = ln[:, 1] > 0.45
    tl = top & text_lines(q[:, 0], q[:, 2], np.sign(q[:, 0]))
    paper[tl] = to_lin((120, 116, 110))
    side = ~top
    paper[side] *= (0.82 + 0.18 * ((q[side, 1] * 160) % 1 > 0.5))[:, None]
    col[pg] = st.plastic(pg, paper, spec=0.05, power=10)
    c = mat == COVER
    col[c] = st.plastic(c, to_lin((40, 170, 160)), spec=0.3, power=30)
    t = mat == TURN
    qt = local(p[t])
    paper2 = np.tile(to_lin((252, 249, 240)), (t.sum(), 1))
    tl2 = text_lines(qt[:, 0] * 1.0, qt[:, 2], 3) & (local(st.n[t])[:, 1] > 0)
    paper2[tl2] = to_lin((130, 126, 120))
    col[t] = st.plastic(t, paper2, spec=0.05, power=10) + 0.03
    r = mat == RIBBON
    col[r] = st.plastic(r, to_lin((230, 60, 90)), spec=0.4)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.95, -1), extent=1.22, center=(0.02, 0.32, 0.0), k=0.8)
    finish(im).save(sys.argv[1])
    print("ok")
