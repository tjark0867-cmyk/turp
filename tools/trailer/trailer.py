"""kumi-Trailer (9:16, 15 s) aus den eigenen App-Grafiken – für TikTok/Reels. Ohne Ton.

Aufruf: python3 trailer.py <ausgabe.mp4>
"""
import math
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res/drawable-nodpi"
BG_SRC = ROOT / "tools/paper/out/app_bg.png"
W, H, FPS, SECONDS = 1080, 1920, 30, 15
FONT = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"
INK = (43, 26, 12)

OUT = sys.argv[1] if len(sys.argv) > 1 else "kumi_trailer.mp4"


def load(name, size=None):
    im = Image.open(RES / f"{name}.webp").convert("RGBA")
    if size:
        im = im.resize(size, Image.LANCZOS)
    return im


def font(sz):
    return ImageFont.truetype(FONT, sz)


# ---------------------------------------------------------------- Easing
def clamp(x, a=0.0, b=1.0):
    return max(a, min(b, x))


def ease_out_back(t, s=1.70158):
    t = clamp(t) - 1
    return t * t * ((s + 1) * t + s) + 1


def ease_out(t):
    t = clamp(t)
    return 1 - (1 - t) ** 3


def ease_in(t):
    t = clamp(t)
    return t ** 3


def prog(t, a, b):
    return clamp((t - a) / (b - a))


# ---------------------------------------------------------------- Bausteine
bg_full = Image.open(BG_SRC).convert("RGBA") if BG_SRC.exists() else Image.new("RGBA", (W, 2400), (196, 141, 86, 255))
bg = bg_full.resize((W, int(bg_full.height * W / bg_full.width))).crop((0, 0, W, H))
logo = load("app_logo")
tag_teal = load("tag_teal")
tag_white = load("tag_white")
yes = load("swipe_yes")
no = load("swipe_no")
letter = load("item_letter")


def make_heart(sz=90, col=(226, 58, 85, 255)):
    im = Image.new("RGBA", (sz * 4, sz * 4), (0, 0, 0, 0))
    pts = []
    for k in range(120):
        a = k / 120 * 2 * math.pi
        x = 16 * math.sin(a) ** 3
        y = -(13 * math.cos(a) - 5 * math.cos(2 * a) - 2 * math.cos(3 * a) - math.cos(4 * a))
        pts.append((sz * 2 + x * sz / 9, sz * 2 + y * sz / 9))
    ImageDraw.Draw(im).polygon(pts, fill=col)
    return im.resize((sz, sz), Image.LANCZOS)


heart = make_heart()

STICKERS = ["basketball", "katzen", "singen", "reisen", "lesen", "kaffee", "fussball", "klavier",
            "tanzen", "fotografie", "hunde", "gaming", "kochen", "skaten", "zeichnen", "musik",
            "filme", "natur", "schach", "surfen", "backen", "pflanzen", "rock", "weltall"]
stk = {n: load(f"interest_{n}") for n in STICKERS if (RES / f"interest_{n}.webp").exists()}


def paste(canvas, im, cx, cy, scale=1.0, rot=0.0, alpha=1.0):
    if scale <= 0.01 or alpha <= 0.01:
        return
    w, h = max(1, int(im.width * scale)), max(1, int(im.height * scale))
    s = im.resize((w, h), Image.BICUBIC)
    if rot:
        s = s.rotate(rot, resample=Image.BICUBIC, expand=True)
    if alpha < 1:
        a = s.getchannel("A").point(lambda v: int(v * alpha))
        s.putalpha(a)
    canvas.alpha_composite(s, (int(cx - s.width / 2), int(cy - s.height / 2)))


def shadow(im, blur=14, off=(0, 12), op=0.35):
    a = im.getchannel("A").point(lambda v: int(v * op))
    sh = Image.new("RGBA", (im.width + 80, im.height + 80), (0, 0, 0, 0))
    blk = Image.new("RGBA", im.size, (0, 0, 0, 255))
    blk.putalpha(a)
    sh.alpha_composite(blk, (40 + off[0], 40 + off[1]))
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    sh.alpha_composite(im, (40, 40))
    return sh


def label(text, size=58, teal=True, pad=(56, 34), max_w=980):
    """Text auf einem ausgerissenen Pappschild (wie die Schilder in der App)."""
    f = font(size)
    lines, cur = [], ""
    for word in text.split():
        test = (cur + " " + word).strip()
        if f.getlength(test) > max_w - pad[0] * 2 and cur:
            lines.append(cur)
            cur = word
        else:
            cur = test
    lines.append(cur)
    lh = int(size * 1.18)
    tw = int(max(f.getlength(l) for l in lines)) + pad[0] * 2
    th = lh * len(lines) + pad[1] * 2
    base = (tag_teal if teal else tag_white).resize((tw, th), Image.LANCZOS)
    d = ImageDraw.Draw(base)
    for i, l in enumerate(lines):
        d.text(((tw - f.getlength(l)) / 2, pad[1] + i * lh - size * 0.08), l, font=f, fill=INK)
    return shadow(base, blur=10, off=(0, 8), op=0.3)


def profile_card(name, interests, accent):
    """Gezeichnete Profilkarte (ohne Foto): Papier, Namensschild, Interessen-Sticker."""
    cw, ch = 760, 1040
    card = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    d = ImageDraw.Draw(card)
    d.rounded_rectangle((0, 0, cw, ch), 44, fill=(251, 244, 230, 255))
    d.rounded_rectangle((0, 0, cw, 430), 44, fill=accent)
    d.rectangle((0, 380, cw, 430), fill=accent)
    # Silhouette als Platzhalter-Avatar
    cx = cw // 2
    d.ellipse((cx - 95, 90, cx + 95, 280), fill=(255, 255, 255, 120))
    d.pieslice((cx - 190, 260, cx + 190, 640), 180, 360, fill=(255, 255, 255, 120))
    nm = label(name, size=60, teal=False, pad=(40, 22))
    card.alpha_composite(nm, (40 - 40, 380 - 40))
    for i, n in enumerate(interests[:6]):
        s = stk[n].resize((200, 200), Image.LANCZOS).rotate((-8, 6, -4, 9, -6, 4)[i], resample=Image.BICUBIC, expand=True)
        x = 55 + (i % 3) * 230
        y = 520 + (i // 3) * 250
        card.alpha_composite(s, (x, y))
    return shadow(card, blur=22, off=(0, 18), op=0.4)


cards = [
    profile_card("Lena, 15", ["basketball", "katzen", "singen", "reisen", "lesen", "kaffee"], (45, 225, 194, 255)),
    profile_card("Mia, 14", ["fussball", "gaming", "zeichnen", "hunde", "musik", "filme"], (255, 201, 77, 255)),
    profile_card("Tom, 16", ["skaten", "rock", "schach", "weltall", "kochen", "fotografie"], (156, 140, 255, 255)),
]

cap1 = label("Finde Freunde, die das Gleiche lieben wie du")
cap2 = label("Wisch durch Leute mit deinen Interessen", teal=False)
cap3 = label("Schreib schon vorher: mit einem Brief")
cap4 = label("Über 170 Interessen", teal=False)
cta = label("Jetzt kostenlos holen", size=66)
small = label("ab 13 · Goldene Regel: Sei immer nett", size=38, teal=False, pad=(40, 22))


def caption(canvas, cap, t, a, b, y=1620):
    p_in = ease_out_back(prog(t, a, a + 0.45))
    p_out = ease_in(prog(t, b - 0.3, b))
    if p_in <= 0 or p_out >= 1:
        return
    paste(canvas, cap, W / 2, y + 260 * p_out, scale=0.6 + 0.4 * p_in, rot=-2 + 2 * p_in, alpha=1 - p_out)


def frame(t):
    c = bg.copy()

    # Szene 1 (0–3 s): Logo + Sticker fliegen rein
    if t < 3.2:
        out = ease_in(prog(t, 2.8, 3.2))
        p = ease_out_back(prog(t, 0.1, 0.8))
        paste(c, logo, W / 2, 760 - 900 * out, scale=0.62 * p, rot=-6 * (1 - p))
        ring = ["basketball", "katzen", "singen", "reisen", "lesen", "kaffee", "fussball", "klavier"]
        for i, n in enumerate(ring):
            a = i / len(ring) * 2 * math.pi + t * 0.35
            r = 390 + 30 * math.sin(t * 2 + i)
            pi = ease_out_back(prog(t, 0.3 + i * 0.12, 0.8 + i * 0.12))
            x = W / 2 + r * math.cos(a)
            y = 760 + r * 1.05 * math.sin(a)
            paste(c, stk[n], x + (x - W / 2) * 2 * out, y + (y - 760) * 2 * out, scale=0.95 * pi, rot=12 * math.sin(t * 1.6 + i))
        caption(c, cap1, t, 0.9, 3.2)

    # Szene 2 (3–7 s): Karten wischen
    elif t < 7.2:
        lt = t - 3.0
        # Karte 3 liegt unten, Karte 2 darüber, Karte 1 oben
        for k, card in reversed(list(enumerate(cards))):
            start = 0.6 + k * 1.3          # wann diese Karte weggewischt wird
            enter = ease_out(prog(lt, 0.0, 0.5))
            sw = ease_in(prog(lt, start, start + 0.55))
            direction = 1 if k != 1 else -1   # Lena rechts (Anfrage), Mia links (weiter), Tom rechts
            if sw >= 1:
                continue
            drag = math.sin(min(1, prog(lt, start - 0.5, start)) * math.pi / 2) * 0.18
            x = W / 2 + direction * (drag + sw * 1.4) * W
            y = 830 + (1 - enter) * 1400 + k * 16
            rot = -direction * (drag + sw) * 22
            paste(c, card, x, y, scale=0.98 - k * 0.02, rot=rot)
            if k == min(i for i in range(3) if ease_in(prog(lt, 0.6 + i * 1.3, 1.15 + i * 1.3)) < 1) and drag + sw > 0.04:
                mark = yes if direction > 0 else no
                paste(c, mark, W / 2 - direction * 250, 500, scale=0.8 * min(1, (drag + sw) * 4), rot=-12 * direction)
        caption(c, cap2, t, 3.3, 7.2)

    # Szene 3 (7–10.5 s): Brief
    elif t < 10.7:
        lt = t - 7.2
        p = ease_out_back(prog(lt, 0.0, 0.7))
        out = ease_in(prog(lt, 3.1, 3.5))
        bob = 18 * math.sin(lt * 3)
        paste(c, letter, W / 2 - 700 * (1 - p) + 900 * out, 780 + bob, scale=2.6, rot=-10 + 10 * p + 8 * math.sin(lt * 2))
        # kleine Herzen aus dem Brief
        for i in range(6):
            hp = prog(lt, 0.8 + i * 0.3, 2.2 + i * 0.3)
            if 0 < hp < 1:
                hx = W / 2 + (i - 2.5) * 120 + 40 * math.sin(lt * 3 + i)
                hy = 640 - 420 * hp
                paste(c, heart, hx, hy, scale=0.9 + 0.3 * hp, rot=15 * math.sin(i + lt * 2), alpha=1 - hp)
        caption(c, cap3, t, 7.4, 10.7)

    # Szene 4 (10.5–13 s): Sticker-Wand
    elif t < 13.0:
        lt = t - 10.7
        names = list(stk)
        for i, n in enumerate(names):
            col, row = i % 4, i // 4
            x = 150 + col * 260 + (60 if row % 2 else 0)
            y = 180 + row * 240
            p = ease_out_back(prog(lt, i * 0.04, 0.4 + i * 0.04))
            paste(c, stk[n], x, y - 300 * (1 - p), scale=1.05 * p, rot=10 * math.sin(i * 1.7 + lt * 2))
        caption(c, cap4, t, 10.9, 13.0, y=1700)

    # Szene 5 (13–15 s): Abschluss
    else:
        lt = t - 13.0
        p = ease_out_back(prog(lt, 0.0, 0.6))
        paste(c, logo, W / 2, 740, scale=0.72 * p, rot=-4 + 4 * p + 2 * math.sin(lt * 4))
        for i, n in enumerate(["katzen", "basketball", "singen", "reisen"]):
            pi = ease_out_back(prog(lt, 0.2 + i * 0.1, 0.7 + i * 0.1))
            x = (220, 860, 180, 900)[i]
            y = (420, 400, 1050, 1060)[i]
            paste(c, stk[n], x, y, scale=0.9 * pi, rot=10 * math.sin(lt * 2 + i))
        q = ease_out_back(prog(lt, 0.4, 0.9))
        paste(c, cta, W / 2, 1300, scale=q, rot=-2)
        paste(c, small, W / 2, 1520, scale=ease_out(prog(lt, 0.7, 1.1)))
    return c.convert("RGB")


proc = subprocess.Popen(
    ["ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS),
     "-i", "-", "-c:v", "libx264", "-preset", "medium", "-crf", "20", "-pix_fmt", "yuv420p", "-movflags", "+faststart", OUT],
    stdin=subprocess.PIPE,
)
for i in range(FPS * SECONDS):
    proc.stdin.write(frame(i / FPS).tobytes())
proc.stdin.close()
proc.wait()
print("ok", OUT)
