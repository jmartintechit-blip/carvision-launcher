"""Recorta un logotipo OVALADO (p. ej. Ford) de una imagen sin transparencia real
(fondo blanco o "cuadros de transparencia" pintados en la propia imagen).

Método: detecta los píxeles del logotipo (no grises claros), ajusta la elipse exterior y genera un canal alfa
con borde suavizado por supermuestreo. Se encoge el borde ~1,5 px para no arrastrar restos del fondo.

Requiere Pillow.  Uso:  python blender/scripts/extract_oval_logo.py <entrada> <salida.png> [ancho_salida]
"""
import sys

from PIL import Image, ImageFilter

src, dst = sys.argv[1], sys.argv[2]
out_w = int(sys.argv[3]) if len(sys.argv) > 3 else 0

im = Image.open(src).convert("RGB")
W, H = im.size
px = im.load()


def is_logo(p):
    r, g, b = p
    bright = (r + g + b) / 3
    chroma = max(r, g, b) - min(r, g, b)
    # Fondo: grises claros (blanco / gris de los cuadros). Logotipo: azul o zonas oscuras.
    return chroma > 40 or bright < 170


xs, ys = [], []
for y in range(H):
    for x in range(W):
        if is_logo(px[x, y]):
            xs.append(x)
            ys.append(y)
if not xs:
    raise SystemExit("No se detecta el logotipo")

# Bordes robustos (descarta píxeles sueltos): percentiles muy extremos.
xs.sort(); ys.sort()
k = max(1, len(xs) // 20000)
x0, x1 = xs[k], xs[-k - 1]
y0, y1 = ys[k], ys[-k - 1]
cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
SHRINK = 1.5
a, b = (x1 - x0) / 2 + 0.5 - SHRINK, (y1 - y0) / 2 + 0.5 - SHRINK
print(f"elipse centro=({cx:.1f},{cy:.1f}) semiejes=({a:.1f},{b:.1f})")

# Alfa supermuestreado 4x4.
SS = 4
alpha = Image.new("L", (W, H), 0)
ap = alpha.load()
offs = [(i + 0.5) / SS for i in range(SS)]
for y in range(int(cy - b) - 2, int(cy + b) + 3):
    for x in range(int(cx - a) - 2, int(cx + a) + 3):
        if not (0 <= x < W and 0 <= y < H):
            continue
        inside = 0
        for oy in offs:
            for ox in offs:
                dx = (x + ox - 0.5 - cx) / a
                dy = (y + oy - 0.5 - cy) / b
                if dx * dx + dy * dy <= 1.0:
                    inside += 1
        ap[x, y] = round(255 * inside / (SS * SS))

out = im.convert("RGBA")
out.putalpha(alpha)
box = (int(cx - a) - 2, int(cy - b) - 2, int(cx + a) + 3, int(cy + b) + 3)
out = out.crop(box)
if out_w and out_w != out.width:
    h = round(out.height * out_w / out.width)
    out = out.resize((out_w, h), Image.LANCZOS)
    out = out.filter(ImageFilter.UnsharpMask(radius=1.2, percent=60, threshold=2))
out.save(dst, optimize=True)
print("CV_LOGO_OK", dst, out.size)
