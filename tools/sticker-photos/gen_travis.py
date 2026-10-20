"""Achterbahn-Looping mit Wagen (selbst erzeugt)."""
import sys
import numpy as np
from rm import (nz, length, smin, sd_round_box, sd_torus_z, sd_capsule, sd_cyl_y, rot_y, rot_z, render, Studio,
                to_lin, to_srgb, finish)

RAIL, TIE, SUPPORT, CART, SEAT = 0, 1, 2, 3, 4
LC, R = np.array([0.0, 1.3, 0.0]), 0.92
YB = LC[1] - R


def local(p):
    return rot_y(p, 0.5)


def scene(p):
    q = local(p)
    rails = []
    for z in (-0.15, 0.15):
        rails.append(sd_torus_z(q - np.array([0, LC[1], z]), R, 0.035))
        rails.append(sd_capsule(q, (-2.0, YB, z), (0.0, YB, z), 0.035))
    spine = sd_torus_z(q - LC, R + 0.1, 0.05)
    spine = np.minimum(spine, sd_capsule(q, (-2.0, YB - 0.1, 0), (0.0, YB - 0.1, 0), 0.05))
    rail = np.minimum(np.min(np.stack(rails, -1), -1), spine)
    # Schwellen im Looping
    rel = q - LC
    phi = np.arctan2(rel[:, 1], rel[:, 0])
    step = np.radians(12)
    pr = np.round(phi / step) * step
    tc = np.stack([R * np.cos(pr) + LC[0], R * np.sin(pr) + LC[1], np.zeros_like(pr)], -1)
    tq = rot_z(q - tc, -pr)  # x radial, y tangential
    ties = sd_round_box(tq - np.array([0.05, 0, 0]), (0.06, 0.02, 0.17), 0.01)
    xs = np.round(np.clip(q[:, 0], -1.95, -0.05) / 0.15) * 0.15
    ties = np.minimum(ties, sd_round_box(np.stack([q[:, 0] - xs, q[:, 1] - (YB - 0.05), q[:, 2]], -1), (0.02, 0.06, 0.17), 0.01))
    sup = np.min(np.stack([sd_cyl_y(q - np.array([x, 0, 0]), -0.05, YB - 0.12, 0.05) for x in (-1.7, -0.9)] +
                          [sd_capsule(q, (0.0, -0.05, -0.0), (0.0, YB - 0.12, 0), 0.06),
                           sd_round_box(q - np.array([-0.85, -0.05, 0]), (1.25, 0.04, 0.3), 0.02)], -1), -1)
    cq = q - np.array([-1.25, YB + 0.19, 0])
    cart = sd_round_box(cq, (0.32, 0.1, 0.2), 0.06)
    seat = np.minimum(sd_round_box(cq - np.array([-0.12, 0.13, 0]), (0.04, 0.09, 0.16), 0.03),
                      sd_round_box(cq - np.array([0.16, 0.13, 0]), (0.04, 0.09, 0.16), 0.03))
    D = np.stack([rail, ties, sup, cart, seat], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.3)
    col = np.zeros((p.shape[0], 3))
    r = mat == RAIL
    col[r] = st.plastic(r, to_lin((245, 110, 30)), spec=0.8, power=60) + st.env(r, 0.08)[:, None]
    t = mat == TIE
    col[t] = st.metal(t, to_lin((150, 155, 165)), 0.8)
    s = mat == SUPPORT
    col[s] = st.plastic(s, to_lin((235, 235, 240)), spec=0.4)
    c = mat == CART
    col[c] = st.glossy(c, to_lin((40, 190, 175)), 0.4)
    se = mat == SEAT
    col[se] = st.plastic(se, to_lin((40, 40, 46)), spec=0.3)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.18, -1), extent=1.45, center=(-0.45, 1.05, 0), k=0.7)
    finish(im).save(sys.argv[1])
    print("ok")
