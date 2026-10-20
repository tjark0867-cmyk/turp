"""Grüner Kapuzenumhang von hinten, leicht im Wind (eigenes Design, ohne Wappen)."""
import sys
import numpy as np
from PIL import Image, ImageDraw
from cloth import mask_from, fabric
from rm import finish

S = 1300
body = [(430, 300), (650, 260), (870, 300), (980, 420), (1040, 650), (1100, 900), (1150, 1080),
        (1040, 1060), (960, 1120), (860, 1080), (760, 1140), (660, 1090), (560, 1140), (460, 1080),
        (360, 1120), (270, 1050), (170, 1080), (230, 880), (290, 640), (330, 420)]
m = mask_from((S, S), body)
folds = [(520, 420, 300, 1060, 34, -24), (600, 430, 470, 1080, 34, 22), (650, 440, 650, 1100, 36, -24),
         (700, 430, 840, 1080, 34, 22), (780, 420, 1020, 1050, 34, -24), (860, 400, 1110, 1010, 30, 18),
         (460, 400, 230, 980, 30, 18)]
cloak = fabric(m, (54, 124, 80), folds, depth=60, sheen=0.08, color_dark=(30, 82, 54))
# Kapuze, über die Schultern gefallen
hood = [(470, 200), (650, 165), (830, 200), (905, 300), (850, 430), (730, 560), (650, 640), (570, 560),
        (450, 430), (395, 300)]
hm = mask_from((S, S), hood)
hood_img = fabric(hm, (60, 134, 88), [(560, 240, 600, 520, 26, -20), (740, 240, 700, 520, 26, -20), (650, 210, 650, 600, 22, 14), (470, 300, 830, 300, 20, -10)],
                  depth=55, sheen=0.08, color_dark=(40, 100, 64))
# Saum-Naht
seam = Image.new("RGBA", (S, S), (0, 0, 0, 0))
can = Image.new("RGBA", (S, S), (0, 0, 0, 0))
can.alpha_composite(cloak)
# Schatten unter der Kapuze
sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
from PIL import ImageFilter, ImageChops
sm = hm.filter(ImageFilter.GaussianBlur(25)).point(lambda v: int(v * 0.5))
sh.putalpha(ImageChops.multiply(ImageChops.offset(sm, 0, 22), m))
can.alpha_composite(sh)
can.alpha_composite(hood_img)
finish(can).save(sys.argv[1])
print("ok")
