"""Kleines Haus mit Garten auf einer Rasenplatte – wie im Baumodus (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_round_box, sd_ellipsoid, sd_capsule, rot_y, render, Studio, to_lin, to_srgb, finish, smoothstep)

GRASS, SOIL, WALL, ROOF, WOOD, GLASS, FRAME, BUSH, PATH, CHIM = range(10)


def local(p):
    return rot_y(p, 0.62)


def roof_sd(q):
    # Satteldach: Prisma entlang x
    y = q[:, 1] - 1.32
    z = np.abs(q[:, 2])
    slope = (y * 0.78 + z * 0.62) - 0.62 * 0.98
    d = np.maximum(slope, np.maximum(-y, np.abs(q[:, 0]) - 0.92))
    return d - 0.02


def scene(p):
    q = local(p)
    plate = sd_round_box(q - np.array([0, -0.08, 0.15]), (1.45, 0.08, 1.2), 0.05)
    soil = sd_round_box(q - np.array([0, -0.22, 0.15]), (1.43, 0.08, 1.18), 0.04)
    walls = sd_round_box(q - np.array([0, 0.66, 0]), (0.82, 0.66, 0.7), 0.02)
    roof = roof_sd(q)
    door = sd_round_box(q - np.array([0.25, 0.38, 0.71]), (0.15, 0.36, 0.02), 0.01)
    wins = []
    for x, y in ((-0.38, 0.62), (0.05, 1.0), (-0.38, 1.0)):
        wins.append(sd_round_box(q - np.array([x, y, 0.71]), (0.16, 0.15, 0.015), 0.01))
    for z in (-0.3, 0.3):
        wins.append(sd_round_box(q - np.array([-0.83, 0.75, z]), (0.015, 0.15, 0.14), 0.01))
    glass = np.min(np.stack(wins, -1), -1)
    frames = np.min(np.stack([sd_round_box(q - np.array([x, y, 0.705]), (0.19, 0.18, 0.02), 0.01) for x, y in ((-0.38, 0.62), (0.05, 1.0), (-0.38, 1.0))] +
                             [sd_round_box(q - np.array([-0.825, 0.75, z]), (0.02, 0.18, 0.17), 0.01) for z in (-0.3, 0.3)], -1), -1)
    chim = sd_round_box(q - np.array([-0.45, 1.75, -0.25]), (0.11, 0.3, 0.11), 0.015)
    bush = 9.0
    for c, r in (((0.95, 0.2, 0.75), 0.22), ((1.12, 0.17, 0.5), 0.17), ((-0.95, 0.22, 0.8), 0.24), ((-1.15, 0.5, -0.6), 0.32)):
        bush = smin(bush, length(q - np.array(c)) - r, 0.05)
    trunk = sd_capsule(q, (-1.15, 0.0, -0.6), (-1.15, 0.3, -0.6), 0.05)
    path = sd_round_box(q - np.array([0.25, 0.005, 1.0]), (0.16, 0.012, 0.32), 0.01)
    D = np.stack([plate, soil, walls, roof, np.minimum(door, trunk), glass, frames, bush, path, chim], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.35)
    col = np.zeros((p.shape[0], 3))
    q = local(p)
    nse = np.sin(q[:, 0] * 47 + 1) * np.sin(q[:, 2] * 53 + 2) * np.sin(q[:, 1] * 41)
    pal = {GRASS: (110, 200, 80), SOIL: (150, 100, 62), WALL: (250, 238, 214), ROOF: (214, 84, 64), WOOD: (150, 96, 58),
           FRAME: (255, 255, 255), BUSH: (70, 160, 70), PATH: (210, 205, 195), CHIM: (190, 110, 90)}
    for m_id, c in pal.items():
        m = mat == m_id
        base = np.tile(to_lin(c), (m.sum(), 1))
        if m_id == ROOF:
            ql = q[m]
            rows = ((ql[:, 1] * 9) % 1) < 0.12
            base[rows] *= 0.75
            gable = np.abs(local(st.n[m])[:, 0]) > 0.8
            base[gable] = to_lin((250, 238, 214))
            rw = gable & (np.hypot(ql[:, 2], ql[:, 1] - 1.55) < 0.13)
            base[rw] = to_lin((120, 190, 240))
            rf = gable & (np.abs(np.hypot(ql[:, 2], ql[:, 1] - 1.55) - 0.14) < 0.025)
            base[rf] = to_lin((255, 255, 255))
        if m_id in (GRASS, BUSH):
            base *= (0.9 + 0.12 * nse[m])[:, None]
        if m_id == WALL:
            ql = q[m]
            bricks = (((ql[:, 1] * 10) % 1) < 0.06)
            base[bricks] *= 0.93
        col[m] = st.plastic(m, base, spec=0.15 if m_id != FRAME else 0.4, power=30)
    g = mat == GLASS
    fr = 0.04 + 0.96 * (1 - st.ndv[g]) ** 5
    col[g] = to_lin((120, 190, 240)) * (0.4 + 0.5 * st.dif[g])[:, None] + (st.env(g) * (0.15 + 0.6 * fr))[:, None]
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.62, -1), extent=1.95, center=(0, 0.78, 0.1), k=0.75)
    finish(im).save(sys.argv[1])
    print("ok")
