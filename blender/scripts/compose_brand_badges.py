"""Compone los logotipos de marca del catálogo (brand/logo.png) a partir de las máscaras de render_svg_mask.py.

El logotipo se ve sobre negro (animación de arranque) y sobre fondo claro (barra lateral de Inicio), así que cada
uno se compone como una insignia que se lee en los dos, igual que el óvalo de Ford:
- Volkswagen (logo 2019): disco azul VW (#001E50) con la W y el aro en blanco, y un aro exterior plateado fino.
- Opel (logo 2023): disco amarillo Opel con el aro y el rayo en negro (el rayo se recorta en el borde del disco).

Uso: python blender/scripts/compose_brand_badges.py <carpeta_mascaras> <vw_logo.png> <opel_logo.png>
Requiere Pillow y numpy.
"""
import sys

import numpy as np
from PIL import Image

masks, vw_out, opel_out = sys.argv[1], sys.argv[2], sys.argv[3]
SS = 2048            # resolución de trabajo; la salida se reduce a 1024 px con buen antialias


def disc(n, cx, cy, r):
    """Disco con borde suave (1 px) en un lienzo n×m."""
    h, w = n
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt((xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2)
    return np.clip(r - d + 0.5, 0, 1)


def over(dst, rgb, a):
    """Pinta el color rgb con opacidad a (0..1) sobre dst (float RGBA)."""
    a = a[..., None]
    col = np.array(rgb, dtype=np.float32)[None, None, :] / 255.0
    dst[..., :3] = col * a + dst[..., :3] * (1 - a)
    dst[..., 3:4] = a + dst[..., 3:4] * (1 - a)


def save(arr, path, size=1024):
    im = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA")
    # Alfa premultiplicado al reducir: evita halos oscuros en el borde.
    im = im.resize((size, round(size * im.height / im.width)), Image.LANCZOS)
    im.save(path, optimize=True)
    print("CV_LOGO", path, im.size)


# ---------------------------------------------------------------- Volkswagen
m = np.asarray(Image.open(f"{masks}/vw_mask.png").convert("RGBA"), dtype=np.float32)[..., 3] / 255.0
n = m.shape[0]
pad = int(n * 0.045)                  # sitio para el aro plateado
N = n + 2 * pad
c = N / 2
r_logo = n / 2
canvas = np.zeros((N, N, 4), np.float32)
over(canvas, (186, 192, 201), disc((N, N), c, c, r_logo + pad * 0.85))          # aro exterior plateado
over(canvas, (120, 128, 140), disc((N, N), c, c, r_logo + pad * 0.85) - disc((N, N), c, c, r_logo + pad * 0.62))
# La máscara del SVG (relleno par-impar al importarlo) son los trazos de la W y el aro: van en blanco; el resto del
# disco, en azul VW (así es el logotipo 2019: disco azul, W y aro blancos).
mm = np.zeros((N, N), np.float32)
mm[pad:pad + n, pad:pad + n] = m
full = disc((N, N), c, c, r_logo)
over(canvas, (255, 255, 255), full)
over(canvas, (0, 30, 80), full * (1 - mm))                                       # azul VW
save(canvas, vw_out)

# ---------------------------------------------------------------- Opel
m = np.asarray(Image.open(f"{masks}/opel_mask.png").convert("RGBA"), dtype=np.float32)[..., 3] / 255.0
h, w = m.shape
# Aro del emblema: ocupa toda la altura de la máscara y está centrado en ella (ver CV_PATH 5 y 6).
scale = w / (0.1283 - 0.0006)
cx = (0.06415 - 0.0006) * scale
cy = h / 2
r_ring = (0.1037 - 0.0246) / 2 * scale
R = r_ring * 1.13                     # disco amarillo algo mayor que el aro
size = int(2 * R) + 8
canvas = np.zeros((size, size, 4), np.float32)
ox, oy = cx - size / 2, cy - size / 2
crop = np.zeros((size, size), np.float32)
x0, y0 = int(round(ox)), int(round(oy))
for yy in range(size):
    sy = yy + y0
    if 0 <= sy < h:
        xs = slice(max(0, x0), min(w, x0 + size))
        crop[yy, xs.start - x0:xs.stop - x0] = m[sy, xs]
d = disc((size, size), size / 2, size / 2, R)
over(canvas, (247, 255, 20), d)                                                  # amarillo Opel
over(canvas, (10, 10, 10), crop * d)                                             # aro y rayo, recortados al disco
save(canvas, opel_out)
