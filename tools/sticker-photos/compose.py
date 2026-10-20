"""Macht aus freigestellten Fotos Sticker: heller Papierrand, leichte Körnung, weicher Schatten, leicht schräg."""
import random
import zlib
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont, ImageOps

CANVAS = 512          # intern doppelt so groß rendern, am Ende verkleinern
OUT_SIZE = 224
BOX = 372             # maximale Motivgröße im Canvas
BORDER = 15           # Randbreite (bei CANVAS=512)
PAPER = (250, 247, 240)


def paper_noise(size, seed):
    rng = np.random.default_rng(seed)
    n = rng.normal(0, 1, (size[1], size[0])).astype(np.float32)
    img = Image.fromarray(np.clip(128 + n * 22, 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(0.7))
    return img


def make_sticker(cut: Image.Image, seed: int, out_size: int = OUT_SIZE) -> Image.Image:
    r = random.Random(seed)
    cut = cut.convert("RGBA")
    # Motiv auf die Box skalieren
    w, h = cut.size
    s = min(BOX / w, BOX / h)
    cut = cut.resize((max(1, int(w * s)), max(1, int(h * s))), Image.LANCZOS)
    # weiche, saubere Kante des Motivs
    a = cut.getchannel("A").point(lambda v: 0 if v < 40 else v)
    # kleine, abgetrennte Reste vom Freistellen entfernen
    from scipy import ndimage
    arr = np.array(a)
    lab, n = ndimage.label(arr > 60)
    if n > 1:
        sizes = ndimage.sum(np.ones_like(arr), lab, range(1, n + 1))
        keep = {i + 1 for i, sz in enumerate(sizes) if sz >= sizes.max() * 0.06}
        mask = np.isin(lab, list(keep))
        mask = ndimage.binary_dilation(mask, iterations=3)
        arr = np.where(mask, arr, 0).astype(np.uint8)
        a = Image.fromarray(arr, "L")
    a = a.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(0.6))
    cut.putalpha(a)

    layer = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    ox, oy = (CANVAS - cut.width) // 2, (CANVAS - cut.height) // 2
    layer.alpha_composite(cut, (ox, oy))
    alpha = layer.getchannel("A")

    # Rand: Alpha aufblasen (rund), Löcher schließen, kantenglatt
    grown = alpha.filter(ImageFilter.GaussianBlur(BORDER * 0.75)).point(lambda v: 255 if v > 14 else 0)
    grown = grown.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.GaussianBlur(1.4))
    grown = grown.point(lambda v: 0 if v < 100 else min(255, int((v - 100) * 255 / 60)))

    # Papierrand mit leichter Körnung
    paper = Image.new("RGBA", (CANVAS, CANVAS), PAPER + (255,))
    noise = paper_noise((CANVAS, CANVAS), seed)
    grain = Image.merge("RGB", [noise] * 3)
    paper_rgb = ImageChops.multiply(paper.convert("RGB"), Image.eval(grain, lambda v: 215 + v * 40 // 255))
    paper = paper_rgb.convert("RGBA")
    paper.putalpha(grown)

    # Schatten
    shadow = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    sh_alpha = grown.filter(ImageFilter.GaussianBlur(9)).point(lambda v: int(v * 0.42))
    shadow.putalpha(sh_alpha)
    shadow = ImageChops.offset(shadow, 3, 8)

    sticker = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    sticker.alpha_composite(shadow)
    sticker.alpha_composite(paper)
    # ganz leichte Innenkante am Rand des Motivs (wirkt wie aufgeklebt)
    edge = alpha.filter(ImageFilter.GaussianBlur(2)).point(lambda v: int(v * 0.18))
    edge_layer = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    edge_layer.putalpha(ImageChops.subtract(edge, alpha.point(lambda v: int(v * 0.18))))
    sticker.alpha_composite(layer)

    # dezenter Glanz von oben links
    gloss = Image.new("L", (CANVAS, CANVAS), 0)
    d = ImageDraw.Draw(gloss)
    d.polygon([(0, 0), (CANVAS * 0.95, 0), (0, CANVAS * 0.75)], fill=26)
    gloss = gloss.filter(ImageFilter.GaussianBlur(40))
    gl = Image.new("RGBA", (CANVAS, CANVAS), (255, 255, 255, 0))
    gl.putalpha(ImageChops.multiply(gloss, grown))
    sticker.alpha_composite(gl)

    angle = r.uniform(-6, 6)
    sticker = sticker.rotate(angle, resample=Image.BICUBIC)
    bbox = sticker.getchannel("A").getbbox()
    # zentriert zuschneiden, damit alle gleich groß wirken
    if bbox:
        cx, cy = (bbox[0] + bbox[2]) / 2, (bbox[1] + bbox[3]) / 2
        half = max(bbox[2] - bbox[0], bbox[3] - bbox[1]) / 2 + 6
        sticker = sticker.crop((int(cx - half), int(cy - half), int(cx + half), int(cy + half)))
    return sticker.resize((out_size, out_size), Image.LANCZOS)


def sheet(paths, out, cols=8, cell=180, labels=None):
    rows = (len(paths) + cols - 1) // cols
    img = Image.new("RGB", (cols * cell, rows * (cell + 20)), (5, 5, 6))
    d = ImageDraw.Draw(img)
    font = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-Medium.ttf", 13)
    for i, p in enumerate(paths):
        im = Image.open(p).convert("RGBA")
        im.thumbnail((cell - 12, cell - 12))
        x, y = (i % cols) * cell, (i // cols) * (cell + 20)
        img.paste(im, (x + (cell - im.width) // 2, y + 4), im)
        d.text((x + cell // 2, y + cell - 4), (labels or [Path(p).stem for p in paths])[i], fill=(200, 200, 205), font=font, anchor="mt")
    img.save(out)


if __name__ == "__main__":
    src, dst = Path(sys.argv[1]), Path(sys.argv[2])
    dst.mkdir(parents=True, exist_ok=True)
    files = sorted(src.glob("*.png"))
    for i, f in enumerate(files):
        make_sticker(Image.open(f), seed=zlib.crc32(f.stem.encode()) % 10000).save(dst / f"{f.stem}.png")
    sheet(sorted(dst.glob("*.png")), dst.parent / f"{dst.name}_sheet.png")
    print("ok", len(files))
