"""Kleiner numpy-Raymarcher für selbst erzeugte, realistisch schattierte Sticker-Motive."""
import numpy as np
from PIL import Image


def nz(v):
    v = np.asarray(v, float)
    return v / np.maximum(np.linalg.norm(v, axis=-1, keepdims=True), 1e-9)


def length(v):
    return np.sqrt((v * v).sum(-1))


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


def sd_round_box(p, b, r):
    q = np.abs(p) - (np.asarray(b, float) - r)
    return length(np.maximum(q, 0)) + np.minimum(q.max(-1), 0) - r


def sd_ellipsoid(p, r):
    r = np.asarray(r, float)
    k0 = length(p / r)
    k1 = length(p / (r * r))
    return k0 * (k0 - 1) / np.maximum(k1, 1e-9)


def sd_torus_y(p, R, r):
    """Torus liegend (Achse = y)."""
    q = np.stack([length(p[:, [0, 2]]) - R, p[:, 1]], -1)
    return length(q) - r


def sd_torus_z(p, R, r):
    """Torus stehend (Achse = z), z.B. Henkel."""
    q = np.stack([length(p[:, [0, 1]]) - R, p[:, 2]], -1)
    return length(q) - r


def rot_y(p, a):
    """Welt -> lokal für ein um a (rad) um y gedrehtes Objekt."""
    c, s = np.cos(a), np.sin(a)
    x, y, z = p[:, 0], p[:, 1], p[:, 2]
    return np.stack([c * x - s * z, y, s * x + c * z], -1)


def rot_y_vec(v, a):
    return rot_y(v, a)


def camera(n, view_dir, extent, center):
    d = nz(view_dir)
    up = np.array([0, 1.0, 0])
    right = nz(np.cross(d, up))
    upv = np.cross(right, d)
    u = (np.arange(n) + 0.5) / n * 2 - 1
    U, V = np.meshgrid(u, -u)
    o = np.asarray(center, float) + U.reshape(-1, 1) * extent * right + V.reshape(-1, 1) * extent * upv - d * 30
    return o, d, right, upv


def march(scene, o, d, eps, steps=260, k=0.9, tmax=60):
    n = o.shape[0]
    t = np.zeros(n)
    hit = np.zeros(n, bool)
    act = np.arange(n)
    for _ in range(steps):
        if act.size == 0:
            break
        p = o[act] + t[act, None] * d
        dist = scene(p)[0]
        t[act] += dist * k
        h = dist < eps
        hit[act[h]] = True
        act = act[(~h) & (t[act] < tmax)]
    return t, hit


def normals(scene, p, e=1e-3):
    ks = np.array([[1, -1, -1], [-1, -1, 1], [-1, 1, -1], [1, 1, 1]], float)
    n = np.zeros_like(p)
    for kk in ks:
        n += kk * scene(p + kk * e)[0][:, None]
    return nz(n)


def ambient_occlusion(scene, p, n, scale=1.0):
    occ = np.zeros(p.shape[0])
    w = 1.0
    for i in range(1, 6):
        h = scale * (0.02 + 0.07 * i)
        occ += (h - scene(p + n * h)[0]) * w
        w *= 0.7
    return np.clip(1 - 2.2 * occ / scale, 0, 1)


def soft_shadow(scene, p, L, k=10, steps=48, tmax=6):
    res = np.ones(p.shape[0])
    t = np.full(p.shape[0], 0.02)
    act = np.arange(p.shape[0])
    for _ in range(steps):
        if act.size == 0:
            break
        h = scene(p[act] + L * t[act, None])[0]
        res[act] = np.minimum(res[act], k * h / t[act])
        t[act] += np.clip(h, 0.01, 0.25)
        keep = (h > 1e-4) & (t[act] < tmax)
        res[act[h <= 1e-4]] = 0
        act = act[keep]
    return np.clip(res, 0, 1)


def to_lin(c):
    return (np.asarray(c, float) / 255.0) ** 2.2


def to_srgb(c):
    c = np.clip(c, 0, None)
    c = c / (1 + 0.15 * c)  # sanftes Tonemapping gegen ausgefressene Lichter
    return np.clip(c ** (1 / 2.2) * 255, 0, 255)


def render(scene, shade, n, view_dir, extent, center, eps=6e-4, ss=2, k=0.9):
    N = n * ss
    o, d, right, upv = camera(N, view_dir, extent, center)
    t, hit = march(scene, o, d, eps, k=k)
    rgb = np.zeros((N * N, 3))
    idx = np.where(hit)[0]
    p = o[idx] + t[idx, None] * d
    nrm = normals(scene, p)
    mat = scene(p)[1]
    res = shade(p, nrm, mat, d, right, upv)
    alpha = hit.astype(float)
    if isinstance(res, tuple):
        res, a = res
        alpha[idx] = np.clip(a, 0, 1)
    rgb[idx] = res
    img = np.dstack([rgb.reshape(N, N, 3) * alpha.reshape(N, N, 1), alpha.reshape(N, N) * 255])
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
    # vorgemultipliziert verkleinern -> keine dunklen Säume
    pre = im.resize((n, n), Image.LANCZOS)
    a = np.array(pre).astype(float)
    al = a[..., 3:4] / 255.0
    a[..., :3] = np.where(al > 0.01, a[..., :3] / np.maximum(al, 1e-3), 0)
    out = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    return out.crop(out.getbbox())


# ---------- gemeinsame Beleuchtung / Materialien ----------
class Studio:
    """Licht-Setup: Hauptlicht oben links, Himmel, AO, weiche Schatten."""

    def __init__(self, scene, p, n, d, right, upv, ao_scale=0.4, shadow=True):
        self.V = -d
        self.d = d
        self.right, self.upv = right, upv
        self.L = nz(-0.55 * right + 0.8 * upv + 0.45 * self.V)
        self.L2 = nz(0.75 * right - 0.05 * upv + 0.5 * self.V)
        self.n = n
        self.ao = ambient_occlusion(scene, p, n, ao_scale)
        self.sh = soft_shadow(scene, p + n * 0.003, self.L, k=12) if shadow else np.ones(p.shape[0])
        self.dif = np.clip((n * self.L).sum(-1), 0, 1) * self.sh
        self.dif2 = np.clip((n * self.L2).sum(-1), 0, 1)
        self.sky = 0.5 + 0.5 * (n * upv).sum(-1)
        self.H = nz(self.L + self.V)
        self.ndv = np.clip((n * self.V).sum(-1), 0, 1)
        self.R = d - 2 * (d * n).sum(-1, keepdims=True) * n

    def plastic(self, m, base, spec=0.6, power=60, fres=0.12):
        """base: lineare Farbe (3,) oder (k,3) für Maske m."""
        b = np.broadcast_to(np.asarray(base, float), (m.sum(), 3))
        lit = 1.2 * self.dif[m] + 0.2 * self.dif2[m] + 0.32 * self.sky[m] * self.ao[m]
        s = np.clip((self.n[m] * self.H).sum(-1), 0, 1) ** power * spec * self.sh[m]
        f = (1 - self.ndv[m]) ** 4 * fres * self.ao[m]
        return b * lit[:, None] + (s + f)[:, None]

    def env(self, m, strength=1.0):
        R = self.R[m]
        ry = (R * self.upv).sum(-1)
        rx = (R * self.right).sum(-1)
        hor = smoothstep(-0.25, 0.25, ry)
        e = 0.18 * (1 - hor) + (0.75 + 0.5 * np.clip(ry, 0, 1)) * hor
        e *= 1 - 0.45 * np.exp(-((ry - 0.04) / 0.1) ** 2)
        e += 2.5 * smoothstep(0.86, 0.97, (R * self.L).sum(-1))
        e += 1.2 * np.exp(-((rx + 0.42) / 0.08) ** 2) * smoothstep(-0.3, 0.0, ry)
        return e * strength

    def metal(self, m, tint, strength=1.0):
        e = self.env(m, strength)
        return np.asarray(tint, float) * (e * (0.45 + 0.55 * self.sh[m]) * (0.45 + 0.55 * self.ao[m]))[:, None]

    def glossy(self, m, base, refl=0.25):
        """Lackiert / Glas-artig: Grundfarbe + Spiegelung nach Fresnel."""
        f = 0.04 + 0.96 * (1 - self.ndv[m]) ** 5
        return self.plastic(m, base, spec=0.9, power=90) + (self.env(m) * (0.05 + refl * f))[:, None]


def sd_cyl_y(p, y0, y1, r):
    q = length(p[:, [0, 2]]) - r
    return np.maximum(q, np.maximum(y0 - p[:, 1], p[:, 1] - y1))


def sd_cyl_z(p, z0, z1, r):
    q = length(p[:, [0, 1]]) - r
    return np.maximum(q, np.maximum(z0 - p[:, 2], p[:, 2] - z1))


def rot_x(p, a):
    c, s = np.cos(a), np.sin(a)
    x, y, z = p[:, 0], p[:, 1], p[:, 2]
    return np.stack([x, c * y + s * z, -s * y + c * z], -1)


def rot_z(p, a):
    c, s = np.cos(a), np.sin(a)
    x, y, z = p[:, 0], p[:, 1], p[:, 2]
    return np.stack([c * x + s * y, -s * x + c * y, z], -1)


def finish(im, size=800):
    """Auf Inhalt zuschneiden und verkleinern."""
    from PIL import Image as _I
    bb = im.getchannel("A").point(lambda v: 255 if v > 24 else 0).getbbox()
    im = im.crop(bb)
    im.thumbnail((size, size), _I.LANCZOS)
    return im


def glow_layer(mask_img, color, radius, strength=1.0):
    """Weiches Leuchten aus einer L-Maske."""
    from PIL import Image as _I, ImageFilter as _F
    g = mask_img.filter(_F.GaussianBlur(radius))
    layer = _I.new("RGBA", mask_img.size, tuple(color) + (0,))
    layer.putalpha(g.point(lambda v: min(255, int(v * strength))))
    return layer


def sd_capsule(p, a, b, r):
    a, b = np.asarray(a, float), np.asarray(b, float)
    pa, ba = p - a, b - a
    h = np.clip((pa @ ba) / (ba @ ba), 0, 1)
    return length(pa - h[:, None] * ba) - r


def sd_box2(px, py, bx, by):
    dx, dy = np.abs(px) - bx, np.abs(py) - by
    return np.hypot(np.maximum(dx, 0), np.maximum(dy, 0)) + np.minimum(np.maximum(dx, dy), 0)


def sd_slab(q, w, h, t, rc, re):
    """Flaches Gerät (Handy/Tablet): Ecken mit Radius rc, Kanten mit re. q lokal, Dicke 2t entlang z."""
    d2 = sd_box2(q[:, 0], q[:, 1], w - rc, h - rc) - rc
    wx, wy = d2 + re, np.abs(q[:, 2]) - t + re
    return np.hypot(np.maximum(wx, 0), np.maximum(wy, 0)) + np.minimum(np.maximum(wx, wy), 0) - re
