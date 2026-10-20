"""KI-Chip mit leuchtender Beschriftung und Leiterbahnen (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from rm import (nz, length, sd_round_box, render, Studio, to_lin, to_srgb, finish, glow_layer)

PKG, LID, PIN = 0, 1, 2
font = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf", 300)
TXT = Image.new("L", (512, 512), 0)
ImageDraw.Draw(TXT).text((256, 270), "AI", font=font, anchor="mm", fill=255)
TXT = np.array(TXT.filter(ImageFilter.GaussianBlur(1))) / 255.0


def scene(p):
    pkg = sd_round_box(p - np.array([0, 0.08, 0]), (0.8, 0.08, 0.8), 0.03)
    lid = sd_round_box(p - np.array([0, 0.19, 0]), (0.6, 0.035, 0.6), 0.03)
    # Pins: an allen vier Seiten, wiederholt
    pins = []
    for axis in (0, 2):
        q = p.copy()
        a = q[:, axis]
        o = q[:, 2 - axis]
        rep = np.clip(np.round(o / 0.14), -5, 5)
        o2 = o - rep * 0.14
        side = np.abs(a) - 0.88
        qq = np.stack([o2, q[:, 1] - 0.05, side], -1)
        pins.append(sd_round_box(qq, (0.035, 0.018, 0.1), 0.01))
    pin = np.minimum(pins[0], pins[1])
    D = np.stack([pkg, lid, pin], -1)
    return D.min(-1), D.argmin(-1)


def shade(p, n, mat, d, right, upv):
    st = Studio(scene, p, n, d, right, upv, 0.3)
    col = np.zeros((p.shape[0], 3))
    k = mat == PKG
    col[k] = st.plastic(k, to_lin((24, 26, 32)), spec=0.4, power=40)
    l = mat == LID
    x, z = p[l, 0], p[l, 2]
    brushed = 0.92 + 0.08 * np.sin(x * 400 + np.sin(z * 30))
    col[l] = st.metal(l, to_lin((175, 180, 190)), 0.9) * brushed[:, None]
    top = st.n[l, 1] > 0.9
    iu = np.clip(((x / 0.55) * 0.5 + 0.5) * 511, 0, 511).astype(int)
    iv = np.clip(((z / 0.55) * 0.5 + 0.5) * 511, 0, 511).astype(int)
    t = TXT[iv, iu] * top
    border = top & (np.abs(np.maximum(np.abs(x), np.abs(z)) - 0.5) < 0.012)
    glow = to_lin((60, 240, 220)) * 1.8
    col[l] = col[l] * (1 - t[:, None]) + glow * t[:, None]
    col[np.where(l)[0][border]] = glow
    pn = mat == PIN
    col[pn] = st.metal(pn, to_lin((235, 190, 90)), 0.9)
    return to_srgb(col)


if __name__ == "__main__":
    chip = render(scene, shade, 640, view_dir=(-0.55, -1.0, -0.75), extent=1.35, center=(0, 0.1, 0), k=0.8)
    S = 1100
    can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    lines = Image.new("L", (S, S), 0)
    dd = ImageDraw.Draw(lines)
    cx, cy = S // 2, S // 2
    rng = np.random.default_rng(2)
    for i in range(16):
        ang = i / 16 * 2 * np.pi + 0.2
        r0, r1 = 240, rng.uniform(380, 500)
        x0, y0 = cx + r0 * np.cos(ang), cy + r0 * np.sin(ang) * 0.7
        xm, ym = cx + (r0 + r1) / 2 * np.cos(ang), cy + (r0 + r1) / 2 * np.sin(ang) * 0.7
        x1 = xm + (r1 - r0) / 2 * (1 if np.cos(ang) > 0 else -1)
        dd.line([(x0, y0), (xm, ym), (x1, ym)], fill=255, width=9)
        dd.ellipse((x1 - 15, ym - 15, x1 + 15, ym + 15), fill=255)
    can.alpha_composite(glow_layer(lines, (40, 230, 210), 12, 1.3))
    tr = Image.new("RGBA", (S, S), (70, 235, 215, 0))
    tr.putalpha(lines)
    can.alpha_composite(tr)
    can.alpha_composite(chip, (cx - chip.width // 2, cy - chip.height // 2))
    finish(can).save(sys.argv[1])
    print("ok")
