"""Hilfen für Stoff-Illustrationen: glatte Kurven, Falten, weiche Plastizität."""
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops
from scipy.ndimage import distance_transform_edt, gaussian_filter


def catmull(pts, n=16, closed=True):
    P = np.array(pts, float)
    if closed:
        P = np.vstack([P[-1], P, P[0], P[1]])
    else:
        P = np.vstack([P[0], P, P[-1]])
    out = []
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
        for t in np.linspace(0, 1, n, endpoint=False):
            t2, t3 = t * t, t * t * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    if not closed:
        out.append(P[-2])
    return [tuple(p) for p in out]


def mask_from(size, pts):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).polygon(catmull(pts), fill=255)
    return m


def fabric(mask, color, folds=(), depth=60, light=(-0.5, -0.65, 0.6), sheen=0.12, color_dark=None):
    """folds: Liste (x0,y0,x1,y1,breite,stärke) – Faltenlinien (Tal = negativ, Grat = positiv)."""
    m = np.array(mask) > 127
    h, w = m.shape
    dist = distance_transform_edt(m)
    hgt = np.sin(np.clip(dist / depth, 0, 1) * np.pi / 2) * depth * 0.6
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    for x0, y0, x1, y1, wd, st in folds:
        dx, dy = x1 - x0, y1 - y0
        L2 = dx * dx + dy * dy
        t = np.clip(((xx - x0) * dx + (yy - y0) * dy) / L2, 0, 1)
        d = np.hypot(xx - (x0 + t * dx), yy - (y0 + t * dy))
        fade = np.sin(t * np.pi) ** 0.6
        hgt += st * np.exp(-(d / wd) ** 2) * fade
    hgt = gaussian_filter(hgt, 2)
    gy, gx = np.gradient(hgt)
    nx, ny, nz = -gx, -gy, np.ones_like(gx)
    ln = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    nx, ny, nz = nx / ln, ny / ln, nz / ln
    L = np.array(light, float); L /= np.linalg.norm(L)
    dif = np.clip(nx * L[0] + ny * L[1] + nz * L[2], 0, 1)
    Hh = L + np.array([0, 0, 1.0]); Hh /= np.linalg.norm(Hh)
    spec = np.clip(nx * Hh[0] + ny * Hh[1] + nz * Hh[2], 0, 1) ** 12 * sheen
    c = np.array(color, float)
    base = np.broadcast_to(c, (h, w, 3)).copy()
    if color_dark is not None:
        t = np.linspace(0, 1, h)[:, None, None]
        base = c[None, None] * (1 - t) + np.array(color_dark, float)[None, None] * t
    # Stoffstruktur
    rng = np.random.default_rng(0)
    weave = 1 + 0.025 * np.sin(xx * 1.7) * np.sin(yy * 1.7) + rng.normal(0, 0.015, (h, w))
    rgb = base * ((0.35 + 0.85 * dif) * weave)[..., None] + 255 * spec[..., None]
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), (m * 255).astype(np.uint8)]), "RGBA")
