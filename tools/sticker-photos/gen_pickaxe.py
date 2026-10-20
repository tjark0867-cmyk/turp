"""Spitzhacke mit Holzstiel und geschmiedetem Stahlkopf (selbst erzeugt)."""
import sys
import numpy as np
from PIL import Image
from rm import (nz, length, smin, smoothstep, sd_round_box, render, ambient_occlusion, soft_shadow, to_lin, to_srgb)

WOOD, STEEL, GRIP = 0, 1, 2
HC = np.array([0.0, 0.25, 0.0])   # Mittelpunkt des Kopf-Bogens
RM, AMAX = 1.68, 0.5             # Bogenradius, halber Öffnungswinkel


def head_sd(p):
    rel = p - HC
    ang = np.arctan2(rel[:, 0], rel[:, 1])
    t = np.clip(np.abs(ang) / AMAX, 0, 1)
    r = 0.012 + 0.1 * (1 - t ** 1.7)
    ring = length(rel[:, [0, 1]]) - RM
    q = np.stack([ring, rel[:, 2] * 1.25], -1)
    d = length(q) - r
    return np.maximum(d * 0.8, (np.abs(ang) - AMAX) * RM)


def scene(p):
    y = p[:, 1]
    taper = 0.075 - 0.012 * np.clip((y - 0.2) / 1.7, 0, 1)
    handle = np.maximum(length(p[:, [0, 2]]) - taper, np.maximum(-y, y - 2.02)) - 0.006
    grip_r = 0.088 + 0.006 * np.sin(y * 70)
    grip = np.maximum(length(p[:, [0, 2]]) - grip_r, np.maximum(0.05 - y, y - 0.62))
    eye = sd_round_box(p - np.array([0, 1.9, 0]), (0.12, 0.15, 0.115), 0.035)
    steel = smin(head_sd(p), eye, 0.05)
    D = np.stack([handle, steel, grip], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    V = -d
    L = nz(-0.55 * right + 0.8 * upv + 0.45 * V)
    ao = ambient_occlusion(scene, p, n, 0.3)
    sh = soft_shadow(scene, p + n * 0.003, L, k=12)
    dif = np.clip((n * L).sum(-1), 0, 1) * sh
    sky = 0.5 + 0.5 * n[:, 1]
    col = np.zeros((p.shape[0], 3))
    x, y, z = p[:, 0], p[:, 1], p[:, 2]
    w = mat == WOOD
    grain = 0.86 + 0.14 * np.sin(y[w] * 34 + np.sin(np.arctan2(x[w], z[w]) * 3 + y[w] * 2) * 2.2)
    col[w] = to_lin((176, 118, 62)) * (grain * (1.15 * dif[w] + 0.35 * sky[w] * ao[w]))[:, None]
    H = nz(L + V)
    col[w] += (np.clip((n[w] * H).sum(-1), 0, 1) ** 40 * 0.15 * sh[w])[:, None]
    g = mat == GRIP
    ridge = 0.75 + 0.25 * (np.sin(y[g] * 70) * 0.5 + 0.5)
    col[g] = to_lin((70, 42, 30)) * (ridge * (1.1 * dif[g] + 0.3 * sky[g] * ao[g]))[:, None]
    col[g] += (np.clip((n[g] * H).sum(-1), 0, 1) ** 20 * 0.12 * sh[g])[:, None]
    s = mat == STEEL
    R = d - 2 * (d * n[s]).sum(-1, keepdims=True) * n[s]
    ry = (R * upv).sum(-1)
    rx = (R * right).sum(-1)
    hor = smoothstep(-0.25, 0.25, ry)
    e = 0.12 * (1 - hor) + (0.5 + 0.4 * np.clip(ry, 0, 1)) * hor
    e *= 1 - 0.5 * np.exp(-((ry - 0.03) / 0.1) ** 2)
    e += 1.8 * np.exp(-((rx + 0.4) / 0.09) ** 2) * smoothstep(-0.2, 0.1, ry)
    e += 2.0 * smoothstep(0.88, 0.97, (R * L).sum(-1))
    nse = np.sin(x[s] * 151 + y[s] * 83) * np.sin(y[s] * 137 - z[s] * 97) * np.sin(z[s] * 121 + x[s] * 67)
    forged = 0.82 + 0.1 * nse
    # Spitzen blank geschliffen
    rel = p[s] - HC
    tip = smoothstep(0.62, 0.95, np.abs(np.arctan2(rel[:, 0], rel[:, 1])) / AMAX)
    base = to_lin((92, 98, 108)) * (1 - tip[:, None]) + to_lin((200, 205, 212)) * tip[:, None]
    col[s] = base * (forged * (0.55 * dif[s] + 0.25 * ao[s]))[:, None] + to_lin((190, 196, 206)) * (e * (0.5 + 0.5 * sh[s]) * 0.55)[:, None]
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 760, view_dir=(-0.35, -0.12, -1), extent=1.12, center=(0, 1.05, 0), k=0.7)
    im = im.rotate(-38, resample=Image.BICUBIC, expand=True)
    im = im.crop(im.getbbox())
    im.save(sys.argv[1])
    print("ok", im.size)
