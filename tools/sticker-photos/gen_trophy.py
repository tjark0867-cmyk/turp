"""Goldener Pokal mit Stern auf schwarzem Sockel (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smin, smoothstep, sd_round_box, sd_ellipsoid, sd_torus_y, sd_torus_z,
                render, ambient_occlusion, soft_shadow, to_lin, to_srgb)

GOLD, BLACK, PLATE = 0, 1, 2
Y0, Y1 = 0.98, 1.82          # Kelch unten / oben


def r_cup(y):
    t = np.clip((y - Y0) / (Y1 - Y0), 0, 1)
    return 0.1 + 0.6 * (1 - (1 - t) ** 2.1)


def sd_lathe(p, y0, y1, rfun, k=0.6):
    q = length(p[:, [0, 2]])
    y = p[:, 1]
    d_side = (q - rfun(np.clip(y, y0, y1))) * k
    return np.maximum(d_side, np.maximum(y0 - y, y - y1))


def r_foot(y):
    t = np.clip((y - 0.44) / 0.2, 0, 1)
    return 0.1 + 0.25 * (1 - t) ** 2


def scene(p):
    # Sockel
    plinth = sd_round_box(p - np.array([0, 0.22, 0]), (0.5, 0.22, 0.5), 0.03)
    plate = sd_round_box(p - np.array([0, 0.22, 0.5]), (0.3, 0.1, 0.012), 0.008)
    # Fuß, Schaft, Knauf
    foot = sd_lathe(p, 0.44, 0.64, r_foot)
    lip = sd_torus_y(p - np.array([0, 0.465, 0]), 0.33, 0.028)
    stem = sd_lathe(p, 0.6, 1.02, lambda y: 0.07 + 0 * y, k=1.0)
    knob = sd_ellipsoid(p - np.array([0, 0.8, 0]), (0.15, 0.07, 0.15))
    # Kelch (hohl) mit Rand
    outer = sd_lathe(p, Y0, Y1, r_cup)
    inner = sd_lathe(p, Y0 + 0.1, Y1 + 0.2, lambda y: r_cup(y) - 0.035)
    cup = np.maximum(outer, -inner)
    rim = sd_torus_y(p - np.array([0, Y1, 0]), 0.682, 0.028)
    # Henkel links/rechts (nur die äußere Hälfte)
    ax = np.abs(p[:, 0])
    hp = np.stack([ax - 0.64, p[:, 1] - 1.42, p[:, 2]], -1)
    handle = np.maximum(sd_torus_z(hp, 0.22, 0.042), 0.5 - ax)
    gold = smin(foot, lip, 0.02)
    gold = smin(gold, stem, 0.05)
    gold = smin(gold, knob, 0.04)
    gold = smin(gold, cup, 0.05)
    gold = np.minimum(gold, rim)
    gold = smin(gold, handle, 0.03)
    D = np.stack([gold, plinth, plate], -1)
    return D.min(-1), D.argmin(-1)


def sd_star5(px, py, r, rf):
    k1 = np.array([0.809016994375, -0.587785252292])
    k2 = np.array([-k1[0], k1[1]])
    x, y = np.abs(px), py.copy()
    dt = np.maximum(k1[0] * x + k1[1] * y, 0)
    x, y = x - 2 * dt * k1[0], y - 2 * dt * k1[1]
    dt = np.maximum(k2[0] * x + k2[1] * y, 0)
    x, y = x - 2 * dt * k2[0], y - 2 * dt * k2[1]
    x = np.abs(x)
    y = y - r
    ba = np.array([-k1[1] * rf, k1[0] * rf - 1])
    h = np.clip((x * ba[0] + y * ba[1]) / (ba @ ba), 0, r)
    return np.sqrt((x - ba[0] * h) ** 2 + (y - ba[1] * h) ** 2) * np.sign(y * ba[0] - x * ba[1])


def env(R, Lk, Lf, right):
    ry = R[:, 1]
    hor = smoothstep(-0.25, 0.25, ry)
    e = (0.22 + 0.1 * np.clip(-ry, 0, 1)) * (1 - hor) + (0.8 + 0.6 * np.clip(ry, 0, 1)) * hor
    e *= 1 - 0.45 * np.exp(-((ry - 0.04) / 0.1) ** 2)
    rx = (R * right).sum(-1)
    e += 1.5 * np.exp(-((rx + 0.42) / 0.07) ** 2) * smoothstep(-0.3, 0.0, ry)   # Lichtstreifen links
    e += 0.6 * np.exp(-((rx - 0.55) / 0.09) ** 2) * smoothstep(-0.3, 0.0, ry)   # schwächer rechts
    e += 3.0 * smoothstep(0.86, 0.97, (R * Lk).sum(-1))
    e += 1.3 * smoothstep(0.9, 0.985, (R * Lf).sum(-1))
    return e


def shade(p, n, mat, d, right, upv):
    V = -d
    Lk = nz(-0.55 * right + 0.75 * upv + 0.45 * V)
    Lf = nz(0.8 * right + 0.15 * upv + 0.35 * V)
    # Stern auf der Vorderseite des Kelchs: leicht erhaben (Normale verbiegen)
    theta = np.arctan2(p[:, 0], p[:, 2])
    sx, sy = theta * 0.55, p[:, 1] - 1.4
    sd = sd_star5(sx, sy, 0.2, 0.45)
    on_cup = (mat == GOLD) & (p[:, 1] > Y0 + 0.15) & (p[:, 1] < Y1 - 0.05) & (np.abs(theta) < 1.2)
    edge = on_cup & (np.abs(sd) < 0.012)
    inside = on_cup & (sd < 0)
    n = n.copy()
    n[inside] = nz(n[inside] + 0.18 * upv)  # Sternfläche fängt mehr Licht
    R = d - 2 * (d * n).sum(-1, keepdims=True) * n
    ao = ambient_occlusion(scene, p, n, 0.5)
    sh = soft_shadow(scene, p + n * 0.004, Lk, k=10)
    e = env(R, Lk, Lf, right)
    col = np.zeros((p.shape[0], 3))
    g = mat == GOLD
    gold = to_lin((255, 196, 64))
    col[g] = gold * (e[g] * (0.35 + 0.65 * sh[g]) * (0.4 + 0.6 * ao[g]))[:, None]
    col[edge] *= 0.45
    col[inside] *= 1.3
    b = mat == BLACK
    fres = 0.04 + 0.96 * (1 - np.clip((n * V).sum(-1), 0, 1)) ** 5
    dif = np.clip((n * Lk).sum(-1), 0, 1) * sh
    col[b] = (to_lin((22, 22, 28)) * (0.25 + 0.9 * dif[b])[:, None] * ao[b, None]
              + (0.06 + 0.5 * fres[b])[:, None] * e[b, None] * 0.35)
    pl = mat == PLATE
    brushed = 0.9 + 0.1 * np.sin(p[pl, 0] * 900)
    col[pl] = gold * (0.45 + 0.6 * e[pl] * brushed)[:, None] * ao[pl, None]
    # eingravierte Linien auf dem Schild
    lines = pl.copy()
    lines[pl] = (np.abs(p[pl, 1] - 0.24) < 0.008) & (np.abs(p[pl, 0]) < 0.2) | \
                (np.abs(p[pl, 1] - 0.18) < 0.006) & (np.abs(p[pl, 0]) < 0.13)
    col[lines] *= 0.35
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(-0.22, -0.32, -1), extent=1.04, center=(0, 0.94, 0), k=0.6)
    im.save(sys.argv[1])
    print("ok", im.size)
