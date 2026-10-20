"""Gestapelte, abgerundete Kunststoff-Bauklötze (selbst erzeugt, ohne Noppen/Logo)."""
import sys
import numpy as np
from rm import rot_y, nz, sd_round_box, render, ambient_occlusion, soft_shadow, to_lin, to_srgb, smoothstep

G = 1.06
CUBES = [
    ((0, 0.5, 0), (226, 52, 60)),      # hinten, rot
    ((G, 0.5, 0), (40, 120, 235)),     # rechts vorne, blau
    ((0, 0.5, G), (255, 196, 30)),     # links vorne, gelb
    ((0, 0.5 + G, 0), (34, 190, 140)), # oben, grün
]
ROT = [0, 0.05, -0.06, 0.32]  # oberer Klotz leicht verdreht


def scene(p):
    D = np.stack([sd_round_box(rot_y(p - np.array(c), ROT[i]), (0.5, 0.5, 0.5), 0.1) for i, (c, _) in enumerate(CUBES)], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    V = -d
    L = nz(-0.55 * right + 0.8 * upv + 0.5 * V)
    L2 = nz(0.7 * right - 0.1 * upv + 0.5 * V)
    base = np.array([to_lin(CUBES[m][1]) for m in range(len(CUBES))])[mat]
    ao = ambient_occlusion(scene, p, n, 0.6)
    sh = soft_shadow(scene, p + n * 0.003, L, k=12)
    dif = np.clip((n * L).sum(-1), 0, 1) * sh
    dif2 = np.clip((n * L2).sum(-1), 0, 1)
    sky = 0.5 + 0.5 * n[:, 1]
    H = nz(L + V)
    spec = np.clip((n * H).sum(-1), 0, 1) ** 70 * sh
    fres = (1 - np.clip((n * V).sum(-1), 0, 1)) ** 4
    col = base * (1.25 * dif + 0.22 * dif2 + 0.32 * sky * ao)[:, None]
    col += (0.9 * spec + 0.12 * fres * ao)[:, None]
    # ganz feine Kunststoff-Körnung
    rng = np.random.default_rng(3)
    col *= (1 + rng.normal(0, 0.012, col.shape[0]))[:, None]
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(-1, -0.82, -1), extent=1.75, center=(0.36, 1.0, 0.36))
    im.save(sys.argv[1])
    print("ok", im.size)
