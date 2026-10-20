"""Hölzerne Schatztruhe mit Goldbeschlägen (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smoothstep, sd_round_box, render, ambient_occlusion, soft_shadow, to_lin, to_srgb)

WOOD, METAL, HOLE = 0, 1, 2
HX, HZ, BH, LR = 0.78, 0.5, 0.82, 0.5   # halbe Breite/Tiefe, Korpushöhe, Deckelradius


def lid_sd(p, r, x0):
    q = length(np.stack([p[:, 1] - BH, p[:, 2]], -1)) - r
    return np.maximum(np.maximum(q, BH - p[:, 1]), np.abs(p[:, 0]) - x0)


def scene(p):
    body = sd_round_box(p - np.array([0, BH / 2, 0]), (HX, BH / 2, HZ), 0.02)
    lid = lid_sd(p, LR, HX) - 0.01
    wood = np.minimum(body, lid)
    m = []
    for sx in (-1, 1):
        q = p - np.array([sx * 0.5, 0, 0])
        m.append(sd_round_box(q - np.array([0, BH / 2, 0]), (0.06, BH / 2 + 0.005, HZ + 0.022), 0.012))
        m.append(lid_sd(q, LR + 0.022, 0.06))
    m.append(sd_round_box(p - np.array([0, 0.04, 0]), (HX + 0.02, 0.045, HZ + 0.02), 0.012))
    m.append(sd_round_box(p - np.array([0, BH - 0.02, 0]), (HX + 0.02, 0.04, HZ + 0.02), 0.012))
    m.append(sd_round_box(p - np.array([0, BH - 0.06, HZ + 0.03]), (0.13, 0.16, 0.025), 0.02))
    metal = np.min(np.stack(m, -1), -1)
    hole = np.maximum(length(np.stack([p[:, 0], p[:, 1] - (BH - 0.04)], -1)) - 0.035, np.abs(p[:, 2] - (HZ + 0.06)) - 0.03)
    slot = np.maximum(sd_round_box(np.stack([p[:, 0], p[:, 1] - (BH - 0.11), p[:, 2] - (HZ + 0.06)], -1), (0.015, 0.05, 0.03), 0.005), 0)
    hole = np.minimum(hole, slot)
    D = np.stack([wood, metal, hole], -1)
    return D.min(-1), D.argmin(-1)


def wood_color(p, n):
    x, y, z = p[:, 0], p[:, 1], p[:, 2]
    on_lid = y > BH + 0.005
    theta = np.arctan2(y - BH, z)
    plank = np.where(on_lid, np.floor(theta / 0.4), np.floor(y / 0.205))
    frac = np.where(on_lid, (theta / 0.4) % 1, (y / 0.205) % 1)
    side = np.abs(n[:, 0]) > 0.7
    # Seitenflächen: senkrechte Bretter
    plank = np.where(side, np.floor(z / 0.25), plank)
    frac = np.where(side, (z / 0.25) % 1, frac)
    rnd = (np.sin(plank * 12.9898) * 43758.5453) % 1
    base = to_lin((150, 88, 44)) * (0.75 + 0.4 * rnd)[:, None]
    along = np.where(side, y, x)
    grain = 0.9 + 0.1 * np.sin(along * 14 + np.sin(along * 3 + plank * 3) * 2.0 + frac * 5) \
        + 0.05 * np.sin(frac * 40 + along * 2)
    gap = smoothstep(0.0, 0.05, frac) * (1 - smoothstep(0.95, 1.0, frac))
    return base * (grain * (0.35 + 0.65 * gap))[:, None]


def env(R, Lk, right, upv):
    ry = (R * upv).sum(-1)
    rx = (R * right).sum(-1)
    hor = smoothstep(-0.25, 0.25, ry)
    e = 0.2 * (1 - hor) + (0.75 + 0.5 * np.clip(ry, 0, 1)) * hor
    e *= 1 - 0.45 * np.exp(-((ry - 0.04) / 0.1) ** 2)
    e += 2.5 * smoothstep(0.86, 0.97, (R * Lk).sum(-1))
    e += 1.0 * np.exp(-((rx + 0.42) / 0.08) ** 2) * smoothstep(-0.3, 0.0, ry)
    return e


def shade(p, n, mat, d, right, upv):
    V = -d
    L = nz(-0.55 * right + 0.8 * upv + 0.45 * V)
    ao = ambient_occlusion(scene, p, n, 0.4)
    sh = soft_shadow(scene, p + n * 0.003, L, k=12)
    dif = np.clip((n * L).sum(-1), 0, 1) * sh
    sky = 0.5 + 0.5 * n[:, 1]
    col = np.zeros((p.shape[0], 3))
    w = mat == WOOD
    col[w] = wood_color(p[w], n[w]) * (1.2 * dif[w] + 0.35 * sky[w] * ao[w])[:, None]
    m = mat == METAL
    R = d - 2 * (d * n).sum(-1, keepdims=True) * n
    e = env(R[m], L, right, upv)
    col[m] = to_lin((240, 182, 60)) * (e * (0.45 + 0.55 * sh[m]) * (0.4 + 0.6 * ao[m]))[:, None]
    # Nieten auf den Bändern
    x, y = p[m, 0], p[m, 1]
    for yy in (0.2, 0.42, 0.64):
        for xx in (-0.5, 0.5):
            r = np.hypot(x - xx, y - yy)
            col[np.where(m)[0][r < 0.022]] *= 1.6
    col[mat == HOLE] = to_lin((20, 12, 6))
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(-0.55, -0.5, -1), extent=1.08, center=(0, 0.66, 0.05), k=0.8)
    im.save(sys.argv[1])
    print("ok", im.size)
