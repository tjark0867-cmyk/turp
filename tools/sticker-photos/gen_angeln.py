"""Angelrute mit Rolle, Schnur und rot-weißer Pose (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_round_cone, sd_capsule, sd_cyl_z, render, Studio, to_lin, to_srgb, finish)

ROD, CORK, METAL, LINE, FLOAT_R, FLOAT_W, REEL = range(7)
A = np.array([-1.25, -1.05, 0.0])
B = np.array([1.2, 1.45, 0.0])
DIR = nz(B - A)
DOWN = np.array([DIR[1], -DIR[0], 0.0])
TIPY = -0.35


def at(t):
    return A + (B - A) * t


def ring(p, c, R, r):
    q = p - c
    along = q @ DIR
    radial = length(q - along[:, None] * DIR)
    return np.hypot(radial - R, along) - r


def scene(p):
    rod = sd_round_cone(p, at(0.2), B, 0.05, 0.012)
    cork = sd_round_cone(p, A, at(0.24), 0.085, 0.078)
    butt = sd_round_cone(p, A - DIR * 0.08, A + DIR * 0.02, 0.07, 0.085)
    seat = sd_round_cone(p, at(0.24), at(0.32), 0.07, 0.065)
    metal = np.minimum(butt, seat)
    for t in (0.45, 0.58, 0.7, 0.8, 0.88, 0.95):
        c = at(t) + DOWN * (0.07 - 0.03 * t)
        metal = np.minimum(metal, ring(p, c, 0.035 - 0.015 * t, 0.007))
        metal = np.minimum(metal, sd_capsule(p, at(t), c - DOWN * 0.03, 0.008))
    # Rolle unter der Rute
    rc = at(0.28) + DOWN * 0.27
    stem = sd_capsule(p, at(0.28), rc, 0.03)
    q = p - rc
    along = q @ DIR
    radial = length(q - along[:, None] * DIR)
    spool = np.maximum(radial - 0.15, np.abs(along) - 0.11) - 0.02
    knob_arm = sd_capsule(p, rc + np.array([0, 0, 0.15]), rc + np.array([0, 0, 0.15]) + DOWN * 0.17, 0.02)
    knob = length(p - (rc + np.array([0, 0, 0.17]) + DOWN * 0.17)) - 0.045
    reel = smin(np.minimum(spool, stem), np.minimum(knob_arm, knob), 0.02)
    line = sd_capsule(p, B, np.array([B[0], TIPY + 0.12, 0]), 0.014)
    line = np.minimum(line, sd_capsule(p, np.array([B[0], TIPY - 0.12, 0]), np.array([B[0], TIPY - 0.45, 0]), 0.014))
    fl = length((p - np.array([B[0], TIPY, 0])) * np.array([1, 0.8, 1])) - 0.11
    stick = sd_capsule(p, np.array([B[0], TIPY + 0.1, 0]), np.array([B[0], TIPY + 0.24, 0]), 0.018)
    flt = np.minimum(fl, stick)
    hk = p - np.array([B[0] + 0.05, TIPY - 0.5, 0])
    hook = np.maximum(np.hypot(np.hypot(hk[:, 0], hk[:, 1]) - 0.05, hk[:, 2]) - 0.014, hk[:, 1] - 0.0)
    metal = np.minimum(metal, hook)
    fmat = np.where(p[:, 1] > TIPY, FLOAT_R, FLOAT_W)
    D = np.stack([rod, cork, metal, line, np.where(fmat == FLOAT_R, flt, 9.0), np.where(fmat == FLOAT_W, flt, 9.0), reel], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.25)
    col = np.zeros((p.shape[0], 3))
    r = mat == ROD
    col[r] = st.plastic(r, to_lin((24, 60, 110)), spec=1.0, power=90) + st.env(r, 0.12)[:, None]
    c = mat == CORK
    x, y, z = p[c, 0], p[c, 1], p[c, 2]
    nse = np.sin(x * 160 + y * 90) * np.sin(y * 140 - z * 120) * np.sin(z * 130 + x * 70)
    col[c] = st.plastic(c, to_lin((196, 150, 100)) * (0.9 + 0.1 * nse - 0.25 * (nse > 0.6))[:, None], spec=0.05)
    m = mat == METAL
    col[m] = st.metal(m, to_lin((200, 204, 212)), 1.0)
    l = mat == LINE
    col[l] = to_lin((230, 230, 230)) * 0.9
    fr = mat == FLOAT_R
    col[fr] = st.plastic(fr, to_lin((230, 40, 40)), spec=0.8, power=60)
    fw = mat == FLOAT_W
    col[fw] = st.plastic(fw, to_lin((245, 245, 245)), spec=0.8, power=60)
    rl = mat == REEL
    col[rl] = st.metal(rl, to_lin((60, 64, 72)), 0.9) + st.plastic(rl, to_lin((20, 20, 24)), spec=0.3) * 0.5
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 760, view_dir=(0.15, -0.1, -1), extent=1.5, center=(0, 0.2, 0), k=0.6)
    finish(im).save(sys.argv[1])
    print("ok")
