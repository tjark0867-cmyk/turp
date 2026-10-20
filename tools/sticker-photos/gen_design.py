"""Aufgefächerter Farbfächer (Farbmusterkarten) – selbst erzeugt."""
import colorsys
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops
from rm import finish

S = 1800
PIV = (S // 2, S - 300)
L, Wd = 860, 150
HUES = [0.0, 0.07, 0.13, 0.33, 0.47, 0.58, 0.72, 0.85]


def strip(h):
    im = Image.new("RGBA", (Wd, L), (0, 0, 0, 0))
    m = Image.new("L", (Wd, L), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, Wd - 1, L - 1), 22, fill=255)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, Wd - 1, L - 1), 22, fill=(250, 249, 245, 255))
    n = 5
    top, bottom = 18, L - 170
    hgt = (bottom - top) / n
    for i in range(n):
        light = 0.82 - i * 0.13
        sat = 0.45 + i * 0.1
        r, g, b = colorsys.hls_to_rgb(h, light, sat)
        y0 = top + i * hgt
        d.rectangle((14, y0 + 6, Wd - 15, y0 + hgt - 22), fill=(int(r * 255), int(g * 255), int(b * 255), 255))
        d.rectangle((14, y0 + hgt - 16, 14 + 60 + (i * 7) % 30, y0 + hgt - 11), fill=(170, 170, 175, 255))
    d.ellipse((Wd // 2 - 26, L - 100, Wd // 2 + 26, L - 48), fill=(225, 225, 228, 255))
    im.putalpha(ImageChops.multiply(im.getchannel("A"), m))
    # leichter Glanz
    gl = Image.new("L", (Wd, L), 0)
    ImageDraw.Draw(gl).polygon([(0, 0), (Wd, 0), (0, L * 0.4)], fill=40)
    w = Image.new("RGBA", (Wd, L), (255, 255, 255, 0))
    w.putalpha(ImageChops.multiply(gl.filter(ImageFilter.GaussianBlur(20)), m))
    im.alpha_composite(w)
    return im


can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
angles = np.linspace(48, -42, len(HUES))
for h, a in zip(HUES, angles):
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    layer.alpha_composite(strip(h), (PIV[0] - Wd // 2, PIV[1] - L + 74))
    layer = layer.rotate(a, resample=Image.BICUBIC, center=PIV)
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sh.putalpha(layer.getchannel("A").filter(ImageFilter.GaussianBlur(10)).point(lambda v: int(v * 0.35)))
    sh2 = Image.new("RGBA", (S, S), (0, 0, 0, 0)); sh2.alpha_composite(sh.crop((6, 0, S, S - 6)), (0, 6))
    can.alpha_composite(sh2)
    can.alpha_composite(layer)
# Schraube
d = ImageDraw.Draw(can)
d.ellipse((PIV[0] - 34, PIV[1] - 34, PIV[0] + 34, PIV[1] + 34), fill=(150, 152, 160, 255))
d.ellipse((PIV[0] - 22, PIV[1] - 22, PIV[0] + 22, PIV[1] + 22), fill=(205, 207, 214, 255))
d.ellipse((PIV[0] - 14, PIV[1] - 26, PIV[0] + 6, PIV[1] - 8), fill=(245, 245, 250, 255))
finish(can).save(sys.argv[1])
print("ok")
