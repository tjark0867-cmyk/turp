"""Versorgungskiste am Fallschirm (Airdrop, eigenes Design)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_round_box, sd_capsule, rot_z, rot_y, render, Studio, to_lin, to_srgb, finish, smoothstep)

WOOD, METAL, CANOPY, CORD = 0, 1, 2, 3
CC, RC, CAPY = np.array([0, 1.5, 0]), 1.4, 0.5


def local(p):
    return rot_y(rot_z(p - np.array([0, 1.0, 0]), 0.16), 0.5) + np.array([0, 1.0, 0])


RIM_Y = CC[1] + CAPY
RIM_R = np.sqrt(RC ** 2 - CAPY ** 2)
CORNERS = [np.array([sx * 0.44, 1.0, sz * 0.44]) for sx in (-1, 1) for sz in (-1, 1)]
RIMS = [np.array([sx * RIM_R * 0.72, RIM_Y, sz * RIM_R * 0.72]) for sx in (-1, 1) for sz in (-1, 1)]


def scene(p):
    q = local(p)
    crate = sd_round_box(q - np.array([0, 0.5, 0]), (0.5, 0.5, 0.5), 0.03)
    # Metallkanten: Rahmen an allen 12 Kanten (Box minus innere Kreuze)
    frame = sd_round_box(q - np.array([0, 0.5, 0]), (0.515, 0.515, 0.515), 0.03)
    inner = np.minimum(np.minimum(
        sd_round_box(q - np.array([0, 0.5, 0]), (0.43, 0.43, 0.6), 0.0),
        sd_round_box(q - np.array([0, 0.5, 0]), (0.6, 0.43, 0.43), 0.0)),
        sd_round_box(q - np.array([0, 0.5, 0]), (0.43, 0.6, 0.43), 0.0))
    metal = np.maximum(frame, -inner)
    shell = np.abs(length(q - CC) - RC) - 0.025
    canopy = np.maximum(shell, RIM_Y - q[:, 1])
    cords = np.min(np.stack([sd_capsule(q, a, b, 0.024) for a, b in zip(CORNERS, RIMS)], -1), -1)
    D = np.stack([crate, metal, canopy, cords], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.4)
    col = np.zeros((p.shape[0], 3))
    w = mat == WOOD
    q = local(p[w])
    ln = local(st.n[w] + 0) - local(np.zeros_like(st.n[w]))
    along = np.where(np.abs(ln[:, 1]) > 0.7, q[:, 0], q[:, 1])
    plank = np.floor((np.where(np.abs(ln[:, 1]) > 0.7, q[:, 2], q[:, 0] + q[:, 2]) + 1) / 0.29)
    fr = ((np.where(np.abs(ln[:, 1]) > 0.7, q[:, 2], q[:, 0] + q[:, 2]) + 1) / 0.29) % 1
    rnd = (np.sin(plank * 12.9898) * 43758.5453) % 1
    grain = 0.88 + 0.12 * np.sin(along * 20 + plank * 2 + np.sin(along * 4) * 2)
    gap = smoothstep(0, 0.06, fr) * (1 - smoothstep(0.94, 1, fr))
    base = to_lin((205, 145, 80)) * ((0.85 + 0.35 * rnd) * grain * (0.4 + 0.6 * gap))[:, None]
    col[w] = st.plastic(w, base, spec=0.12, power=20)
    m = mat == METAL
    col[m] = st.metal(m, to_lin((110, 160, 230)), 1.0)
    c = mat == CANOPY
    qc = local(p[c]) - CC
    gore = (np.floor((np.arctan2(qc[:, 0], qc[:, 2]) + np.pi) / (2 * np.pi) * 12) % 2).astype(bool)
    cbase = np.where(gore[:, None], to_lin((255, 130, 40)), to_lin((250, 245, 240)))
    # Innenseite (von unten gesehen) dunkler
    col[c] = st.plastic(c, cbase, spec=0.25, power=20) * (0.75 + 0.25 * st.ao[c])[:, None]
    k = mat == CORD
    col[k] = st.plastic(k, to_lin((230, 230, 225)), spec=0.1)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.1, -0.22, -1), extent=1.65, center=(0, 1.42, 0), k=0.7)
    finish(im).save(sys.argv[1])
    print("ok")
