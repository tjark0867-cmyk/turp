"""Überwachungskamera mit roter Aufnahme-LED (selbst erzeugt, Nachtwächter-Stimmung)."""
import sys
import numpy as np
from PIL import Image
from rm import (nz, length, smin, sd_round_box, sd_cyl_z, sd_capsule, rot_x, rot_y, render, Studio,
                to_lin, to_srgb, finish, glow_layer, smoothstep)

HOUSING, LENS, RIM, ARM, LED = 0, 1, 2, 3, 4
C = np.array([0.0, 1.0, 0.0])
AY, AX = -0.62, 0.32


def local(p):
    return rot_x(rot_y(p - C, AY), -AX)


def to_world(q):
    # Umkehrung von local() für einzelne Punkte
    from rm import rot_x as rx, rot_y as ry
    return ry(rx(np.atleast_2d(q), AX), -AY)[0] + C


JOINT = to_world(np.array([0, -0.34, -0.3]))
WALL = np.array([-0.95, 0.55, -1.05])


def scene(p):
    q = local(p)
    housing = sd_round_box(q, (0.32, 0.26, 0.72), 0.08)
    visor = sd_round_box(q - np.array([0, 0.3, 0.1]), (0.37, 0.02, 0.8), 0.015)
    housing = np.minimum(housing, visor)
    rim = sd_cyl_z(q, 0.66, 0.84, 0.165) - 0.015
    lens = length(q - np.array([0, 0, 0.74])) - 0.135
    rim = np.maximum(rim, -(sd_cyl_z(q, 0.7, 0.9, 0.13)))
    led = length(q - np.array([0.235, -0.17, 0.79])) - 0.032
    arm = np.minimum(sd_capsule(p, JOINT, WALL, 0.06), length(p - JOINT) - 0.1)
    plate = sd_round_box(rot_y(p - WALL, 0.75) - np.array([0, 0, -0.04]), (0.26, 0.34, 0.04), 0.02)
    arm = smin(arm, plate, 0.05)
    D = np.stack([housing, lens, rim, arm, led], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.4)
    col = np.zeros((p.shape[0], 3))
    h = mat == HOUSING
    x, y, z = p[h, 0], p[h, 1], p[h, 2]
    grime = 0.92 + 0.08 * np.sin(x * 37 + z * 23) * np.sin(y * 29 - x * 17)
    col[h] = st.plastic(h, to_lin((222, 218, 206)) * grime[:, None], spec=0.35, power=40)
    l = mat == LENS
    q = local(p[l])
    r = np.hypot(q[:, 0], q[:, 1])
    iris = (np.abs(r - 0.085) < 0.01) | (np.abs(r - 0.05) < 0.007)
    glass = to_lin((10, 14, 22)) * (0.6 + 0.4 * st.dif[l])[:, None] + st.env(l, 0.6)[:, None] * to_lin((150, 190, 255))
    glass[iris] += to_lin((60, 70, 90))
    col[l] = glass + (np.clip((st.n[l] * st.H).sum(-1), 0, 1) ** 200 * 3)[:, None]
    col[mat == RIM] = st.plastic(mat == RIM, to_lin((40, 40, 46)), spec=0.6)
    col[mat == ARM] = st.metal(mat == ARM, to_lin((150, 152, 160)), 0.8)
    col[mat == LED] = to_lin((255, 60, 50)) * 2.5
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.15, -0.2, -1), extent=1.15, center=(-0.25, 0.85, 0), k=0.8)
    pad = 80
    big = Image.new("RGBA", (im.width + 2 * pad, im.height + 2 * pad), (0, 0, 0, 0))
    big.alpha_composite(im, (pad, pad))
    a = np.array(big).astype(int)
    mask = Image.fromarray((((a[..., 0] > 220) & (a[..., 1] < 140) & (a[..., 3] > 200)) * 255).astype(np.uint8), "L")
    out = Image.new("RGBA", big.size, (0, 0, 0, 0))
    out.alpha_composite(big)
    out.alpha_composite(glow_layer(mask, (255, 50, 40), 20, 4.0))
    finish(out).save(sys.argv[1])
    print("ok")
