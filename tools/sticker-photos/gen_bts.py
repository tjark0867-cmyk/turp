"""Glänzende Luftballons in Lila-Tönen (selbst erzeugt)."""
import sys
import numpy as np
from rm import nz, length, smin, sd_ellipsoid, sd_capsule, render, Studio, to_lin, to_srgb, finish

BALLS = [  # Mitte, Größe, Farbe
    ((0.0, 1.05, 0.25), 1.0, (150, 80, 230)),
    ((-0.82, 0.8, -0.05), 0.92, (196, 160, 245)),
    ((0.85, 0.78, -0.1), 0.94, (112, 48, 196)),
    ((-0.42, 1.78, -0.35), 0.9, (126, 66, 214)),
    ((0.5, 1.82, -0.3), 0.88, (210, 182, 250)),
]
TIE = np.array([0.0, -0.75, 0.25])


def balloon(p, c, s):
    q = (p - np.array(c)) / s
    y = q[:, 1]
    taper = 1 - 0.22 * np.clip(-y / 0.6, 0, 1) ** 1.5
    qq = np.stack([q[:, 0] / taper, y, q[:, 2] / taper], -1)
    body = sd_ellipsoid(qq, (0.5, 0.6, 0.5)) * 0.8
    knot = sd_ellipsoid(q - np.array([0, -0.64, 0]), (0.07, 0.06, 0.07))
    return smin(body, knot, 0.06) * s


def scene(p):
    ds = [balloon(p, c, s) for c, s, _ in BALLS]
    strings = []
    for c, s, _ in BALLS:
        a = np.array(c) + np.array([0, -0.68 * s, 0])
        mid = (a + TIE) / 2 + np.array([0.08, 0, 0])
        strings.append(np.minimum(sd_capsule(p, a, mid, 0.012), sd_capsule(p, mid, TIE, 0.012)))
    D = np.stack(ds + [np.min(np.stack(strings, -1), -1)], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.5)
    col = np.zeros((p.shape[0], 3))
    for i, (_, _, c) in enumerate(BALLS):
        m = mat == i
        base = to_lin(c)
        # Latex: an dünnen Rändern satter, Mitte heller
        rim = (1 - st.ndv[m]) ** 2
        b = base[None] * (0.85 + 0.35 * rim[:, None]) * (1 + 0.25 * rim[:, None] * (base[None] / base.max() - 1))
        col[m] = st.plastic(m, b, spec=1.4, power=110, fres=0.25)
        # zweites weiches Glanzlicht (Fenster)
        col[m] += (np.clip((st.n[m] * st.H).sum(-1), 0, 1) ** 12 * 0.12)[:, None]
    s = mat == len(BALLS)
    col[s] = st.plastic(s, to_lin((235, 235, 245)), spec=0.2)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.1, -0.08, -1), extent=1.72, center=(0, 0.62, 0), k=0.7)
    finish(im).save(sys.argv[1])
    print("ok")
