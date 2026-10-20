"""Altes Zauberbuch mit Ledereinband, Messingecken, Schließe und leuchtendem Siegel (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smoothstep, sd_round_box, render, ambient_occlusion, soft_shadow, to_lin, to_srgb)

LEATHER, PAGES, BRASS = 0, 1, 2
TOP = 0.36


def scene(p):
    cover = sd_round_box(p - np.array([0, 0.18, 0]), (0.74, 0.18, 1.0), 0.04)
    slot = sd_round_box(p - np.array([0.12, 0.18, 0]), (0.8, 0.125, 0.975), 0.005)
    cover = np.maximum(cover, -slot)
    pages = sd_round_box(p - np.array([0.0, 0.18, 0]), (0.7, 0.128, 0.955), 0.01)
    b = []
    for sz in (-1, 1):
        for yy in (TOP, 0.0):
            b.append(sd_round_box(p - np.array([0.62, yy, sz * 0.88]), (0.14, 0.018, 0.14), 0.012))
            b.append(sd_round_box(p - np.array([0.75, 0.18 + (0.1 if yy else -0.1), sz * 0.88]), (0.012, 0.09, 0.14), 0.008))
    b.append(sd_round_box(p - np.array([0.6, TOP + 0.005, 0]), (0.16, 0.016, 0.1), 0.01))
    b.append(sd_round_box(p - np.array([0.765, 0.2, 0]), (0.016, 0.17, 0.1), 0.01))
    brass = np.min(np.stack(b, -1), -1)
    D = np.stack([cover, pages, brass], -1)
    return D.min(-1), D.argmin(-1)


def emblem(u, v):
    """Siegel auf dem Deckel: Kreisringe, achtzackiger Stern, Punkte. 0..1"""
    r = np.hypot(u, v)
    a = np.arctan2(v, u)
    ring = (np.abs(r - 0.46) < 0.018) | (np.abs(r - 0.39) < 0.01)
    # achtzackiger Stern aus zwei Quadraten (Linien)
    sq1 = np.abs(np.maximum(np.abs(u), np.abs(v)) - 0.27) < 0.012
    ur, vr = (u + v) / np.sqrt(2), (v - u) / np.sqrt(2)
    sq2 = np.abs(np.maximum(np.abs(ur), np.abs(vr)) - 0.27) < 0.012
    dots = np.zeros_like(u, bool)
    for k in range(12):
        ang = k * np.pi / 6
        dots |= np.hypot(u - 0.425 * np.cos(ang), v - 0.425 * np.sin(ang)) < 0.018
    core = r < 0.07
    lines = (r > 0.08) & (r < 0.38) & (np.abs(np.sin(a * 4)) < 0.04)
    return (ring | sq1 | sq2 | dots | core | lines).astype(float), r


def shade(p, n, mat, d, right, upv):
    V = -d
    L = nz(-0.5 * right + 0.85 * upv + 0.45 * V)
    ao = ambient_occlusion(scene, p, n, 0.3)
    sh = soft_shadow(scene, p + n * 0.003, L, k=12)
    dif = np.clip((n * L).sum(-1), 0, 1) * sh
    sky = 0.5 + 0.5 * n[:, 1]
    H = nz(L + V)
    col = np.zeros((p.shape[0], 3))
    x, y, z = p[:, 0], p[:, 1], p[:, 2]
    le = mat == LEATHER
    nse = np.sin(x[le] * 131 + z[le] * 77) * np.sin(z[le] * 119 - y[le] * 91) * np.sin(y[le] * 103 + x[le] * 59)
    wear = 0.85 + 0.12 * nse + 0.1 * smoothstep(0.5, 1.0, np.maximum(np.abs(x[le]) / 0.74, np.abs(z[le])))
    base = to_lin((112, 34, 38)) * wear[:, None]
    lit = (1.1 * dif[le] + 0.32 * sky[le] * ao[le])
    c = base * lit[:, None] + (np.clip((n[le] * H).sum(-1), 0, 1) ** 25 * 0.18 * sh[le])[:, None]
    # Siegel und Zierrahmen auf dem Deckel (leuchtend)
    top = n[le, 1] > 0.8
    u, v = z[le] / 0.62, -(x[le] + 0.04) / 0.62
    em, r = emblem(u, v)
    frame = (np.abs(np.maximum(np.abs(x[le] + 0.02) / 0.62, np.abs(z[le]) / 0.86) - 1) < 0.012)
    glow = np.exp(-((r - 0.43) / 0.12) ** 2) * 0.35
    teal = to_lin((90, 255, 220))
    mark = top & ((em > 0) | frame)
    c[top] += teal * glow[top, None] * 0.6
    c[mark] = teal * 1.4
    col[le] = c
    pg = mat == PAGES
    lines = 0.8 + 0.2 * (np.sin(y[pg] * 900) * 0.5 + 0.5)
    col[pg] = to_lin((236, 220, 180)) * (lines * (1.05 * dif[pg] + 0.4 * sky[pg] * ao[pg]))[:, None]
    br = mat == BRASS
    R = d - 2 * (d * n[br]).sum(-1, keepdims=True) * n[br]
    ry = (R * upv).sum(-1)
    e = 0.25 + 0.7 * smoothstep(-0.2, 0.5, ry) + 2.2 * smoothstep(0.86, 0.97, (R * L).sum(-1))
    col[br] = to_lin((214, 160, 70)) * (e * (0.5 + 0.5 * sh[br]) * (0.5 + 0.5 * ao[br]))[:, None]
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(-0.75, -1.25, -0.9), extent=1.25, center=(0.05, 0.18, 0.0), k=0.8)
    im.save(sys.argv[1])
    print("ok", im.size)
