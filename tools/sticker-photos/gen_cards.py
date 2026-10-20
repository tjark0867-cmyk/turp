"""Aufgefächerte Holo-Sammelkarten mit eigenem Design (Stern, Mond, Kristall) – selbst erzeugt."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops

W, H, R = 440, 616, 30
S = 1600


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def vgrad(w, h, c1, c2):
    t = np.linspace(0, 1, h)[:, None, None]
    arr = np.array(c1)[None, None] * (1 - t) + np.array(c2)[None, None] * t
    return Image.fromarray(np.repeat(arr, w, 1).astype(np.uint8), "RGB")


def holo(w, h, seed):
    y, x = np.mgrid[0:h, 0:w].astype(float)
    ph = (x * 0.9 + y * 0.6) / 70 + seed
    r = 0.5 + 0.5 * np.sin(ph)
    g = 0.5 + 0.5 * np.sin(ph + 2.1)
    b = 0.5 + 0.5 * np.sin(ph + 4.2)
    a = (0.5 + 0.5 * np.sin((x - y) / 23 + seed)) ** 6
    return Image.fromarray(np.dstack([r * 255, g * 255, b * 255, a * 90]).astype(np.uint8), "RGBA")


def star_pts(cx, cy, ro, ri, n=5, rot=-90):
    pts = []
    for i in range(n * 2):
        r = ro if i % 2 == 0 else ri
        a = np.radians(rot + i * 180 / n)
        pts.append((cx + r * np.cos(a), cy + r * np.sin(a)))
    return pts


def emblem(kind, size, color):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = size / 2
    if kind == "star":
        d.polygon(star_pts(c, c * 1.04, size * 0.42, size * 0.18), fill=color)
    elif kind == "moon":
        d.ellipse((c - size * 0.38, c - size * 0.38, c + size * 0.38, c + size * 0.38), fill=color)
        d.ellipse((c - size * 0.2, c - size * 0.46, c + size * 0.5, c + size * 0.24), fill=(0, 0, 0, 0))
    else:  # Kristall
        d.polygon([(c, c - size * 0.42), (c + size * 0.3, c - size * 0.1), (c, c + size * 0.42), (c - size * 0.3, c - size * 0.1)], fill=color)
    glow = im.getchannel("A").filter(ImageFilter.GaussianBlur(size * 0.08))
    out = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    out.putalpha(glow.point(lambda v: int(v * 0.8)))
    out.alpha_composite(im)
    # Facettenlicht
    hl = Image.new("L", (size, size), 0)
    ImageDraw.Draw(hl).polygon([(0, 0), (size, 0), (0, size * 0.9)], fill=70)
    hl = ImageChops.multiply(hl.filter(ImageFilter.GaussianBlur(size * 0.1)), im.getchannel("A"))
    wl = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    wl.putalpha(hl)
    out.alpha_composite(wl)
    return out


def card(art1, art2, kind, ecol, seed):
    c = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, W - 1, H - 1), R, fill=255)
    frame = vgrad(W, H, (226, 228, 238), (160, 166, 190)).convert("RGBA")
    frame.alpha_composite(holo(W, H, seed))
    c.paste(frame, (0, 0), mask)
    d = ImageDraw.Draw(c)
    # Innenfläche
    d.rounded_rectangle((18, 18, W - 19, H - 19), 18, fill=(24, 26, 48))
    # Titelzeile
    d.rounded_rectangle((34, 32, W - 35, 70), 12, fill=(44, 48, 82))
    d.rounded_rectangle((50, 45, 200, 57), 6, fill=(220, 224, 240))
    d.ellipse((W - 76, 40, W - 50, 66), fill=ecol)
    # Bildfenster
    aw, ah = W - 68, 270
    art = vgrad(aw, ah, art1, art2).convert("RGBA")
    rng = np.random.default_rng(seed)
    ad = ImageDraw.Draw(art)
    for _ in range(26):  # kleine Funkelpunkte
        x, y, r = rng.uniform(0, aw), rng.uniform(0, ah), rng.uniform(1.5, 4)
        ad.ellipse((x - r, y - r, x + r, y + r), fill=(255, 255, 255, int(rng.uniform(90, 220))))
    e = emblem(kind, 210, ecol)
    art.alpha_composite(e, ((aw - 210) // 2, (ah - 210) // 2 + 6))
    art.alpha_composite(holo(aw, ah, seed + 1))
    am = Image.new("L", (aw, ah), 0)
    ImageDraw.Draw(am).rounded_rectangle((0, 0, aw - 1, ah - 1), 10, fill=255)
    c.paste(art, (34, 84), am)
    d.rounded_rectangle((34, 84, 34 + aw - 1, 84 + ah - 1), 10, outline=(200, 205, 225), width=3)
    # Textzeilen
    y0 = 384
    for i, ln in enumerate([0.78, 0.62, 0.7, 0.45]):
        y = y0 + i * 40
        d.ellipse((46, y, 72, y + 26), fill=ecol if i < 2 else (90, 96, 130))
        d.rounded_rectangle((86, y + 8, 86 + int((W - 150) * ln), y + 18), 5, fill=(150, 156, 190))
    d.line((40, 548, W - 41, 548), fill=(70, 76, 110), width=2)
    d.polygon(star_pts(W - 58, 575, 15, 6.5), fill=(255, 214, 90))
    # Glanz
    gl = Image.new("L", (W, H), 0)
    ImageDraw.Draw(gl).polygon([(0, 0), (W * 0.7, 0), (0, H * 0.55)], fill=60)
    gl = ImageChops.multiply(gl.filter(ImageFilter.GaussianBlur(30)), mask)
    wl = Image.new("RGBA", (W, H), (255, 255, 255, 0))
    wl.putalpha(gl)
    c.alpha_composite(wl)
    c.putalpha(ImageChops.multiply(c.getchannel("A"), mask))
    return c


def place(canvas, cimg, angle, pivot):
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    layer.alpha_composite(cimg, (pivot[0] - W // 2, pivot[1] - H))
    layer = layer.rotate(angle, resample=Image.BICUBIC, center=pivot)
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sh.putalpha(layer.getchannel("A").filter(ImageFilter.GaussianBlur(16)).point(lambda v: int(v * 0.5)))
    canvas.alpha_composite(ImageChops.offset(sh, 6, 12))
    canvas.alpha_composite(layer)


if __name__ == "__main__":
    canvas = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    piv = (S // 2, S // 2 + 420)
    place(canvas, card((60, 40, 120), (190, 70, 160), "moon", (255, 236, 160), 1), 20, piv)
    place(canvas, card((20, 110, 150), (60, 210, 190), "crystal", (200, 250, 255), 2), 2, piv)
    place(canvas, card((250, 120, 60), (255, 200, 80), "star", (255, 250, 220), 3), -16, piv)
    canvas = canvas.crop(canvas.getbbox())
    canvas.thumbnail((800, 800), Image.LANCZOS)
    canvas.save(sys.argv[1])
    print("ok", canvas.size)
