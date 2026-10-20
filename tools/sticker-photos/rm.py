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
