"""Elegante Henkeltasche aus Leder mit Goldschließe (eigenes Design, ohne Marke)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_round_box, sd_torus_z, sd_capsule, rot_y, render, Studio, to_lin, to_srgb, finish, smoothstep)

LEATHER, FLAP, GOLD, HANDLE = 0, 1, 2, 3
BW, BH, BD = 0.85, 0.62, 0.32


def local(p):
    return rot_y(p, -0.42)


def bag_sd(q, grow=0.0):
    y = q[:, 1]
    t = np.clip(y / (2 * BH), 0, 1)
    sx = 1 + 0.16 * (1 - t)          # unten breiter
    qq = np.stack([q[:, 0] / sx, y - BH, q[:, 2]], -1)
    return sd_round_box(qq, (BW + grow, BH + grow, BD + grow), 0.1 + grow) * 0.9


def scene(p):
    q = local(p)
    body = bag_sd(q)
    # Klappe: dünne Schale vorne oben, abgerundeter unterer Rand
    shell = np.abs(bag_sd(q, 0.025)) - 0.012
    fl_lim = np.maximum(-(q[:, 2] + 0.05), np.hypot(q[:, 0] / 1.15, np.maximum(1.35 - q[:, 1], 0)) - 0.62)
    flap = np.maximum(shell, np.maximum(fl_lim, 0.62 - q[:, 1] - 0.0 * q[:, 0]))
    flap = np.maximum(flap, -(np.hypot(q[:, 0] / 1.1, q[:, 1] - 2.0) - 1.05))
    clasp = sd_round_box(q - np.array([0, 0.78, BD + 0.06]), (0.11, 0.08, 0.025), 0.03)
    hq = q - np.array([0, 2 * BH + 0.02, 0])
    handle = np.maximum(sd_torus_z(np.stack([hq[:, 0], hq[:, 1] * 1.15, hq[:, 2]], -1), 0.45, 0.045), -hq[:, 1])
    rings = np.minimum(sd_torus_z(q - np.array([-0.45, 2 * BH + 0.02, 0]), 0.06, 0.018),
                       sd_torus_z(q - np.array([0.45, 2 * BH + 0.02, 0]), 0.06, 0.018))
    gold = np.minimum(clasp, rings)
    D = np.stack([body, flap, gold, handle], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.4)
    col = np.zeros((p.shape[0], 3))
    q = local(p)
    grain = 0.97 + 0.03 * np.sin(q[:, 0] * 61 + q[:, 1] * 37) * np.sin(q[:, 1] * 53 - q[:, 2] * 41)
    base = to_lin((242, 150, 172)) * grain[:, None]
    for m_id, k in ((LEATHER, 1.0), (FLAP, 1.04), (HANDLE, 0.92)):
        m = mat == m_id
        col[m] = st.plastic(m, base[m] * k, spec=0.35, power=35, fres=0.15)
    # Ziernaht an der Klappe
    fl = mat == FLAP
    edge = np.hypot(q[fl, 0] / 1.1, q[fl, 1] - 2.0) - 1.05
    stitch = (np.abs(edge - 0.05) < 0.008) & (np.sin(np.arctan2(q[fl, 1] - 2.0, q[fl, 0]) * 160) > 0)
    col[np.where(fl)[0][stitch]] *= 0.6
    g = mat == GOLD
    col[g] = st.metal(g, to_lin((255, 200, 90)), 1.1)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.22, -1), extent=1.15, center=(0, 0.82, 0), k=0.7)
    finish(im).save(sys.argv[1])
    print("ok")
