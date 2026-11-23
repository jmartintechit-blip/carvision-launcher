"""Convierte iconos SVG de trazo (p. ej. Lucide, licencia ISC) en ImageVector de Jetpack Compose.

Genera un único archivo Kotlin con un objeto `CvIcons`. El grosor de trazo se fija aquí (más fino que el original
para un aspecto más premium). Admite: path, circle, ellipse, rect (con rx/ry), line, polyline, polygon.

Uso: python scripts/svg_icons_to_compose.py <carpeta_svg> <salida.kt> <paquete> <grosor_trazo> nombre_svg=NombreKotlin ...
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

src, out, package, stroke = sys.argv[1], sys.argv[2], sys.argv[3], float(sys.argv[4])
mapping = [a.split("=") for a in sys.argv[5:]]


def f(v):
    return float(v) if v is not None else 0.0


def circle_d(cx, cy, rx, ry):
    return (f"M{cx - rx},{cy} a{rx},{ry} 0 1,0 {2 * rx},0 a{rx},{ry} 0 1,0 {-2 * rx},0 Z")


def rect_d(x, y, w, h, rx, ry):
    if rx == 0 and ry == 0:
        return f"M{x},{y} h{w} v{h} h{-w} Z"
    rx = min(rx or ry, w / 2)
    ry = min(ry or rx, h / 2)
    return (f"M{x + rx},{y} h{w - 2 * rx} a{rx},{ry} 0 0 1 {rx},{ry} v{h - 2 * ry} a{rx},{ry} 0 0 1 {-rx},{ry} "
            f"h{-(w - 2 * rx)} a{rx},{ry} 0 0 1 {-rx},{-ry} v{-(h - 2 * ry)} a{rx},{ry} 0 0 1 {rx},{-ry} Z")


def element_paths(el):
    tag = el.tag.split("}")[-1]
    a = el.attrib
    if tag == "path":
        return [a["d"]]
    if tag == "circle":
        return [circle_d(f(a.get("cx")), f(a.get("cy")), f(a["r"]), f(a["r"]))]
    if tag == "ellipse":
        return [circle_d(f(a.get("cx")), f(a.get("cy")), f(a["rx"]), f(a["ry"]))]
    if tag == "rect":
        return [rect_d(f(a.get("x")), f(a.get("y")), f(a["width"]), f(a["height"]), f(a.get("rx")), f(a.get("ry")))]
    if tag == "line":
        return [f"M{a['x1']},{a['y1']} L{a['x2']},{a['y2']}"]
    if tag in ("polyline", "polygon"):
        pts = re.findall(r"-?[\d.]+", a["points"])
        pairs = [f"{pts[i]},{pts[i + 1]}" for i in range(0, len(pts), 2)]
        d = "M" + " L".join(pairs)
        return [d + (" Z" if tag == "polygon" else "")]
    return []


lines = [
    "// Archivo generado por scripts/svg_icons_to_compose.py. No editar a mano.",
    "// Iconos: Lucide (https://lucide.dev), licencia ISC. Ver docs/third-party/Lucide-LICENSE.txt",
    f"package {package}",
    "",
    "import androidx.compose.ui.graphics.SolidColor",
    "import androidx.compose.ui.graphics.Color",
    "import androidx.compose.ui.graphics.StrokeCap",
    "import androidx.compose.ui.graphics.StrokeJoin",
    "import androidx.compose.ui.graphics.vector.ImageVector",
    "import androidx.compose.ui.graphics.vector.addPathNodes",
    "import androidx.compose.ui.unit.dp",
    "",
    "/** Iconos de línea de CarVision (trazo uniforme, sin aspecto Android). */",
    "object CvIcons {",
    f"    private const val STROKE = {stroke}f",
    "",
    "    private fun icon(name: String, vararg paths: String): ImageVector {",
    "        val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)",
    "        paths.forEach { d ->",
    "            b.addPath(",
    "                pathData = addPathNodes(d),",
    "                fill = null,",
    "                stroke = SolidColor(Color.Black),",
    "                strokeLineWidth = STROKE,",
    "                strokeLineCap = StrokeCap.Round,",
    "                strokeLineJoin = StrokeJoin.Round,",
    "            )",
    "        }",
    "        return b.build()",
    "    }",
    "",
]
for svg_name, kt_name in mapping:
    tree = ET.parse(os.path.join(src, svg_name + ".svg"))
    ds = []
    for el in tree.getroot().iter():
        ds += element_paths(el)
    args = ",\n            ".join('"' + d.replace('"', '') + '"' for d in ds)
    lines.append(f"    val {kt_name}: ImageVector by lazy {{\n        icon(\n            \"{kt_name}\",\n            {args},\n        )\n    }}\n")
lines.append("}")
os.makedirs(os.path.dirname(out), exist_ok=True)
open(out, "w", encoding="utf-8").write("\n".join(lines) + "\n")
print("CV_ICONS", len(mapping), "->", out)
