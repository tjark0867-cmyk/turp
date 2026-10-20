"""Ringständer aus lila Samt mit sieben gestapelten Ringen und Diamanten („7 rings“) – eigenes Design."""
import sys
import numpy as np
from PIL import Image, ImageDraw
from rm import (nz, length, smin, sd_torus_y, sd_cyl_y, rot_x, rot_z, render, Studio, to_lin, to_srgb, finish)

CONE, BASE, GOLD, ROSE, SILVER, GEM = range(6)
H = 1.75


def cone_r(y):
    return 0.46 - 0.3 * np.clip(y / H, 0, 1)


RINGS = []
for i in range(7):
    y = 0.28 + i * 0.19
    metal = (GOLD, ROSE, SILVER)[i % 3]
    RINGS.append(dict(y=y, R=cone_r(y) + 0.04, r=0.042 if i % 2 else 0.034, tx=0.12 * np.sin(i * 1.7), tz=0.1 * np.cos(i * 2.3),
                      metal=metal, gem=i in (1, 3, 5)))


def ring_local(p, rg):
    return rot_x(rot_z(p - np.array([0, rg["y"], 0]), rg["tz"]), rg["tx"])


def gem_sd(g, size):
    """facettierter Brillant, Achse +z (zeigt nach vorne)"""
    q = np.stack([g[:, 0], g[:, 2], -g[:, 1]], -1)  # Achse z -> y
    rr = np.hypot(q[:, 0], q[:, 2])
    a = np.arctan2(q[:, 2], q[:, 0])
    a8 = (a % (np.pi / 4)) - np.pi / 8
    rf = rr * np.cos(a8)  # Abstand zur Facettenkante
    y = q[:, 1] / size
    crown = (rf / size) * 0.7 + y * 0.72 - 0.62 * 0.72 - 0.12
    table = y - 0.32
    pav = (rf / size) * 0.62 - y * 0.78 - 0.62
    gird = rf / size - 0.62
    return np.maximum.reduce([crown, table, pav, gird]) * size * 0.8


def scene(p):
    y = p[:, 1]
    cone = np.maximum(length(p[:, [0, 2]]) - cone_r(y), np.maximum(-y, y - H)) * 0.9
    cone = smin(cone, length(p - np.array([0, H, 0])) - cone_r(H), 0.05)
    base = sd_cyl_y(p, -0.14, 0.0, 0.62) - 0.03
    ds = {GOLD: 9.0, ROSE: 9.0, SILVER: 9.0}
    gems = 9.0
    for rg in RINGS:
        q = ring_local(p, rg)
        t = sd_torus_y(q, rg["R"], rg["r"])
        ds[rg["metal"]] = np.minimum(ds[rg["metal"]], t)
        if rg["gem"]:
            gc = np.array([0, 0.0, rg["R"] + rg["r"] + 0.05])
            gems = np.minimum(gems, gem_sd(q - gc, 0.12))
            prong = sd_cyl_y(q - np.array([0, 0, rg["R"] + rg["r"] * 0.6]), -0.03, 0.03, 0.055) - 0.01
            ds[rg["metal"]] = smin(ds[rg["metal"]], prong, 0.02)
    D = np.stack([cone, base, ds[GOLD], ds[ROSE], ds[SILVER], np.full(p.shape[0], 9.0) + 0 * gems + (gems - 9.0)], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.25)
    col = np.zeros((p.shape[0], 3))
    c = mat == CONE
    rim = (1 - st.ndv[c]) ** 2.2
    col[c] = to_lin((150, 110, 200)) * (0.35 + 0.8 * st.dif[c] * st.ao[c])[:, None] + to_lin((220, 200, 255)) * rim[:, None] * 0.45
    b = mat == BASE
    col[b] = st.glossy(b, to_lin((245, 240, 248)), 0.4)
    for m_id, tint in ((GOLD, (255, 200, 90)), (ROSE, (245, 170, 140)), (SILVER, (225, 228, 238))):
        m = mat == m_id
        col[m] = st.metal(m, to_lin(tint), 1.15)
    g = mat == GEM
    R = st.R[g]
    k = np.sin(R[:, 0] * 23) * np.sin(R[:, 1] * 19) * np.sin(R[:, 2] * 29)
    fire = np.stack([np.sin(R[:, 0] * 31) * 0.5 + 0.5, np.sin(R[:, 1] * 37 + 2) * 0.5 + 0.5, np.sin(R[:, 2] * 41 + 4) * 0.5 + 0.5], -1)
    bright = np.where(k > 0.15, 2.2, 0.25)
    col[g] = (0.55 * bright[:, None] + 0.35 * fire * bright[:, None]) * (0.6 + 0.4 * st.env(g)[:, None])
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.0, -0.22, -1), extent=1.08, center=(0, 0.82, 0), k=0.7)
    can = Image.new("RGBA", (im.width + 160, im.height + 100), (0, 0, 0, 0))
    can.alpha_composite(im, (80, 60))
    d = ImageDraw.Draw(can)
    for (x, y, r) in ((can.width * 0.72, 70, 30), (can.width * 0.2, can.height * 0.3, 24), (can.width - 70, can.height * 0.62, 20)):
        d.polygon([(x, y - r), (x + r * 0.28, y - r * 0.28), (x + r, y), (x + r * 0.28, y + r * 0.28), (x, y + r),
                   (x - r * 0.28, y + r * 0.28), (x - r, y), (x - r * 0.28, y - r * 0.28)], fill=(255, 244, 210, 255))
    finish(can).save(sys.argv[1])
    print("ok")
