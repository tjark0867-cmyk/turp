"""Leuchtender Konzert-Lightstick mit Herz in der Kugel (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageChops, ImageFilter
from rm import (nz, length, smin, sd_torus_y, sd_cyl_y, render, Studio, to_lin, to_srgb, finish, glow_layer, smoothstep)

GC, GR = np.array([0, 1.62, 0]), 0.62
GLOBE, BODY, GRIP, BUTTON = 0, 1, 2, 3


def scene(p):
    globe = length(p - GC) - GR
    y = p[:, 1]
    r = 0.15 + 0.03 * np.clip((y - 0.2) / 0.8, 0, 1)
    handle = sd_cyl_y(p, -0.75, 1.0, 0) - 0  # Platzhalter
    handle = np.maximum(length(p[:, [0, 2]]) - r, np.maximum(-0.75 - y, y - 1.0)) - 0.02
    collar = smin(sd_cyl_y(p, 0.92, 1.1, 0.26) - 0.02, sd_torus_y(p - np.array([0, 1.08, 0]), 0.27, 0.035), 0.03)
    body = smin(handle, collar, 0.06)
    grip = np.maximum(length(p[:, [0, 2]]) - (r + 0.035 + 0.006 * np.sin(y * 60)), np.maximum(-0.55 - y, y - 0.15))
    button = length((p - np.array([0, 0.5, 0.17])) * np.array([1, 0.7, 1.6])) - 0.06
    D = np.stack([globe, body, grip, button], -1)
    return D.min(-1), D.argmin(-1)


def sd_heart(px, py):
    px = np.abs(px)
    a = np.sqrt((px - 0.25) ** 2 + (py - 0.75) ** 2) - np.sqrt(2) / 4
    m = np.maximum(px + py, 0) * 0.5
    b = np.sqrt(np.minimum(px ** 2 + (py - 1) ** 2, (px - m) ** 2 + (py - m) ** 2)) * np.sign(px - py)
    return np.where(px + py > 1, a, b)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.4, shadow=False)
    col = np.zeros((p.shape[0], 3))
    g = mat == GLOBE
    pg = p[g]
    # Weglänge durch die Kugel -> Mitte heller
    oc = pg - GC
    b = (oc * d).sum(-1)
    c = (oc * oc).sum(-1) - GR ** 2
    chord = 2 * np.sqrt(np.clip(b * b - c, 0, None))
    depth = chord / (2 * GR)
    # Herz in der Kugel (in Bildebene)
    hx, hy = (oc * right).sum(-1), (oc * upv).sum(-1)
    hd = sd_heart(hx / 0.36, (hy + 0.3) / 0.36) * 0.36
    heart = 1 - smoothstep(-0.01, 0.01, hd)
    halo = np.exp(-np.clip(hd, 0, None) / 0.07)
    base = to_lin((255, 96, 170)) * (0.35 + 0.9 * depth[:, None]) + to_lin((255, 170, 210)) * (0.6 * halo)[:, None]
    base = base * (1 - heart[:, None]) + to_lin((255, 235, 250)) * 2.2 * heart[:, None]
    spec = np.clip((st.n[g] * st.H).sum(-1), 0, 1) ** 140 * 2.5
    col[g] = base + (st.env(g) * 0.12 * (1 - st.ndv[g]) ** 2 + spec)[:, None]
    for m_id, c_ in ((BODY, (244, 244, 248)), (GRIP, (30, 30, 36)), (BUTTON, (200, 120, 255))):
        m = mat == m_id
        col[m] = st.plastic(m, to_lin(c_), spec=0.7, power=70)
    return to_srgb(col)


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(0.25, -0.15, -1), extent=1.3, center=(0, 0.95, 0), k=0.8)
    im = im.rotate(-14, resample=Image.BICUBIC, expand=True)
    pad = 120
    big = Image.new("RGBA", (im.width + 2 * pad, im.height + 2 * pad), (0, 0, 0, 0))
    big.alpha_composite(im, (pad, pad))
    a = np.array(big).astype(float)
    lum = (a[..., 0] + a[..., 2]) / 2
    mask = Image.fromarray(((lum > 170) & (a[..., 0] > 200) & (a[..., 3] > 200)).astype(np.uint8) * 255, "L")
    out = Image.new("RGBA", big.size, (0, 0, 0, 0))
    out.alpha_composite(glow_layer(mask, (255, 120, 190), 40, 0.9))
    out.alpha_composite(big)
    finish(out).save(sys.argv[1])
    print("ok")
