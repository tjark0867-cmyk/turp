"""Hintergrund aus Wellpappe (numpy): Höhenkarte + Licht.

Oberfläche: Kraftpapier mit Fasern, Flecken und den durchscheinenden Wellen der Pappe.
An zwei Ecken ist die oberste Papierlage abgerissen – dort sieht man die Wellen
(mit Papierresten auf den Kämmen), einen hellen, faserigen Rissrand und den Schatten der Kante.
"""
import sys
from pathlib import Path

import cv2
import numpy as np
from scipy import ndimage as ndi

OUT = Path(__file__).parent / "out"
OUT.mkdir(exist_ok=True)
W, H = 1080, 2400
rng = np.random.default_rng(7)
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)


def noise(sigma, seed, shape=(H, W)):
    r = np.random.default_rng(seed).standard_normal(shape).astype(np.float32)
    n = ndi.gaussian_filter(r, sigma, mode="wrap")
    return (n - n.mean()) / (n.std() + 1e-6)


def fbm(seed, sigmas=(2, 6, 18, 60, 180), falloff=0.62):
    out = np.zeros((H, W), np.float32)
    amp, tot = 1.0, 0.0
    for i, s in enumerate(sigmas[::-1]):
        out += amp * noise(s, seed + i)
        tot += amp
        amp *= falloff
    return out / tot


def torn_poly(pts, seed, jag=9.0, step=6):
    """Polygon mit gerissenem Rand: dicht abtasten, mehrskalig verschieben."""
    r = np.random.default_rng(seed)
    out = []
    for (x0, y0), (x1, y1) in zip(pts, pts[1:] + pts[:1]):
        n = max(2, int(np.hypot(x1 - x0, y1 - y0) / step))
        for t in np.linspace(0, 1, n, endpoint=False):
            out.append((x0 + (x1 - x0) * t, y0 + (y1 - y0) * t))
    out = np.array(out, np.float32)
    k = len(out)
    d = np.zeros(k, np.float32)
    for scale, a in ((40, 1.0), (10, 0.55), (3, 0.35)):
        c = r.standard_normal(k // scale + 3)
        d += a * np.interp(np.arange(k) / scale, np.arange(len(c)), c)
    # Normale je Punkt
    tang = np.roll(out, -1, 0) - np.roll(out, 1, 0)
    nrm = np.stack([tang[:, 1], -tang[:, 0]], 1)
    nrm /= np.linalg.norm(nrm, axis=1, keepdims=True) + 1e-6
    return out + nrm * (d * jag)[:, None]


def fill(poly):
    m = np.zeros((H, W), np.uint8)
    cv2.fillPoly(m, [np.round(poly * 4).astype(np.int32)], 255, lineType=cv2.LINE_AA, shift=2)
    return m.astype(np.float32) / 255


# ---------------------------------------------------------------- Abrissstellen
peel = np.zeros((H, W), np.float32)
peel = np.maximum(peel, fill(torn_poly([(-60, -60), (360, -60), (300, 70), (215, 150), (120, 205), (40, 300), (-60, 330)], 11, jag=8)))
peel = np.maximum(peel, fill(torn_poly([(1140, 2140), (1140, 2460), (700, 2460), (760, 2370), (860, 2300), (960, 2250)], 23, jag=8)))
# feine Faserkante: Maske mit Rauschen ausfransen
fray = noise(1.2, 99) * 0.5 + noise(3, 98) * 0.5
soft = ndi.gaussian_filter(peel, 2.2)
peel = np.clip((soft - 0.5 + 0.10 * fray) * 7 + 0.5, 0, 1)
liner = 1 - peel

# ---------------------------------------------------------------- Höhenkarte
P = 92.0                      # Wellenabstand in px (senkrechte Wellen)
phase = 2 * np.pi * (xx + 6 * noise(80, 5)) / P
crest = 0.5 + 0.5 * np.cos(phase)              # 1 = Kamm, 0 = Tal

fib_h = noise((0.6, 3.0), 31) * 0.5 + noise(0.8, 33) * 0.5
surface = 1.25 * crest + 0.8 * fbm(40, (20, 60, 160)) + 0.10 * fib_h   # Liner: Wellen schimmern sanft durch
flutes = -16 + 16 * crest ** 1.3 + 0.25 * fib_h                         # freigelegte Wellen

# Papierreste auf den Kämmen (oft bleibt etwas Deckpapier kleben)
rest = ((crest > 0.70 + 0.12 * noise(6, 52)) & (noise((14, 1.6), 51) > 0.45)).astype(np.float32)
rest = ndi.gaussian_filter(rest, 0.8) * peel
flutes = flutes + 2.2 * rest

h = liner * surface + peel * flutes
# Rissrand: Deckpapier hebt sich an der Kante leicht an
dist_in = ndi.distance_transform_edt(liner > 0.5)
rim_w = 7.0 + 4.0 * noise(5, 91)
rim = np.clip(1 - dist_in / np.maximum(rim_w, 2), 0, 1) * liner
h += 1.6 * rim

# ---------------------------------------------------------------- Licht
L = np.array([-0.42, -0.62, 0.66], np.float32)
L /= np.linalg.norm(L)
gy, gx = np.gradient(ndi.gaussian_filter(h, 0.8))
k = 0.9
n = np.stack([-gx * k, -gy * k, np.ones_like(h)], -1)
n /= np.linalg.norm(n, axis=-1, keepdims=True)
diff = np.clip((n @ L) / L[2], 0, 2.2)
shade = 0.40 + 0.60 * diff

# Schlagschatten der Papierkante in die Abrissstellen
shift = ndi.shift(liner, (9, 6), order=1, mode="nearest")
cast = ndi.gaussian_filter(shift, 4) * peel
shade *= 1 - 0.34 * cast
# Täler der Wellen etwas dunkler (Umgebungsverdeckung)
shade *= 1 - 0.18 * peel * (1 - crest) ** 2

# ---------------------------------------------------------------- Farbe
def rgb(c):
    return np.array(c, np.float32) / 255

base = rgb((196, 141, 86))
dark = rgb((168, 112, 62))
light = rgb((214, 166, 110))
blot = fbm(60, (14, 50, 160, 420))
blot2 = noise(260, 61)
t = np.clip(0.5 + 0.38 * blot, 0, 1)[..., None]
col = dark * (1 - t) + light * t
col = col * 0.55 + base * 0.45
col *= (1 + 0.05 * blot2)[..., None]

# Fasern: kurze helle und dunkle Striche, dunkle Pünktchen (Recyclingpapier)
def specks(seed, count, length, color, alpha, thick=1):
    r = np.random.default_rng(seed)
    m = np.zeros((H, W), np.uint8)
    for _ in range(count):
        x, y = r.uniform(0, W), r.uniform(0, H)
        a = r.normal(0, 0.5) + (np.pi / 2 if r.random() < 0.3 else 0)
        ln = r.uniform(*length)
        x2, y2 = x + np.cos(a) * ln, y + np.sin(a) * ln
        cv2.line(m, (int(x * 4), int(y * 4)), (int(x2 * 4), int(y2 * 4)), int(r.uniform(120, 255)), thick, cv2.LINE_AA, shift=2)
    m = m.astype(np.float32) / 255 * alpha
    return m[..., None], rgb(color)

for seed, cnt, ln, c, a in [
    (70, 900, (3, 12), (226, 192, 144), 0.35),
    (71, 1100, (2, 8), (120, 78, 42), 0.40),
    (72, 1400, (0.5, 1.5), (92, 58, 30), 0.85),
]:
    m, c = specks(seed, cnt, ln, c, a)
    col = col * (1 - m * liner[..., None]) + c * m * liner[..., None]

# freigelegte Wellen: Innenpapier heller/gelblicher, Reste etwas grauer
flute_col = rgb((214, 166, 106)) * (1 + 0.06 * noise((1, 8), 80))[..., None]
rest_col = rgb((200, 150, 96))
fc = flute_col * (1 - rest[..., None]) + rest_col * rest[..., None]
col = col * liner[..., None] + fc * peel[..., None]
# Rissrand: helle, faserige Kante
rim_noise = np.clip(rim * (0.75 + 0.6 * noise(1.0, 90)), 0, 1)[..., None]
col = col * (1 - 0.9 * rim_noise) + rgb((240, 216, 178)) * 0.9 * rim_noise

# Aufdruck wie auf Versandkartons: „diese Seite oben“ (zwei Pfeile über einer Linie, mit Rahmen)
def stamp(cx, cy, s, angle, seed):
    S = 4
    m = np.zeros((H * 1, W * 1), np.uint8)
    def P(x, y):
        a = np.deg2rad(angle)
        X, Y = x * np.cos(a) - y * np.sin(a), x * np.sin(a) + y * np.cos(a)
        return (int((cx + X * s) * S), int((cy + Y * s) * S))
    th = max(1, int(0.07 * s * S))
    # Rahmen
    pts = np.array([P(-1, -1.1), P(1, -1.1), P(1, 1.1), P(-1, 1.1)], np.int32)
    cv2.polylines(m, [pts], True, 255, th, cv2.LINE_AA, shift=2)
    for ax in (-0.42, 0.42):
        cv2.line(m, P(ax, 0.55), P(ax, -0.35), 255, int(th * 1.25), cv2.LINE_AA, shift=2)
        head = np.array([P(ax, -0.82), P(ax - 0.27, -0.30), P(ax + 0.27, -0.30)], np.int32)
        cv2.fillPoly(m, [head], 255, cv2.LINE_AA, shift=2)
    cv2.line(m, P(-0.72, 0.78), P(0.72, 0.78), 255, int(th * 1.25), cv2.LINE_AA, shift=2)
    ink = m.astype(np.float32) / 255
    # Stempelfarbe ist nie gleichmäßig: Aussetzer und Verdichtungen
    fine = noise(0.9, seed)
    patchy = noise(10, seed + 1)
    grain = np.clip(0.80 + 0.20 * patchy, 0, 1) * (fine > -1.1 + 0.25 * patchy)   # feine Aussetzer
    return np.clip(ink * grain, 0, 1)

ink = stamp(905, 262, 78, -9, 300)
ink_col = rgb((74, 42, 22))
a = (0.30 * ink * liner)[..., None]
col = col * (1 - a) + ink_col * a

img = col * shade[..., None]

# große Lichtführung: oben-mittig heller, Ränder dunkler
cx, cy = W * 0.48, H * 0.22
r2 = ((xx - cx) / (W * 0.95)) ** 2 + ((yy - cy) / (H * 0.75)) ** 2
glow = 1.10 - 0.30 * np.clip(r2, 0, 1.6) ** 0.9
img *= glow[..., None]

img = np.clip(img, 0, 1)
out = (img * 255 + rng.uniform(-1.2, 1.2, img.shape)).clip(0, 255).astype(np.uint8)
cv2.imwrite(str(OUT / "kraft_bg.png"), cv2.cvtColor(out, cv2.COLOR_RGB2BGR))
ok, buf = cv2.imencode(".webp", cv2.cvtColor(out, cv2.COLOR_RGB2BGR), [cv2.IMWRITE_WEBP_QUALITY, 88])
(OUT / "kraft_bg.webp").write_bytes(buf.tobytes())
print("kraft_bg", out.shape, len(buf) // 1024, "KB")
