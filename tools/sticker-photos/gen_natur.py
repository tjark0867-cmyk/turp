"""Kleine schwebende Grasinsel mit Laubbaum, Steinen und Blumen (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_ellipsoid, sd_capsule, render, Studio, to_lin, to_srgb, finish, smoothstep)

LEAF, TRUNK, GRASS, EARTH, STONE, FLOWER = 0, 1, 2, 3, 4, 5
BLOBS = [((0.0, 1.95, 0.0), 0.62), ((-0.48, 1.7, 0.1), 0.45), ((0.5, 1.72, -0.05), 0.47), ((0.15, 2.35, -0.1), 0.42),
         ((-0.25, 2.2, 0.25), 0.38), ((0.3, 1.55, 0.35), 0.36), ((-0.2, 1.5, -0.35), 0.36)]
FLOWERS = [(-0.75, 0.62, 0.45, (255, 90, 120)), (0.62, 0.6, 0.55, (255, 210, 60)), (0.95, 0.56, 0.05, (255, 255, 255)),
           (-0.95, 0.56, -0.2, (180, 120, 255)), (0.2, 0.62, 0.75, (255, 140, 60))]


def noise(p, f):
    return np.sin(p[:, 0] * f + 1.3) * np.sin(p[:, 1] * f * 1.1 + 2.1) * np.sin(p[:, 2] * f * 0.9 + 0.7)


def scene(p):
    leaf = 9.0
    for c, r in BLOBS:
        leaf = smin(leaf, length(p - np.array(c)) - r, 0.12)
    leaf = leaf + 0.05 * noise(p, 9) + 0.02 * noise(p, 23)
    trunk = sd_capsule(p, (0, 0.45, 0), (0, 1.7, 0), 0.11 - 0.0)
    trunk = smin(trunk, sd_capsule(p, (0, 1.2, 0), (-0.4, 1.7, 0.1), 0.06), 0.06)
    trunk = smin(trunk, sd_capsule(p, (0, 1.35, 0), (0.42, 1.8, -0.05), 0.055), 0.06)
    island = sd_ellipsoid(p - np.array([0, 0.45, 0]), (1.25, 0.6, 1.0))
    island = np.maximum(island, p[:, 1] - 0.55 - 0.02 * noise(p, 8))
    island = island + 0.03 * noise(p, 6)
    stones = np.minimum(sd_ellipsoid(p - np.array([0.75, 0.58, -0.35]), (0.16, 0.1, 0.13)),
                        sd_ellipsoid(p - np.array([0.55, 0.57, -0.55]), (0.1, 0.07, 0.09)))
    fl = np.min(np.stack([length(p - np.array([x, y + 0.05, z])) - 0.055 for x, y, z, _ in FLOWERS], -1), -1)
    stems = np.min(np.stack([sd_capsule(p, (x, y - 0.06, z), (x, y + 0.03, z), 0.012) for x, y, z, _ in FLOWERS], -1), -1)
    ground = np.minimum(island, stems)
    D = np.stack([leaf, trunk, ground, np.full(p.shape[0], 9.0), stones, fl], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.5)
    col = np.zeros((p.shape[0], 3))
    l = mat == LEAF
    v = 0.85 + 0.2 * noise(p[l], 17)
    lc = to_lin((70, 160, 60)) * v[:, None] * (0.7 + 0.5 * smoothstep(1.4, 2.6, p[l, 1]))[:, None]
    col[l] = st.plastic(l, lc, spec=0.15, power=20, fres=0.1)
    t = mat == TRUNK
    bark = 0.8 + 0.2 * (np.sin(np.arctan2(p[t, 0], p[t, 2]) * 14 + p[t, 1] * 6) > 0.3)
    col[t] = st.plastic(t, to_lin((110, 72, 44)) * bark[:, None], spec=0.05)
    g = mat == GRASS
    top = st.n[g, 1] > 0.35
    grass = to_lin((96, 186, 70)) * (0.9 + 0.12 * noise(p[g], 21))[:, None]
    earth = to_lin((140, 96, 60)) * (0.85 + 0.15 * (np.sin(p[g, 1] * 40 + noise(p[g], 10) * 3) > 0))[:, None]
    gc = np.where(top[:, None], grass, earth)
    rim = (st.n[g, 1] > 0.2) & (st.n[g, 1] <= 0.35)
    gc[rim] = to_lin((80, 150, 60))
    col[g] = st.plastic(g, gc, spec=0.08, power=15)
    s = mat == STONE
    col[s] = st.plastic(s, to_lin((150, 150, 158)) * (0.9 + 0.1 * noise(p[s], 50))[:, None], spec=0.2)
    f = mat == FLOWER
    fcol = np.zeros((f.sum(), 3))
    pf = p[f]
    for x, y, z, c in FLOWERS:
        near = np.hypot(pf[:, 0] - x, pf[:, 2] - z) < 0.09
        fcol[near] = to_lin(c)
    col[f] = st.plastic(f, fcol, spec=0.3)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.32, -1), extent=1.45, center=(0, 1.35, 0), k=0.6)
    finish(im).save(sys.argv[1])
    print("ok")
