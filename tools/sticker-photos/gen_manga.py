"""Manga-Seite: Panels, Speedlines, Rasterpunkte, Sprechblase und Soundwörter (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops
from rm import finish

W, H = 760, 1060
JP = "/usr/share/fonts/opentype/noto/NotoSansCJK-Black.ttc"
INK = (16, 16, 18, 255)
PAPER = (252, 250, 244, 255)


def font(size):
    return ImageFont.truetype(JP, size, index=0)


def screentone(w, h, spacing=9, grad=True):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(0, h + spacing, spacing):
        for x in range(0, w + spacing, spacing):
            xo = x + (spacing / 2 if (y // spacing) % 2 else 0)
            t = (y / h) if grad else 0.5
            r = 0.6 + 3.2 * t
            d.ellipse((xo - r, y - r, xo + r, y + r), fill=INK)
    return img


def panel_box(page, box):
    d = ImageDraw.Draw(page)
    d.rectangle(box, outline=INK, width=6)


page = Image.new("RGBA", (W, H), PAPER)
d = ImageDraw.Draw(page)
M, G = 34, 18
# Panel 1: Speedlines mit Fokus + DON!
p1 = (M, M, W - M, 430)
layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ld = ImageDraw.Draw(layer)
cx, cy = (p1[0] + p1[2]) / 2 + 40, (p1[1] + p1[3]) / 2 + 10
rng = np.random.default_rng(4)
for i in range(110):
    a = rng.uniform(0, 2 * np.pi)
    r0 = rng.uniform(120, 170)
    w = rng.uniform(1.5, 6)
    x0, y0 = cx + np.cos(a) * r0, cy + np.sin(a) * r0
    x1, y1 = cx + np.cos(a) * 700, cy + np.sin(a) * 700
    nx, ny = -np.sin(a) * w, np.cos(a) * w
    ld.polygon([(x0, y0), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny)], fill=INK)
clip = Image.new("L", (W, H), 0)
ImageDraw.Draw(clip).rectangle(p1, fill=255)
layer.putalpha(ImageChops.multiply(layer.getchannel("A"), clip))
page.alpha_composite(layer)
sfx = Image.new("RGBA", (W, H), (0, 0, 0, 0))
sd = ImageDraw.Draw(sfx)
sd.text((cx, cy), "ドン!", font=font(170), anchor="mm", fill=INK, stroke_width=14, stroke_fill=PAPER)
sfx = sfx.rotate(-8, resample=Image.BICUBIC, center=(cx, cy))
page.alpha_composite(sfx)
panel_box(page, p1)
# Panel 2 links: Rasterpunkte + Sprechblase (senkrechter Text)
p2 = (M, 430 + G, int(W * 0.5) - G // 2, 770)
tone = screentone(p2[2] - p2[0], p2[3] - p2[1])
page.alpha_composite(tone, (p2[0], p2[1]))
bx0, by0, bx1, by1 = p2[0] + 50, p2[1] + 30, p2[2] - 40, p2[3] - 60
d.ellipse((bx0 - 6, by0 - 6, bx1 + 6, by1 + 6), fill=INK)
d.polygon([(bx0 + 60, by1 - 30), (bx0 + 10, by1 + 50), (bx0 + 110, by1 - 20)], fill=INK)
d.ellipse((bx0, by0, bx1, by1), fill=PAPER)
d.polygon([(bx0 + 66, by1 - 34), (bx0 + 20, by1 + 38), (bx0 + 104, by1 - 26)], fill=PAPER)
f2 = font(64)
tx = (bx0 + bx1) / 2
for i, ch in enumerate("すごい!"):
    d.text((tx, by0 + 52 + i * 62), ch if ch != "!" else "！", font=f2, anchor="mm", fill=INK)
panel_box(page, p2)
# Panel 2 rechts: diagonale Aktionslinien + Funkeln
p3 = (int(W * 0.5) + G // 2, 430 + G, W - M, 770)
layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ld = ImageDraw.Draw(layer)
for i in range(60):
    off = rng.uniform(-400, 400)
    w = rng.uniform(1, 5)
    x0 = p3[0] + off
    ld.line([(x0, p3[3]), (x0 + 420, p3[1])], fill=INK, width=int(w))
clip = Image.new("L", (W, H), 0)
ImageDraw.Draw(clip).rectangle((p3[0] + 30, p3[1], p3[2], p3[3]), fill=255)
ImageDraw.Draw(clip).ellipse((p3[0] + 40, p3[1] + 70, p3[2] - 20, p3[3] - 60), fill=0)
layer.putalpha(ImageChops.multiply(layer.getchannel("A"), clip))
page.alpha_composite(layer)
scx, scy = (p3[0] + p3[2]) / 2 + 10, (p3[1] + p3[3]) / 2
for (x, y, r) in ((scx, scy, 70), (scx - 70, scy - 80, 28), (scx + 60, scy + 90, 22)):
    d.polygon([(x, y - r), (x + r * 0.22, y - r * 0.22), (x + r, y), (x + r * 0.22, y + r * 0.22), (x, y + r),
               (x - r * 0.22, y + r * 0.22), (x - r, y), (x - r * 0.22, y - r * 0.22)], fill=INK)
panel_box(page, p3)
# Panel 3 unten: Herzen + ドキドキ
p4 = (M, 770 + G, W - M, H - M)
tone = screentone(p4[2] - p4[0], p4[3] - p4[1], spacing=11, grad=False)
page.alpha_composite(tone, (p4[0], p4[1]))
for (x, y, s) in ((p4[0] + 120, p4[1] + 120, 60), (p4[2] - 130, p4[1] + 90, 44), (p4[2] - 220, p4[3] - 70, 30)):
    pts = []
    for t in np.linspace(0, 2 * np.pi, 60):
        hx = 16 * np.sin(t) ** 3
        hy = -(13 * np.cos(t) - 5 * np.cos(2 * t) - 2 * np.cos(3 * t) - np.cos(4 * t))
        pts.append((x + hx * s / 16, y + hy * s / 16))
    d.polygon(pts, fill=PAPER, outline=INK, width=5)
d.text(((p4[0] + p4[2]) / 2 + 20, (p4[1] + p4[3]) / 2 + 8), "ドキドキ", font=font(92), anchor="mm", fill=INK, stroke_width=10, stroke_fill=PAPER)
panel_box(page, p4)

# Eselsohr unten rechts
fold = 120
mask = Image.new("L", (W, H), 255)
ImageDraw.Draw(mask).polygon([(W - fold, H), (W, H - fold), (W, H)], fill=0)
page.putalpha(mask)
flap = Image.new("RGBA", (W, H), (0, 0, 0, 0))
fd = ImageDraw.Draw(flap)
fd.polygon([(W - fold, H), (W, H - fold), (W - fold, H - fold)], fill=(228, 224, 214, 255))
fd.line([(W - fold, H), (W, H - fold)], fill=(200, 196, 186, 255), width=3)
page.alpha_composite(flap)

# zweite Seite dahinter + Schatten
S = 1300
can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
back = Image.new("RGBA", (W, H), (236, 232, 222, 255))
back.putalpha(mask)
bl = Image.new("RGBA", (S, S), (0, 0, 0, 0))
bl.alpha_composite(back, ((S - W) // 2, (S - H) // 2))
bl = bl.rotate(9, resample=Image.BICUBIC)
fl = Image.new("RGBA", (S, S), (0, 0, 0, 0))
fl.alpha_composite(page, ((S - W) // 2, (S - H) // 2))
fl = fl.rotate(-4, resample=Image.BICUBIC)
for lay in (bl, fl):
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sh.putalpha(lay.getchannel("A").filter(ImageFilter.GaussianBlur(14)).point(lambda v: int(v * 0.35)))
    s2 = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    s2.alpha_composite(sh.crop((0, 0, S - 8, S - 12)), (8, 12))
    can.alpha_composite(s2)
    can.alpha_composite(lay)
finish(can).save(sys.argv[1])
print("ok")
