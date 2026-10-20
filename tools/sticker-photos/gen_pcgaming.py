"""Gaming-Maus mit RGB-Beleuchtung (eigenes Design, ohne Marke)."""
import sys
import numpy as np
from PIL import Image
from rm import (nz, length, smin, sd_ellipsoid, sd_round_box, sd_cyl_y, rot_y, rot_z, render, Studio,
                to_lin, to_srgb, finish, glow_layer, smoothstep)

BODY, WHEEL, SIDEBTN = 0, 1, 2


def local(p):
    return rot_y(p, 2.65)


def hsv(h):
    h = h % 1
    r = np.clip(np.abs(h * 6 - 3) - 1, 0, 1)
    g = np.clip(2 - np.abs(h * 6 - 2), 0, 1)
    b = np.clip(2 - np.abs(h * 6 - 4), 0, 1)
    return np.stack([r, g, b], -1)


def scene(p):
    q = local(p)
    z = q[:, 2]
    hump = 1 + 0.28 * np.exp(-((z + 0.25) / 0.45) ** 2) - 0.12 * np.clip(z / 1.0, 0, 1)
    body = sd_ellipsoid(np.stack([q[:, 0], q[:, 1] / hump, z], -1), (0.56, 0.36, 1.0)) * 0.8
    body = np.maximum(body, -q[:, 1] + 0.02)
    groove = sd_round_box(q - np.array([0, 0.35, 0.65]), (0.012, 0.3, 0.6), 0.005)
    body = np.maximum(body, -groove)
    slot = sd_round_box(q - np.array([0, 0.35, 0.48]), (0.07, 0.3, 0.17), 0.03)
    body = np.maximum(body, -slot)
    wq = np.stack([q[:, 1] - 0.3, q[:, 2] - 0.48, q[:, 0]], -1)
    wheel = np.maximum(length(wq[:, :2]) - 0.11, np.abs(wq[:, 2]) - 0.035) - 0.01
    side = sd_round_box(q - np.array([-0.53, 0.2, 0.25]), (0.03, 0.035, 0.12), 0.02)
    side = np.minimum(side, sd_round_box(q - np.array([-0.52, 0.2, -0.02]), (0.03, 0.035, 0.11), 0.02))
    D = np.stack([body, wheel, side], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.35)
    col = np.zeros((p.shape[0], 3))
    b = mat == BODY
    q = local(p[b])
    base = st.plastic(b, to_lin((26, 27, 32)), spec=0.35, power=30, fres=0.18)
    # RGB-Leiste rundum (unten) und Logo-Fläche hinten
    strip = np.abs(q[:, 1] - 0.07) < 0.025
    ang = np.arctan2(q[:, 0], q[:, 2])
    rgb = hsv(ang / (2 * np.pi) + 0.1) * 1.8
    base[strip] = rgb[strip]
    logo = (st.n[b, 1] > 0.5) & (np.hypot(q[:, 0] * 1.0, (q[:, 2] + 0.45)) < 0.12) & \
           (np.abs(q[:, 0]) + np.abs(q[:, 2] + 0.45) * 0.8 < 0.11)
    base[logo] = hsv(np.full(logo.sum(), 0.48)) * 1.8
    col[b] = base
    w = mat == WHEEL
    ql = local(p[w])
    ridge = 0.6 + 0.4 * (np.sin(np.arctan2(ql[:, 1] - 0.3, ql[:, 2] - 0.48) * 24) > 0)
    col[w] = st.plastic(w, to_lin((40, 40, 46)), spec=0.3) * ridge[:, None] + to_lin((80, 230, 220)) * 0.35
    s = mat == SIDEBTN
    col[s] = st.plastic(s, to_lin((50, 52, 60)), spec=0.5)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.1, -0.75, -1), extent=1.08, center=(0, 0.15, 0.05), k=0.7)
    pad = 70
    big = Image.new("RGBA", (im.width + 2 * pad, im.height + 2 * pad), (0, 0, 0, 0))
    big.alpha_composite(im, (pad, pad))
    a = np.array(big).astype(int)
    sat = a[..., :3].max(-1) - a[..., :3].min(-1)
    mask = Image.fromarray(((sat > 120) & (a[..., 3] > 200)).astype(np.uint8) * 255, "L")
    out = Image.new("RGBA", big.size, (0, 0, 0, 0))
    out.alpha_composite(glow_layer(mask, (120, 200, 255), 16, 1.6))
    out.alpha_composite(big)
    finish(out).save(sys.argv[1])
    print("ok")
