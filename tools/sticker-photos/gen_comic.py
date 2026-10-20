"""Comic-Knall „POW!“ mit Rasterpunkten (selbst erzeugt, keine Figur)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops

S = 1400
C = S / 2
rng = np.random.default_rng(7)


def burst(n, ro, ri, jitter, rot=0):
    pts = []
    for i in range(n * 2):
        a = np.radians(rot + i * 180 / n)
        r = (ro if i % 2 == 0 else ri) * (1 + rng.uniform(-jitter, jitter))
        pts.append((C + r * np.cos(a), C + r * np.sin(a) * 0.86))
    return pts


img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
outer = burst(15, 650, 470, 0.08, 4)
inner = burst(15, 540, 400, 0.07, 10)
d.polygon(outer, fill=(10, 10, 14))
d.polygon(burst(15, 0, 0, 0), fill=None)
# rote Zacken (etwas kleiner als die schwarze Kontur)
red = Image.new("L", (S, S), 0)
ImageDraw.Draw(red).polygon(outer, fill=255)
red = red.filter(ImageFilter.MinFilter(25))
img.paste((232, 38, 52, 255), (0, 0), red)
# gelbe Zacken mit Kontur
yl = Image.new("L", (S, S), 0)
ImageDraw.Draw(yl).polygon(inner, fill=255)
img.paste((10, 10, 14, 255), (0, 0), yl)
yl_in = yl.filter(ImageFilter.MinFilter(19))
img.paste((255, 212, 20, 255), (0, 0), yl_in)
# Rasterpunkte (zum Rand hin größer)
dots = Image.new("L", (S, S), 0)
dd = ImageDraw.Draw(dots)
step = 26
for gy in range(-S, 2 * S, step):
    for gx in range(-S, 2 * S, step):
        x = C + (gx - gy) * 0.7071
        y = C + (gx + gy) * 0.7071
        if not (0 <= x < S and 0 <= y < S):
            continue
        r = np.hypot(x - C, (y - C) / 0.86) / 520
        rad = 1.5 + 7.5 * np.clip(r - 0.25, 0, 1)
        dd.ellipse((x - rad, y - rad, x + rad, y + rad), fill=255)
dots = ImageChops.multiply(dots, yl_in)
img.paste((246, 140, 10, 255), (0, 0), dots)
# Schrift mit 3D-Kante
font = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-BoldItalic.ttf", 330)
text = "POW!"
tl = Image.new("RGBA", (S, S), (0, 0, 0, 0))
td = ImageDraw.Draw(tl)
for k in range(26, 0, -2):  # Extrusion
    td.text((C + k * 0.8, C + k), text, font=font, anchor="mm", fill=(10, 10, 14), stroke_width=16, stroke_fill=(10, 10, 14))
td.text((C, C), text, font=font, anchor="mm", fill=(255, 255, 255), stroke_width=16, stroke_fill=(10, 10, 14))
# Füllung: Verlauf rot -> dunkelrot
fill_mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(fill_mask).text((C, C), text, font=font, anchor="mm", fill=255)
grad = np.linspace(0, 1, S)[:, None]
g = np.dstack([np.repeat(255 - 40 * grad, S, 1), np.repeat(70 - 50 * grad, S, 1), np.repeat(80 - 50 * grad, S, 1)])
tl.paste(Image.fromarray(g.astype(np.uint8), "RGB"), (0, 0), fill_mask)
# Glanzkante oben
hl = fill_mask.filter(ImageFilter.MinFilter(9))
hl = ImageChops.subtract(fill_mask, ImageChops.offset(fill_mask, 0, 10))
hl = ImageChops.multiply(hl, fill_mask.filter(ImageFilter.MinFilter(5)))
tl.paste((255, 200, 200, 255), (0, 0), hl)
tl = tl.rotate(8, resample=Image.BICUBIC, center=(C, C))
img.alpha_composite(tl)
img = img.crop(img.getbbox())
img.thumbnail((800, 800), Image.LANCZOS)
img.save(sys.argv[1])
print("ok", img.size)
