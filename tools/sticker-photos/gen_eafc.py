"""Goldene Fußball-Spielerkarte mit Wertung 99 und Ball (eigenes Design)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops
from rm import render, finish
import gen_ball

W, H = 700, 980
fb = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf", 190)
fm = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf", 70)
fs = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf", 40)


def shape(w, h, inset=0):
    c = 60 + inset * 0.4
    return [(inset + c, inset), (w - inset - c, inset), (w - inset, inset + c * 1.2), (w - inset, h - inset - 140),
            (w / 2, h - inset), (inset, h - inset - 140), (inset, inset + c * 1.2)]


mask = Image.new("L", (W, H), 0)
ImageDraw.Draw(mask).polygon(shape(W, H), fill=255)
mask = mask.filter(ImageFilter.GaussianBlur(2)).point(lambda v: 255 if v > 127 else 0)
yy, xx = np.mgrid[0:H, 0:W].astype(float)
t = (xx * 0.6 + yy) / (W * 0.6 + H)
band = 0.5 + 0.5 * np.sin(t * np.pi * 3.2 + 0.6)
c_dark, c_light = np.array([176, 120, 30]), np.array([255, 226, 130])
rgb = c_dark * (1 - band[..., None]) + c_light * band[..., None]
sheen = np.exp(-(((xx - yy * 0.55) - 140) / 70) ** 2) * 0.5
rgb = rgb + 255 * sheen[..., None] * 0.6
card = Image.fromarray(np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), np.array(mask)]), "RGBA")
d = ImageDraw.Draw(card)
d.polygon(shape(W, H, 26), outline=(120, 78, 10, 255), width=6)
d.polygon(shape(W, H, 38), outline=(255, 240, 190, 200), width=3)
ink = (70, 44, 6, 255)
d.text((150, 230), "99", font=fb, anchor="mm", fill=ink)
d.text((150, 370), "ST", font=fm, anchor="mm", fill=ink)
d.line([(95, 430), (205, 430)], fill=ink, width=5)
# Werte-Balken unten
for i in range(3):
    for j in range(2):
        x0, y0 = 120 + j * 250, 640 + i * 62
        d.text((x0, y0), "99", font=fs, anchor="lm", fill=ink)
        d.rounded_rectangle((x0 + 70, y0 - 10, x0 + 70 + 120 - 15 * ((i + j) % 3), y0 + 10), 8, fill=(120, 78, 10, 255))
d.line([(110, 600), (W - 110, 600)], fill=ink, width=4)
# Ball
ball = render(gen_ball.scene, gen_ball.shade, 330, view_dir=(0, 0, -1), extent=1.02, center=(0, 0, 0))
sh = Image.new("RGBA", card.size, (0, 0, 0, 0))
bm = Image.new("L", card.size, 0)
bm.paste(ball.getchannel("A"), (370, 170))
sh.putalpha(bm.filter(ImageFilter.GaussianBlur(14)).point(lambda v: int(v * 0.45)))
card.alpha_composite(sh, (8, 14)) if False else card.alpha_composite(sh)
card.alpha_composite(ball, (360, 150))
# Sterne
for k in range(5):
    cx = W / 2 - 120 + k * 60
    r = 22
    pts = []
    for i in range(10):
        rr = r if i % 2 == 0 else r * 0.45
        a = np.radians(-90 + i * 36)
        pts.append((cx + rr * np.cos(a), 535 + rr * np.sin(a)))
    d.polygon(pts, fill=(255, 250, 220, 255), outline=(120, 78, 10, 255))
card.putalpha(ImageChops.multiply(card.getchannel("A"), mask))
can = Image.new("RGBA", (W + 200, H + 200), (0, 0, 0, 0))
can.alpha_composite(card, (100, 100))
can = can.rotate(-8, resample=Image.BICUBIC)
finish(can).save(sys.argv[1])
print("ok")
