"""Oversized-Hoodie in Neongrün und Schwarz (eigenes Design, ohne Logo)."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops
from cloth import mask_from, fabric
from rm import finish

S = 1400
GREEN, GREEN_D = (170, 232, 40), (120, 190, 20)
BLACK, BLACK_D = (48, 48, 54), (26, 26, 30)
can = Image.new("RGBA", (S, S), (0, 0, 0, 0))


def add(img, shadow=True):
    if shadow:
        sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        a = img.getchannel("A").filter(ImageFilter.GaussianBlur(14)).point(lambda v: int(v * 0.35))
        sh.putalpha(a)
        tmp = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        tmp.alpha_composite(sh, (0, 0))
        can.alpha_composite(tmp.transform((S, S), Image.AFFINE, (1, 0, -6, 0, 1, -10)))
    can.alpha_composite(img)


# Ärmel
for side in (-1, 1):
    cx = S / 2
    pts = [(cx + side * 300, 360), (cx + side * 420, 420), (cx + side * 560, 760), (cx + side * 610, 1040),
           (cx + side * 470, 1070), (cx + side * 400, 820), (cx + side * 330, 620)]
    m = mask_from((S, S), pts)
    sl = fabric(m, BLACK, [(cx + side * 360, 450, cx + side * 520, 1000, 26, -16), (cx + side * 420, 520, cx + side * 560, 900, 22, 14)],
                depth=50, sheen=0.15, color_dark=BLACK_D)
    add(sl)
    cuff = mask_from((S, S), [(cx + side * 470, 1010), (cx + side * 612, 990), (cx + side * 620, 1060), (cx + side * 478, 1080)])
    add(fabric(cuff, (36, 36, 40), [], depth=20, sheen=0.1), False)
# Körper
cx = S / 2
body = [(cx - 300, 350), (cx - 120, 320), (cx + 120, 320), (cx + 300, 350), (cx + 330, 640), (cx + 340, 1120),
        (cx, 1140), (cx - 340, 1120), (cx - 330, 640)]
bm = mask_from((S, S), body)
folds = [(cx - 200, 420, cx - 240, 1100, 30, -14), (cx + 200, 420, cx + 230, 1100, 30, -14), (cx - 60, 500, cx - 80, 900, 26, 10),
         (cx + 80, 520, cx + 90, 880, 26, 10)]
add(fabric(bm, GREEN, folds, depth=70, sheen=0.1, color_dark=GREEN_D))
hem = mask_from((S, S), [(cx - 342, 1060), (cx, 1078), (cx + 342, 1060), (cx + 344, 1140), (cx, 1160), (cx - 344, 1140)])
add(fabric(hem, GREEN_D, [], depth=20, sheen=0.05), False)
# Bauchtasche
pocket = [(cx - 200, 800), (cx + 200, 800), (cx + 250, 1020), (cx - 250, 1020)]
pm = Image.new("L", (S, S), 0)
ImageDraw.Draw(pm).polygon(pocket, fill=255)
pm = pm.filter(ImageFilter.GaussianBlur(4)).point(lambda v: 255 if v > 127 else 0)
add(fabric(pm, (160, 222, 36), [], depth=30, sheen=0.08, color_dark=(130, 200, 26)))
# Kapuze (schwarz, liegt hinter dem Nacken)
hood = [(cx - 230, 340), (cx - 200, 230), (cx, 190), (cx + 200, 230), (cx + 230, 340), (cx + 120, 420), (cx, 440), (cx - 120, 420)]
add(fabric(mask_from((S, S), hood), BLACK, [(cx - 150, 260, cx - 60, 400, 20, -14), (cx + 150, 260, cx + 60, 400, 20, -14)],
           depth=45, sheen=0.15, color_dark=BLACK_D))
neck = [(cx - 120, 330), (cx, 300), (cx + 120, 330), (cx, 400)]
nm = mask_from((S, S), neck)
add(fabric(nm, (14, 14, 16), [], depth=30, sheen=0.02), False)
# Kordeln
d = ImageDraw.Draw(can)
for side in (-1, 1):
    x0 = cx + side * 70
    pts = [(x0, 400), (x0 + side * 6, 520), (x0 - side * 4, 640)]
    d.line(pts, fill=(245, 245, 240, 255), width=14, joint="curve")
    d.ellipse((x0 - side * 4 - 11, 630, x0 - side * 4 + 11, 668), fill=(200, 200, 200, 255))
finish(can).save(sys.argv[1])
print("ok")
