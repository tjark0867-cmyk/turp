"""Fußball mit Raketen-Feuerschweif (selbst erzeugt)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops
from rm import nz, length, render, to_lin, to_srgb, smoothstep

PHI = (1 + 5 ** 0.5) / 2


def centers():
    pent = []
    for a in (-1, 1):
        for b in (-1, 1):
            pent += [(0, a, b * PHI), (a, b * PHI, 0), (a * PHI, 0, b)]
    hexa = [(a, b, c) for a in (-1, 1) for b in (-1, 1) for c in (-1, 1)]
    for a in (-1, 1):
        for b in (-1, 1):
            hexa += [(0, a * PHI, b / PHI), (a / PHI, 0, b * PHI), (a * PHI, b / PHI, 0)]
    return nz(np.array(pent, float)), nz(np.array(hexa, float))


PENT, HEXA = centers()
ALL = np.vstack([PENT / 2.3274, HEXA / 2.2672])
ISPENT = np.array([True] * 12 + [False] * 20)
a1, a2 = 0.5, 0.35
RX = np.array([[1, 0, 0], [0, np.cos(a1), -np.sin(a1)], [0, np.sin(a1), np.cos(a1)]])
RY = np.array([[np.cos(a2), 0, np.sin(a2)], [0, 1, 0], [-np.sin(a2), 0, np.cos(a2)]])
ROT = RX @ RY


def scene(p):
    d = length(p) - 1.0
    return d, np.zeros(p.shape[0], int)


def shade(p, n, mat, d, right, upv):
    V = -d
    L = nz(-0.55 * right + 0.8 * upv + 0.5 * V)
    q = n @ ROT.T
    s = q @ ALL.T
    srt = np.sort(s, -1)
    best = np.argmax(s, -1)
    gap = (srt[:, -1] - srt[:, -2]) * 2.3
    seam = 1 - smoothstep(0.0, 0.012, gap)
    puff = smoothstep(0.0, 0.06, gap)
    base = np.where(ISPENT[best][:, None], to_lin((24, 24, 30)), to_lin((246, 246, 244)))
    dif = np.clip((n * L).sum(-1), 0, 1)
    sky = 0.5 + 0.5 * (n * upv).sum(-1)
    H = nz(L + V)
    spec = np.clip((n * H).sum(-1), 0, 1) ** 60 * 0.7 * puff
    col = base * ((1.1 * dif + 0.3 * sky) * (0.75 + 0.25 * puff))[:, None]
    col = col * (1 - 0.8 * seam[:, None]) + spec[:, None]
    return to_srgb(col)


if __name__ == "__main__":
    ball = render(scene, shade, 520, view_dir=(0, 0, -1), extent=1.02, center=(0, 0, 0))
    S = 1000
    canvas = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    bx, by = 430, 40
    # Feuerschweif nach links unten
    fl = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rng = np.random.default_rng(4)
    cx, cy = bx + ball.width / 2, by + ball.height / 2
    ang = np.radians(135)
    dirx, diry = np.cos(ang), np.sin(ang)
    px, py = -diry, dirx
    for layer, (col, width, length_, blur) in enumerate([
        ((235, 50, 20), 1.0, 600, 7), ((255, 130, 25), 0.78, 520, 5), ((255, 205, 70), 0.52, 430, 4), ((255, 250, 225), 0.28, 320, 3)]):
        m = Image.new("L", (S, S), 0)
        d = ImageDraw.Draw(m)
        for k in range(5):
            off = (k - 2) * 46 * width
            ln = length_ * (0.8 + 0.25 * rng.random())
            w = 120 * width * (1 - abs(k - 2) * 0.22)
            sx, sy = cx + px * off, cy + py * off
            ex, ey = sx + dirx * ln, sy + diry * ln
            d.polygon([(sx + px * w, sy + py * w), (sx - px * w, sy - py * w), (ex, ey)], fill=255)
        m = m.filter(ImageFilter.GaussianBlur(blur))
        layer_img = Image.new("RGBA", (S, S), col + (0,))
        layer_img.putalpha(m)
        fl.alpha_composite(layer_img)
    canvas.alpha_composite(fl)
    canvas.alpha_composite(ball, (bx, by))
    canvas = canvas.crop(canvas.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox())
    canvas.thumbnail((800, 800), Image.LANCZOS)
    canvas.save(sys.argv[1])
    print("ok", canvas.size)
