"""Gebündelte Geldscheine (Fantasie-Scheine, keine echte Währung) – selbst erzeugt."""
import sys
import numpy as np
from rm import nz, rot_y, sd_round_box, render, ambient_occlusion, soft_shadow, to_lin, to_srgb, smoothstep

HX, HY, HZ = 1.0, 0.16, 0.44
BUNDLES = [((0.0, HY, -0.5), 0.0), ((0.05, HY, 0.46), -0.06), ((0.12, 3 * HY + 0.005, 0.0), 0.42)]
BAND_X = 0.17


def parts(p):
    out = []
    for c, a in BUNDLES:
        q = rot_y(p - np.array(c), a)
        bills = sd_round_box(q, (HX, HY, HZ), 0.02)
        band = sd_round_box(q, (BAND_X, HY + 0.008, HZ + 0.008), 0.01)
        out += [bills, band]
    return np.stack(out, -1)


def scene(p):
    D = parts(p)
    return D.min(-1), D.argmin(-1)


def bill_top(lx, lz):
    """Fantasie-Schein von oben: Rahmen, Oval, Guilloche-Linien, Eck-Kreise."""
    pale = to_lin((206, 222, 186))
    mid = to_lin((150, 190, 140))
    dark = to_lin((48, 96, 64))
    col = np.tile(pale, (lx.size, 1))
    ax, az = np.abs(lx), np.abs(lz)
    # Rahmenfläche
    frame = (ax > HX - 0.13) | (az > HZ - 0.09)
    col[frame] = mid
    wave = 0.5 + 0.5 * np.sin(60 * lx + 7 * np.sin(14 * lz))
    col[frame] *= (0.85 + 0.25 * wave[frame])[:, None]
    # Rahmenlinien
    line = (np.abs(ax - (HX - 0.13)) < 0.008) & (az < HZ - 0.09) | (np.abs(az - (HZ - 0.09)) < 0.008) & (ax < HX - 0.13)
    col[line] = dark
    # Oval in der Mitte mit Ringen
    r = np.sqrt((lx / 0.3) ** 2 + (lz / 0.27) ** 2)
    oval = r < 1
    col[oval] = pale * 1.05
    rings = oval & (np.abs(np.sin(r * 28)) < 0.18)
    col[rings] = mid
    col[np.abs(r - 1) < 0.03] = dark
    # Eck-Kreise
    for sx in (-1, 1):
        for sz in (-1, 1):
            rc = np.sqrt((lx - sx * 0.72) ** 2 + (lz - sz * 0.22) ** 2)
            col[(rc < 0.085) & (rc > 0.065)] = dark
            col[rc < 0.05] = mid
    return col


def shade(p, n, mat, d, right, upv):
    V = -d
    L = nz(-0.5 * right + 0.85 * upv + 0.45 * V)
    ao = ambient_occlusion(scene, p, n, 0.4)
    sh = soft_shadow(scene, p + n * 0.003, L, k=14)
    dif = np.clip((n * L).sum(-1), 0, 1) * sh
    sky = 0.5 + 0.5 * n[:, 1]
    col = np.zeros((p.shape[0], 3))
    for i, (c, a) in enumerate(BUNDLES):
        q = rot_y(p - np.array(c), a)
        ln = rot_y(n, a)
        bm = mat == 2 * i
        top = bm & (ln[:, 1] > 0.7)
        side = bm & ~top
        col[top] = bill_top(q[top, 0], q[top, 2])
        # Papierkanten: viele feine Lagen
        layer = (q[side, 1] * 110) % 1
        rng = np.random.default_rng(i)
        jitter = rng.normal(0, 0.04, side.sum())
        col[side] = to_lin((214, 224, 200)) * (0.78 + 0.22 * smoothstep(0.1, 0.6, layer) + jitter)[:, None]
        bd = mat == 2 * i + 1
        col[bd] = to_lin((236, 222, 190))
        stripe = bd & (np.abs(np.abs(q[:, 0]) - (BAND_X - 0.03)) < 0.012)
        col[stripe] = to_lin((170, 60, 50))
    light = 1.15 * dif + 0.35 * sky * ao
    H = nz(L + V)
    spec = np.clip((n * H).sum(-1), 0, 1) ** 30 * sh * 0.12
    return to_srgb(col * light[:, None] + spec[:, None])


if __name__ == "__main__":
    im = render(scene, shade, 720, view_dir=(-0.75, -0.95, -1), extent=1.32, center=(0.06, 0.25, 0.0))
    im.save(sys.argv[1])
    print("ok", im.size)
