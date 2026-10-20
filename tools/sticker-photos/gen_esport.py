"""Goldmedaille mit Gamepad-Relief und Band (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from rm import (nz, length, smin, sd_box2, sd_cyl_z, sd_torus_z, rot_y, rot_x, render, Studio, to_lin, to_srgb, finish, smoothstep)

GOLD = 0


def pad2d(x, y):
    body = sd_box2(x, y - 0.05, 0.36, 0.12) - 0.1
    grips = np.minimum(np.hypot(x - 0.3, y + 0.12) - 0.17, np.hypot(x + 0.3, y + 0.12) - 0.17)
    shape = np.minimum(body, grips)
    dpad = np.minimum(sd_box2(x + 0.27, y - 0.03, 0.09, 0.03), sd_box2(x + 0.27, y - 0.03, 0.03, 0.09))
    btn = np.min(np.stack([np.hypot(x - 0.27 - dx, y - 0.03 - dy) - 0.035 for dx, dy in ((0.07, 0), (-0.07, 0), (0, 0.07), (0, -0.07))], -1), -1)
    return shape, np.minimum(dpad, btn)


def local(p):
    return rot_x(rot_y(p, 0.35), 0.12)


def scene(p):
    q = local(p)
    disc = sd_cyl_z(q, -0.06, 0.06, 0.72) - 0.02
    rim = sd_torus_z(q - np.array([0, 0, 0.05]), 0.68, 0.045)
    shape, holes = pad2d(q[:, 0], q[:, 1])
    relief = np.maximum(shape, np.abs(q[:, 2] - 0.07) - 0.045) - 0.012
    relief = np.maximum(relief, -np.maximum(holes, np.abs(q[:, 2] - 0.12) - 0.03))
    loop = sd_torus_z(np.stack([q[:, 0], q[:, 1] - 0.82, q[:, 2]], -1), 0.11, 0.035)
    d = smin(np.minimum(disc, rim), relief, 0.02)
    d = np.minimum(d, loop)
    return d, np.zeros(p.shape[0], int)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.25)
    q = local(p)
    r = np.hypot(q[:, 0], q[:, 1])
    flat = (local(st.n)[:, 2] > 0.9) & (r < 0.6)
    shape, _ = pad2d(q[:, 0], q[:, 1])
    satin = flat & (shape > 0.02)   # Hintergrund mattiert
    col = st.metal(np.ones(p.shape[0], bool), to_lin((255, 196, 70)))
    rays = 0.85 + 0.15 * (np.sin(np.arctan2(q[:, 1], q[:, 0]) * 36) > 0)
    col[satin] = col[satin] * 0.55 * rays[satin, None] + to_lin((200, 140, 40)) * 0.25 * st.dif[satin, None]
    return to_srgb(col)


if __name__ == "__main__":
    med = render(scene, shade, 640, view_dir=(0, -0.1, -1), extent=1.0, center=(0, 0.12, 0), k=0.7)
    S = 1000
    can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rib = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(rib)
    mx, my = S // 2, 420
    d.polygon([(mx - 230, 0), (mx - 70, 0), (mx + 40, my), (mx - 40, my)], fill=(40, 200, 185, 255))
    d.polygon([(mx + 230, 0), (mx + 70, 0), (mx - 40, my), (mx + 40, my)], fill=(130, 70, 220, 255))
    d.polygon([(mx - 150, 0), (mx - 120, 0), (mx - 5, my - 30), (mx - 25, my - 20)], fill=(255, 255, 255, 140))
    d.polygon([(mx + 150, 0), (mx + 120, 0), (mx + 5, my - 30), (mx + 25, my - 20)], fill=(255, 255, 255, 140))
    shade_ = Image.new("L", (S, S), 0)
    ImageDraw.Draw(shade_).polygon([(mx - 60, my - 140), (mx + 60, my - 140), (mx + 40, my), (mx - 40, my)], fill=90)
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sh.putalpha(shade_.filter(ImageFilter.GaussianBlur(20)))
    rib.alpha_composite(sh)
    rib.putalpha(Image.fromarray(np.minimum(np.array(rib.getchannel("A")), 255).astype(np.uint8)))
    can.alpha_composite(rib)
    can.alpha_composite(med, (mx - med.width // 2, my - 70))
    finish(can).save(sys.argv[1])
    print("ok")
