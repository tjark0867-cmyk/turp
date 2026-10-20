"""2D-Formen plastisch machen: Höhe aus Abstand zum Rand, Licht von oben links."""
import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt, gaussian_filter


def bevel(mask, color, depth=18, gloss=0.6, power=40, color2=None):
    """mask: L-Bild (0/255), color: RGB (oben), color2: RGB (unten, optional Verlauf)."""
    m = np.array(mask) > 127
    h, w = m.shape
    dist = distance_transform_edt(m)
    hgt = np.clip(dist / depth, 0, 1)
    hgt = np.sin(hgt * np.pi / 2)
    hgt = gaussian_filter(hgt, 1.2)
    gy, gx = np.gradient(hgt)
    nx, ny, nz = -gx * depth * 1.4, -gy * depth * 1.4, np.ones_like(gx)
    ln = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    nx, ny, nz = nx / ln, ny / ln, nz / ln
    L = np.array([-0.5, -0.6, 0.62]); L /= np.linalg.norm(L)
    dif = np.clip(nx * L[0] + ny * L[1] + nz * L[2], 0, 1)
    Hh = L + np.array([0, 0, 1.0]); Hh /= np.linalg.norm(Hh)
    spec = np.clip(nx * Hh[0] + ny * Hh[1] + nz * Hh[2], 0, 1) ** power * gloss
    c1 = np.array(color, float)
    if color2 is not None:
        t = np.linspace(0, 1, h)[:, None, None]
        base = c1[None, None] * (1 - t) + np.array(color2, float)[None, None] * t
    else:
        base = np.broadcast_to(c1, (h, w, 3))
    rgb = base * (0.62 + 0.55 * dif)[..., None] + 255 * spec[..., None]
    a = (m * 255).astype(np.uint8)
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), a]), "RGBA")
