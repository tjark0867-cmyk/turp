"""Flachbild-Fernseher mit Streaming-Oberfläche (eigenes Design)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_round_box, sd_box2, rot_y, render, Studio, to_lin, to_srgb, finish, smoothstep)

FRAME, SCREEN, STAND = 0, 1, 2
TW, TH, TY = 1.25, 0.74, 1.08
A = 0.38


def scene(p):
    q = rot_y(p, A)
    frame = sd_round_box(q - np.array([0, TY, 0]), (TW, TH, 0.045), 0.03)
    screen = sd_round_box(q - np.array([0, TY, 0.04]), (TW - 0.04, TH - 0.04, 0.008), 0.004)
    neck = sd_round_box(q - np.array([0, 0.22, -0.05]), (0.09, 0.2, 0.03), 0.02)
    base = sd_round_box(q - np.array([0, 0.03, -0.05]), (0.5, 0.025, 0.24), 0.02)
    stand = smin(neck, base, 0.04)
    D = np.stack([frame, screen, stand], -1)
    return D.min(-1), D.argmin(-1)


def screen_tex(u, v):
    """u: -1..1 (Breite), v: -1..1 (Höhe) -> lineare Farbe (leuchtend)."""
    t = (v + 1) / 2
    bg = to_lin((20, 16, 44)) * (1 - t[:, None]) + to_lin((60, 30, 90)) * t[:, None]
    # großes „Szenenbild“: weicher Farbverlauf mit Sonne/Berg (Fantasie-Szene)
    scene_m = (v > -0.35)
    sky = to_lin((255, 120, 90)) * (1 - (v[:, None] + 0.35) / 1.35) + to_lin((110, 70, 200)) * ((v[:, None] + 0.35) / 1.35)
    col = np.where(scene_m[:, None], sky, bg)
    sun = np.hypot((u - 0.42) * 1.69, v - 0.12) < 0.24
    col[sun] = to_lin((255, 220, 140))
    hills = v < -0.1 + 0.18 * np.sin(u * 3.2 + 0.6) * np.exp(-((u + 0.2) ** 2))
    col[scene_m & hills] = to_lin((40, 30, 80))
    # Play-Knopf
    r = np.hypot(u * 1.69, v)
    disc = r < 0.3
    col[disc] = col[disc] * 0.45 + to_lin((255, 255, 255)) * 0.45
    tri = (u * 1.69 > -0.08) & (np.abs(v) < 0.14 - (u * 1.69 + 0.08) * 0.55) & (u * 1.69 < 0.18)
    col[tri] = to_lin((255, 255, 255)) * 1.2
    # Fortschrittsbalken
    bar = (np.abs(v + 0.47) < 0.018) & (np.abs(u) < 0.88)
    col[bar] = to_lin((120, 120, 140))
    prog = bar & (u < 0.15)
    col[prog] = to_lin((40, 220, 200)) * 1.3
    dot = np.hypot((u - 0.15) * 1.69, v + 0.47) < 0.045
    col[dot] = to_lin((255, 255, 255)) * 1.2
    # Vorschaubilder der nächsten Folgen
    for i, c in enumerate([(255, 150, 80), (90, 200, 255), (255, 100, 160), (130, 230, 140)]):
        cx = -0.66 + i * 0.44
        th = sd_box2(u - cx, v + 0.76, 0.19, 0.13) < 0.0
        col[th] = to_lin(c) * 0.8
    return col * 1.25


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.4)
    col = np.zeros((p.shape[0], 3))
    f = mat == FRAME
    col[f] = st.plastic(f, to_lin((28, 28, 32)), spec=0.6, power=60) + st.env(f, 0.05)[:, None]
    s = mat == SCREEN
    q = rot_y(p[s], A)
    u, v = q[:, 0] / (TW - 0.04), (q[:, 1] - TY) / (TH - 0.04)
    tex = screen_tex(u, v)
    f_ = 0.04 + 0.96 * (1 - st.ndv[s]) ** 5
    refl = st.env(s, 1.0) * (0.03 + 0.4 * f_)
    sheen = smoothstep(0.2, 0.0, np.abs(u + v * 0.6 - 0.1)) * 0.06
    col[s] = tex + (refl + sheen)[:, None]
    t = mat == STAND
    col[t] = st.metal(t, to_lin((70, 72, 80)), 0.8)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.12, -1), extent=1.35, center=(0, 0.95, 0), k=0.8)
    finish(im).save(sys.argv[1])
    print("ok")
