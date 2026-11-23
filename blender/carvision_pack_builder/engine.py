# SPDX-License-Identifier: GPL-3.0-or-later
# CarVision Pack Builder (complemento de Blender). Ver DECISIONS.md, D-025.
"""Motor del generador de coches de CarVision (sin interfaz).

Convierte el modelo 3D de cualquier coche (.glb/.gltf/.fbx/.obj/.blend) en un Vehicle Pack: modelo con puertas,
capó, portón y ruedas animables, manifest.json, logotipo y .zip listo para importar en la app.

Lo usan el script de línea de comandos (blender/scripts/build_pack.py, para una IA con acceso a archivos) y el panel
de Blender (para personas, guiadas por cualquier IA). Diseño y decisiones: docs/GENERADOR_COCHES.md.

Convenciones del pipeline: metros, Z arriba, frontal hacia -Y, lado izquierdo hacia +X, ruedas apoyadas en z = 0.
"""
import json
import math
import os
import re
import sys
import unicodedata
import zipfile

import bpy
import numpy as np
from mathutils import Matrix, Vector

from . import common as cv

DOORS = ("front_left_door", "front_right_door", "rear_left_door", "rear_right_door")
WHEELS = ("front_left_wheel", "front_right_wheel", "rear_left_wheel", "rear_right_wheel")
OPENINGS = DOORS + ("hood", "rear_hatch")
GROUPS = ("body",) + OPENINGS + WHEELS
SIGNALS = {
    "front_left_door": "door.front_left", "front_right_door": "door.front_right",
    "rear_left_door": "door.rear_left", "rear_right_door": "door.rear_right",
    "hood": "hood", "rear_hatch": "tailgate",
}
LABELS = {
    "body": "Carrocería", "front_left_door": "Puerta delantera izquierda", "front_right_door": "Puerta delantera derecha",
    "rear_left_door": "Puerta trasera izquierda", "rear_right_door": "Puerta trasera derecha", "hood": "Capó",
    "rear_hatch": "Portón / maletero", "front_left_wheel": "Rueda delantera izquierda",
    "front_right_wheel": "Rueda delantera derecha", "rear_left_wheel": "Rueda trasera izquierda",
    "rear_right_wheel": "Rueda trasera derecha",
}

# Pistas de nombre (varios idiomas). El TIPO sale del nombre; la POSICIÓN, siempre de la geometría.
RX = {
    "caliper": re.compile(r"caliper|pinza|brake_?pad|brakepad|bremssattel|etrier|étrier|calliper"),
    "wheel": re.compile(r"wheel|rueda|roue|\brad\b|tire|tyre|neum|(?<![a-z])rim|llanta|felge|reifen|pneu|hubcap|tapacubo"),
    "door": re.compile(r"door|puerta|t[uü]r(?![a-z])|tuer|porte(?!r)|portiera|portier|dvere"),
    "hood": re.compile(r"hood|bonnet|capot|(?<![a-z])cap[oó](?![a-z])|motorhaube|cofano"),
    "trunk": re.compile(r"trunk|(?<![a-z])boot(?![a-z])|tailgate|hatch|maletero|port[oó]n|kofferraum|heckklappe|coffre|"
                        r"bagagliaio|liftgate|decklid"),
    "glass": re.compile(r"glass|window|windshield|windscreen|cristal|vidrio|luna(?![a-z])|scheibe|vitre|vetro"),
    "light": re.compile(r"light|lamp|faro|piloto|leuchte|feu|fanale|lens"),
    "paint": re.compile(r"paint|carpaint|car_paint|body|carroc|lack|pintura|exterior|color ?1|colour"),
    "junk": re.compile(r"ground|floor|shadow|plane|suelo|sombra|backdrop|studio|turntable|camera|platform|plataforma"),
    "front": re.compile(r"headl|head_l|faro|grill|parrilla|windshield|windscreen|parabrisas|hood|bonnet|capot|front"),
    "rear": re.compile(r"taill|tail_l|piloto|trunk|tailgate|maletero|exhaust|escape|rear|heck|boot(?![a-z])"),
}


class Report:
    """Lo que se hizo y lo que hay que revisar. Se guarda como report.json."""

    def __init__(self, ui=False):
        self.steps, self.warnings, self.info = [], [], {}
        self.ui = ui            # True = avisos para el panel de Blender (botones), False = para el config.json

    def say(self, cli, panel):
        """Elige la redacción del aviso: con claves del config (línea de comandos) o con botones del panel."""
        return panel if self.ui else cli

    def step(self, msg):
        print("CV_STEP", msg)
        self.steps.append(msg)

    def warn(self, msg):
        print("CV_WARN", msg)
        self.warnings.append(msg)

    def as_dict(self):
        return {"steps": self.steps, "warnings": self.warnings, **self.info}


# ---------------------------------------------------------------------------------------------- medidas


class Part:
    """Una malla del modelo con sus medidas (coordenadas de mundo; las transformaciones ya están horneadas)."""

    __slots__ = ("obj", "names", "mn", "mx", "c", "e", "area", "mat_area", "verts")

    def __init__(self, obj):
        self.obj = obj
        self.names = obj.get("cv_names", obj.name).lower()
        me = obj.data
        n = len(me.vertices)
        co = np.empty(n * 3, dtype=np.float64)
        me.vertices.foreach_get("co", co)
        co = co.reshape(n, 3)
        self.verts = co
        self.mn, self.mx = Vector(co.min(axis=0)), Vector(co.max(axis=0))
        self.c, self.e = (self.mn + self.mx) / 2, self.mx - self.mn
        pn = len(me.polygons)
        areas = np.empty(pn)
        me.polygons.foreach_get("area", areas)
        idx = np.empty(pn, dtype=np.int32)
        me.polygons.foreach_get("material_index", idx)
        self.area = float(areas.sum())
        self.mat_area = {}
        for i, m in enumerate(me.materials):
            if m is not None:
                a = float(areas[idx == i].sum())
                if a > 0:
                    self.mat_area[m.name] = self.mat_area.get(m.name, 0.0) + a

    @property
    def name(self):
        return self.obj.name


def _n(x):
    """Miles con punto (formato español)."""
    return f"{x:,}".replace(",", ".")


def mesh_objects():
    return [o for o in bpy.data.objects if o.type == 'MESH' and o.data and len(o.data.vertices) > 0]


def measure():
    return [Part(o) for o in mesh_objects()]


def all_bounds(parts):
    mn = Vector((min(p.mn.x for p in parts), min(p.mn.y for p in parts), min(p.mn.z for p in parts)))
    mx = Vector((max(p.mx.x for p in parts), max(p.mx.y for p in parts), max(p.mx.z for p in parts)))
    return mn, mx


def transform_all(m):
    for o in mesh_objects():
        o.data.transform(m)


def _select(objs):
    for o in bpy.context.view_layer.objects:
        o.select_set(False)
    for o in objs:
        o.select_set(True)
    if objs:
        bpy.context.view_layer.objects.active = objs[0]


# ---------------------------------------------------------------------------------------------- 1. importar


def clear_scene():
    """Vacía la escena sin reiniciar Blender (desde el panel no se puede usar read_factory_settings: descargaría
    el propio complemento)."""
    if bpy.context.object and bpy.context.object.mode != 'OBJECT':
        bpy.ops.object.mode_set(mode='OBJECT')
    for o in list(bpy.data.objects):
        bpy.data.objects.remove(o)
    for coll in (bpy.data.meshes, bpy.data.materials, bpy.data.images, bpy.data.cameras, bpy.data.lights,
                 bpy.data.curves):
        for d in list(coll):
            if d.users == 0:
                coll.remove(d)


def import_model(path, rep, reset=True):
    """Importa el modelo, hornea transformaciones y deja solo mallas sueltas (sin jerarquía)."""
    if reset:
        bpy.ops.wm.read_factory_settings(use_empty=True)
    else:
        clear_scene()
    ext = os.path.splitext(path)[1].lower()
    if ext in (".glb", ".gltf"):
        bpy.ops.import_scene.gltf(filepath=path)
    elif ext == ".fbx":
        bpy.ops.import_scene.fbx(filepath=path)
    elif ext == ".obj":
        bpy.ops.wm.obj_import(filepath=path)
    elif ext == ".blend":
        with bpy.data.libraries.load(path, link=False) as (src, dst):
            dst.objects = list(src.objects)
        for o in dst.objects:
            if o is not None:
                bpy.context.scene.collection.objects.link(o)
    else:
        raise ValueError(f"Formato no admitido: {ext} (usa .glb, .gltf, .fbx, .obj o .blend)")
    bpy.context.view_layer.update()

    # Instancias de colección (habituales en .blend y FBX) -> objetos reales
    inst = [o for o in bpy.data.objects if o.instance_type == 'COLLECTION' and o.instance_collection]
    if inst:
        _select(inst)
        bpy.ops.object.duplicates_make_real()

    # Nombres del objeto y de sus padres: pistas para clasificar (antes de deshacer la jerarquía)
    for o in bpy.data.objects:
        chain, p = [], o
        while p is not None:
            chain.append(p.name)
            p = p.parent
        if o.type == 'MESH' and o.data:
            chain.append(o.data.name)
        o["cv_names"] = " / ".join(chain)

    # Curvas, textos, etc. -> malla; aplica modificadores
    convertible = [o for o in bpy.data.objects if o.type in ('MESH', 'CURVE', 'SURFACE', 'META', 'FONT')]
    if convertible:
        _select(convertible)
        try:
            bpy.ops.object.convert(target='MESH')
        except RuntimeError as e:
            rep.warn(f"No se pudieron convertir algunas piezas a malla: {e}")
    bpy.context.view_layer.update()

    meshes = [o for o in bpy.data.objects if o.type == 'MESH' and o.data]
    for o in meshes:
        if o.data.users > 1:
            o.data = o.data.copy()
    for o in meshes:
        mw = o.matrix_world.copy()
        o.parent = None
        o.data.transform(mw)
        o.matrix_world = Matrix.Identity(4)
        if mw.determinant() < 0:
            o.data.flip_normals()
    for o in [o for o in bpy.data.objects if o.type != 'MESH']:
        bpy.data.objects.remove(o)
    for o in [o for o in bpy.data.objects if len(o.data.vertices) == 0]:
        bpy.data.objects.remove(o)

    # Variantes de material glTF: si no se borran, el exportador restaura los materiales antiguos.
    for me in bpy.data.meshes:
        for attr in ("gltf2_variant_default_materials", "gltf2_variant_mesh_data"):
            coll = getattr(me, attr, None)
            if coll is not None:
                coll.clear()
    sc_var = getattr(bpy.context.scene, "gltf2_KHR_materials_variants_variants", None)
    if sc_var is not None:
        sc_var.clear()

    # Materiales asignados «al objeto» (no a la malla): mandan en el render y en la exportación, pero al unir piezas
    # Blender se queda con los de la malla. Se pasan a la malla para que haya un solo material por hueco y sea el que
    # se ve (en el Focus, el asiento tenía verde en el objeto y pintura en la malla).
    moved = 0
    for o in mesh_objects():
        if not any(sl.link == 'OBJECT' for sl in o.material_slots):
            continue
        if o.data.users > 1:
            o.data = o.data.copy()
        for i, sl in enumerate(o.material_slots):
            if sl.link == 'OBJECT':
                m = sl.material
                sl.link = 'DATA'
                if m is not None:
                    o.data.materials[i] = m
                moved += 1
    if moved:
        rep.step(f"materiales del objeto pasados a la malla: {moved}")

    n = len(mesh_objects())
    rep.step(f"importado {os.path.basename(path)}: {n} mallas")

    # Pocas mallas: probablemente puertas fundidas con la carrocería en un mismo objeto -> separar partes sueltas
    if n < 40:
        objs = mesh_objects()
        _select(objs)
        try:
            bpy.ops.object.mode_set(mode='EDIT')
            bpy.ops.mesh.select_all(action='SELECT')
            bpy.ops.mesh.separate(type='LOOSE')
            bpy.ops.object.mode_set(mode='OBJECT')
            rep.step(f"separadas partes sueltas: {n} -> {len(mesh_objects())} mallas")
        except RuntimeError as e:
            if bpy.context.object and bpy.context.object.mode != 'OBJECT':
                bpy.ops.object.mode_set(mode='OBJECT')
            rep.warn(f"No se pudieron separar las partes sueltas: {e}")


def remove_named(cfg, rep):
    """«remove» del config: piezas que se quitan a mano por su nombre (exacto o subcadena), antes de medir el coche.
    Para el trabajo coche a coche: restos del modelo original que ninguna regla general debe adivinar."""
    pats = [str(x).lower() for x in (cfg.get("remove") or [])]
    if not pats:
        return
    gone = []
    for p in measure():
        if any(pat in p.names for pat in pats):
            gone.append(p.name)
            bpy.data.objects.remove(p.obj)
    rep.step(f"quitadas a mano ({len(gone)}): {gone[:8]}")


def remove_duplicates(rep):
    """Piezas repetidas exactamente en el mismo sitio (mismos vértices y materiales): se queda una. Algunos modelos de
    juego traen media carrocería dos veces; dos superficies iguales superpuestas parpadean («z-fighting») y doblan los
    triángulos."""
    seen = {}
    gone = []
    for p in measure():
        v = p.verts
        if len(v) == 0:
            continue
        key = (len(v), tuple(sorted(p.mat_area)), tuple(np.round(v.min(axis=0), 4)), tuple(np.round(v.max(axis=0), 4)),
               tuple(np.round(v.sum(axis=0), 3)))
        if key in seen:
            gone.append(p.name)
            bpy.data.objects.remove(p.obj)
        else:
            seen[key] = p.name
    if gone:
        rep.step(f"quitadas {len(gone)} piezas duplicadas (exactamente iguales y superpuestas): {gone[:5]}")


def remove_junk(rep):
    """Quita suelos, sombras y fondos que traen algunos modelos (planos grandes y planos)."""
    parts = measure()
    mn, mx = all_bounds(parts)
    size = mx - mn
    removed = []
    for p in parts:
        flat = p.e.z < 0.01 * max(size.x, size.y)
        big = p.e.x > 0.8 * size.x and p.e.y > 0.8 * size.y
        footprint = p.e.x > 0.5 * size.x and p.e.y > 0.5 * size.y
        if (flat and big) or (RX["junk"].search(p.names) and flat and footprint and p.mn.z <= mn.z + 0.02 * size.z):
            removed.append(p.name)
            bpy.data.objects.remove(p.obj)
    if removed:
        rep.step(f"quitados suelos/sombras del modelo: {removed[:6]}")


def remove_outliers(rep):
    """Quita piezas sueltas lejos del coche (restos de la escena del juego o del autor: matrículas flotando, otro
    objeto a varios metros). Sin esto, el largo y las ruedas se calculan mal."""
    parts = measure()
    if len(parts) < 8:
        return
    total = sum(p.area for p in parts) or 1.0
    core, acc = [], 0.0
    for p in sorted(parts, key=lambda q: -q.area):
        core.append(p)
        acc += p.area
        if acc >= 0.8 * total:
            break
    lo = Vector([min(p.mn[i] for p in core) for i in range(3)])
    hi = Vector([max(p.mx[i] for p in core) for i in range(3)])
    pad = 0.2 * max(hi - lo)
    out = [p for p in parts if any(p.mn[i] > hi[i] + pad or p.mx[i] < lo[i] - pad for i in range(3))]
    names = [p.name[:24] for p in out]
    for p in out:
        bpy.data.objects.remove(p.obj)
    if out:
        rep.step(f"quitadas piezas sueltas lejos del coche: {names[:6]}")


# ---------------------------------------------------------------------------------------------- 2. orientar


def find_wheel_hubs(parts, mn, mx):
    """Ruedas: piezas redondas cerca del suelo, una por esquina. Devuelve {cuadrante: (Part, centro, radio)}.
    Cuadrante = (lado X: +1/-1, extremo Y: -1/+1). No depende de hacia dónde mire el frontal."""
    size = mx - mn
    cy = (mn.y + mx.y) / 2
    cx = (mn.x + mx.x) / 2
    best = {}
    big = max(size.x, size.y)                         # el largo (antes de orientar puede estar en X)
    # Redonda vista de lado, estrecha, del tamaño de una rueda (no una puerta) y en una esquina.
    round_ = [p for p in parts if 0.12 * size.z < p.e.z < 0.9 * size.z and 0.075 * big < p.e.z < 0.26 * big
              and 0.75 < p.e.y / max(p.e.z, 1e-6) < 1.33 and p.e.x <= 0.8 * p.e.z
              and abs(p.c.x - cx) >= 0.15 * size.x and abs(p.c.y - cy) >= 0.15 * size.y]
    # Suelo: lo más bajo de esas piezas (una placa, un escape o una rueda de plantilla en el centro no cuentan).
    ground = min((p.mn.z for p in round_), default=mn.z)
    for p in round_:
        d = p.e.z
        if p.mn.z > ground + 0.25 * d:
            continue                                  # apoyada en el suelo
        q = (1 if p.c.x > cx else -1, -1 if p.c.y < cy else 1)
        if q not in best or d > best[q][0].e.z:
            best[q] = (p, p.c.copy(), d / 2)
    return best


def orient_and_scale(cfg, rep):
    parts = measure()
    mn, mx = all_bounds(parts)
    size = mx - mn
    # Largo en Y
    if size.x > size.y:
        transform_all(Matrix.Rotation(math.radians(90), 4, 'Z'))
        rep.step("girado 90°: el largo del coche queda en el eje Y")
        parts = measure()
        mn, mx = all_bounds(parts)
        size = mx - mn

    # ¿Dónde está el frontal? Positivo = en -Y (convención); negativo = en +Y.
    score, why = 0.0, []
    fy = [p.c.y for p in parts if RX["front"].search(p.names)]
    ry = [p.c.y for p in parts if RX["rear"].search(p.names) and not RX["front"].search(p.names)]
    if fy and ry:
        d = (sum(ry) / len(ry)) - (sum(fy) / len(fy))
        if abs(d) > 0.1 * size.y:
            score += 3 if d > 0 else -3
            why.append("nombres de piezas (faros, capó, maletero…)")
    hubs = find_wheel_hubs(parts, mn, mx)
    ys_lo = [c.y for (sx, sy), (_, c, _) in hubs.items() if sy < 0]
    ys_hi = [c.y for (sx, sy), (_, c, _) in hubs.items() if sy > 0]
    if ys_lo and ys_hi:
        over_lo = min(ys_lo) - mn.y
        over_hi = mx.y - max(ys_hi)
        if abs(over_lo - over_hi) > 0.05 * size.y:
            score += 1 if over_lo > over_hi else -1
            why.append("voladizo delantero mayor que el trasero")
    flip = score < 0
    confidence = "alta" if abs(score) >= 3 else ("media" if abs(score) >= 1 else "baja")
    if cfg.get("flip_front"):
        flip = not flip
        why.append("flip_front del config")
    if flip:
        transform_all(Matrix.Rotation(math.pi, 4, 'Z'))
    rep.info["orientation"] = {"flipped": flip, "confidence": confidence, "why": why}
    rep.step(f"frontal hacia -Y (confianza {confidence}: {', '.join(why) or 'sin pistas'})")
    if confidence == "baja":
        rep.warn(rep.say("No está claro dónde está el frontal: revisa los renders y, si sale al revés, pon \"flip_front\": true.",
                         "No está claro dónde está el frontal. Mira el coche: si los faros quedan detrás, pulsa "
                         "«Frontal al revés: girar 180°»."))

    # Escala
    parts = measure()
    mn, mx = all_bounds(parts)
    length = mx.y - mn.y
    target = cfg.get("length_m")
    if target:
        s = float(target) / length
    elif 2.5 <= length <= 6.5:
        s = 1.0
    else:
        options = [f for f in (0.001, 0.01, 0.0254, 0.1, 10.0, 100.0, 1000.0) if 2.5 <= length * f <= 6.5]
        s = min(options, key=lambda f: abs(length * f - 4.3)) if options else 4.3 / length
        rep.warn(rep.say(f"El modelo medía {length:.2f} unidades de largo: escalado x{s:g}. Si el tamaño no es real, indica \"length_m\".",
                         f"El modelo venía con un tamaño raro y se ha ajustado a {length * s:.2f} m. Si no es el largo de tu "
                         "coche, escríbelo en «Largo del coche en metros» y pulsa otra vez «Abrir y detectar piezas»."))
    c0 = Vector(((mn.x + mx.x) / 2, (mn.y + mx.y) / 2, mn.z))
    transform_all(Matrix.Scale(s, 4) @ Matrix.Translation(-c0))
    rep.step(f"escala x{s:g}: largo {length * s:.2f} m, centrado y apoyado en el suelo")


# ---------------------------------------------------------------------------------------------- 3. ruedas


def _yaw_of_axle(verts):
    pts = verts - verts.mean(axis=0)
    _, vec = np.linalg.eigh(np.cov(pts.T))
    ax = vec[:, 0]
    if ax[0] < 0:
        ax = -ax
    return math.atan2(ax[1], ax[0])


def place_template_wheels(rep):
    """Modelos sacados de juegos (BeamNG…): traen UNA rueda de plantilla en el centro del coche y nada (o solo los
    bujes) en las esquinas. Se copia la plantilla a las cuatro esquinas: en los bujes si los hay; si no, centrada en
    cada paso de rueda de la carrocería. La cara de la llanta mira siempre hacia fuera."""
    parts = measure()
    mn, mx = all_bounds(parts)
    W, L = mx.x - mn.x, mx.y - mn.y
    cx, cy = (mn.x + mx.x) / 2, (mn.y + mx.y) / 2
    if len(find_wheel_hubs(parts, mn, mx)) >= 3:
        return False
    tires = [p for p in parts if 0.08 * L < p.e.z < 0.24 * L and 0.85 < p.e.y / max(p.e.z, 1e-6) < 1.18
             and p.e.x < 0.7 * p.e.z and abs(p.c.x - cx) < 0.3 * W and abs(p.c.y - cy) < 0.3 * L
             and (RX["wheel"].search(p.names) or p.mn.z < mn.z + 0.15 * p.e.z)]
    if not tires:
        return False
    tire = max(tires, key=lambda q: q.e.z)
    C, r = tire.c.copy(), tire.e.z / 2
    tol = 0.15 * r
    tmpl = [p for p in parts if (p.c - C).length < 1.05 * r
            and all(p.mn[i] >= tire.mn[i] - tol and p.mx[i] <= tire.mx[i] + tol for i in range(3))]
    ids = {id(p.obj) for p in tmpl}
    body = [p for p in parts if id(p.obj) not in ids]
    if not body:
        return False
    half_w = max(p.mx.x for p in tmpl) - min(p.mn.x for p in tmpl)

    # 1) Bujes / discos en las esquinas (redondos, eje lateral, más pequeños que la rueda)
    targets = {}
    for p in body:
        d = p.e.z
        if not (0.04 * L < d < 1.9 * r and 0.8 < p.e.y / max(d, 1e-6) < 1.25 and p.e.x < 0.8 * d):
            continue
        if abs(p.c.x - cx) < 0.25 * W or abs(p.c.y - cy) < 0.2 * L:
            continue
        q = (1 if p.c.x > cx else -1, -1 if p.c.y < cy else 1)
        if q not in targets or d > targets[q][1]:
            targets[q] = (p.c.copy(), d)
    how = "bujes"
    if len(targets) < 4:
        # 2) Pasos de rueda: perfil inferior de los costados; el arco es una «cúpula» en cada mitad del coche.
        how = "pasos de rueda"
        verts = np.concatenate([p.verts for p in body])
        side = verts[np.abs(verts[:, 0] - cx) > 0.3 * W]
        nb = 120
        edges = np.linspace(mn.y, mx.y, nb + 1)
        idx = np.clip(np.digitize(side[:, 1], edges) - 1, 0, nb - 1)
        raw = np.full(nb, np.nan)
        for i in range(nb):
            zs = side[idx == i, 2]
            if len(zs) >= 3:
                raw[i] = zs.min()
        centers = (edges[:-1] + edges[1:]) / 2
        # Mínimo en ±3 % del largo: en un faldón con pocos vértices, muchos tramos no tienen ninguno abajo y parecerían
        # «huecos» altos. Un paso de rueda es mucho más ancho y sobrevive a este filtro.
        k3 = max(1, int(round(0.03 * L / (edges[1] - edges[0]))))
        prof = np.array([np.nanmin(raw[max(0, i - k3):i + k3 + 1]) if not np.all(np.isnan(raw[max(0, i - k3):i + k3 + 1]))
                         else np.nan for i in range(nb)])
        base = np.nanpercentile(prof, 30)
        targets = {}
        def arch_score(i):
            # Un paso de rueda es ANCHO (casi el diámetro de la rueda): se mira lo más bajo de una ventana de ±0,7 r.
            # Un hueco estrecho (entre aleta y puerta, bajo el retrovisor…) no da puntuación.
            win = prof[np.abs(centers - centers[i]) < 0.3 * r]
            return -1.0 if np.any(np.isnan(win)) else float(win.min() - base)
        for sy, (a0, a1) in ((-1, (0.06, 0.42)), (1, (0.58, 0.94))):
            ids = [i for i in range(nb) if mn.y + a0 * L < centers[i] < mn.y + a1 * L and not np.isnan(prof[i])]
            if len(ids) < 5:
                continue
            k = max(ids, key=arch_score)
            if arch_score(k) < 0.4 * r:
                continue                                  # no hay cúpula ancha: no es un paso de rueda
            near = np.abs(centers - centers[k]) < 0.5 * r
            apex = float(np.nanmax(raw[near]))
            thr = base + 0.6 * (apex - base)
            lo_i = hi_i = k
            while lo_i - 1 in ids and prof[lo_i - 1] > thr:
                lo_i -= 1
            while hi_i + 1 in ids and prof[hi_i + 1] > thr:
                hi_i += 1
            y0 = float((centers[lo_i] + centers[hi_i]) / 2)
            z0 = float(apex - r - 0.03)                   # la rueda justo debajo del arco
            band = np.abs(verts[:, 1] - y0) < r
            xs = verts[band, 0]
            for sx in (1, -1):
                xo = (xs.max() if sx > 0 else xs.min()) if len(xs) else (mx.x if sx > 0 else mn.x)
                x0 = xo - sx * (half_w / 2 + 0.01 * L)
                targets[(sx, sy)] = (Vector((x0, y0, z0)), 2 * r)
    if len(targets) < 4:
        rep.warn("Hay una rueda de plantilla pero no se ha podido saber dónde colocarla: asigna las ruedas a mano.")
        return False

    # Hacia dónde mira la cara de la llanta en la plantilla (lo que no es neumático está hacia fuera).
    rim = [p for p in tmpl if p is not tire]
    face = 1.0
    if rim:
        rv = np.concatenate([p.verts for p in rim])
        face = 1.0 if rv[:, 0].mean() >= C.x else -1.0
    for (sx, sy), (t, _) in targets.items():
        mirror = sx != face
        m = Matrix.Translation(t) @ (Matrix.Scale(-1, 4, Vector((1, 0, 0))) if mirror else Matrix.Identity(4)) \
            @ Matrix.Translation(-C)
        for p in tmpl:
            o2 = p.obj.copy()
            o2.data = p.obj.data.copy()
            for col in p.obj.users_collection:
                col.objects.link(o2)
            o2.data.transform(m)
            if mirror:
                o2.data.flip_normals()
    for p in tmpl:
        bpy.data.objects.remove(p.obj)
    # Apoyar en el suelo sobre las ruedas (no sobre una pieza que cuelgue por debajo)
    ground = min(t.z for t, _ in targets.values()) - r
    if abs(ground) > 1e-4:
        transform_all(Matrix.Translation((0, 0, -ground)))
    rep.step(f"rueda de plantilla copiada a las 4 esquinas (por {how}), radio {r:.3f} m")
    return True


def settle_on_wheels(rep):
    """Con las ruedas ya en las esquinas: quita lo que sobra de los modelos de juego (la rueda de plantilla que se
    queda en el centro, placas o restos tirados por debajo del suelo) y apoya el coche sobre sus ruedas, centrado."""
    parts = measure()
    mn, mx = all_bounds(parts)
    hubs = find_wheel_hubs(parts, mn, mx)
    if len(hubs) < 3:
        return
    W, L = mx.x - mn.x, mx.y - mn.y
    ground = min(c.z - r for _, c, r in hubs.values())
    hub_ids = {id(t.obj) for t, _, _ in hubs.values()}
    xs = [c.x for _, c, _ in hubs.values()]
    xc = (max(xs) + min(xs)) / 2
    cy = (mn.y + mx.y) / 2
    gone = []
    # Rueda de plantilla sobrante: redonda, de tamaño rueda, en el centro y tirada en el suelo (bajo la carrocería).
    for p in parts:
        d = p.e.z
        if id(p.obj) in hub_ids or not (0.06 * L < d < 0.26 * L and 0.8 < p.e.y / max(d, 1e-6) < 1.25 and p.e.x < 0.7 * d):
            continue
        if abs(p.c.x - xc) < 0.3 * W and abs(p.c.y - cy) < 0.3 * L and p.mn.z < ground + 0.1 * d:
            for q in parts:
                if q not in gone and all(q.mn[i] >= p.mn[i] - 0.1 * d and q.mx[i] <= p.mx[i] + 0.1 * d for i in range(3)):
                    gone.append(q)
    # Piezas enteras por debajo del suelo en el que apoyan las ruedas: no pueden ser del coche.
    for p in parts:
        if id(p.obj) not in hub_ids and p not in gone and p.mx.z < ground + 0.01:
            gone.append(p)
    names = [p.name[:24] for p in gone]
    for p in gone:
        bpy.data.objects.remove(p.obj)
    if names:
        rep.step(f"quitados restos del modelo de juego (rueda de plantilla, piezas bajo el suelo): {names[:6]}")
    if abs(ground) > 0.005 or abs(xc) > 0.005:
        transform_all(Matrix.Translation((-xc, 0, -ground)))
        rep.step(f"apoyado sobre las ruedas (bajado {ground:.3f} m) y centrado entre ellas")


def detect_wheels(rep):
    """Asigna las piezas de cada rueda; endereza las delanteras si vienen giradas. Devuelve {grupo: (buje, radio)}."""
    parts = measure()
    mn, mx = all_bounds(parts)
    hubs = find_wheel_hubs(parts, mn, mx)
    names = {(1, -1): "front_left_wheel", (-1, -1): "front_right_wheel",
             (1, 1): "rear_left_wheel", (-1, 1): "rear_right_wheel"}
    result = {}
    for q, (tire, hub, r) in hubs.items():
        key = names[q]
        zone = [p for p in parts if abs(p.c.x - hub.x) < max(0.4, 0.9 * r)
                and math.hypot(p.c.y - hub.y, p.c.z - hub.z) < 1.05 * r and max(p.e) < 2.3 * r]
        yaw = _yaw_of_axle(tire.verts)
        if abs(math.degrees(yaw)) > 2.0:
            M = Matrix.Translation(hub) @ Matrix.Rotation(-yaw, 4, 'Z') @ Matrix.Translation(-hub)
            for p in zone:
                p.obj.data.transform(M)
            rep.step(f"{LABELS[key]}: enderezada {math.degrees(-yaw):+.1f}°")
        zone = [Part(z.obj) for z in zone]
        # 1) Lo que está en el eje (neumático, llanta, disco, tapacubos), tornillería pequeña y piezas con nombre de rueda
        wheel_mats = set()
        for p in zone:
            dyz = math.hypot(p.c.y - hub.y, p.c.z - hub.z)
            on_axis = dyz < max(0.05, 0.15 * r) and p.e.y < 2.2 * r and p.e.z < 2.2 * r
            small_bolt = dyz < 0.75 * r and max(p.e) < 0.15 * r
            named = RX["wheel"].search(p.names) is not None and dyz < r
            if RX["caliper"].search(p.names):
                continue                               # la pinza va fija: no gira
            if on_axis or small_bolt or named:
                p.obj["cv_group"] = key
                if on_axis:
                    wheel_mats.update(p.mat_area)
        # 2) Radios y piezas sueltas de la llanta: dentro del disco de la rueda y, o se repiten alrededor del eje
        #    (3 o más iguales a la misma distancia), o son del mismo material que la llanta/neumático.
        #    La pinza de freno también está dentro, pero es única y de otro material: se queda en la carrocería.
        def inside_disc(p):
            dyz = math.hypot(p.c.y - hub.y, p.c.z - hub.z)
            return dyz + max(p.e.y, p.e.z) / 2 <= 1.02 * r and abs(p.c.x - hub.x) < max(0.25, 0.8 * r)
        rest = [p for p in zone if "cv_group" not in p.obj and inside_disc(p) and not RX["caliper"].search(p.names)]
        # Firma que no cambia al girar la pieza alrededor del eje: distancia al eje, superficie y nº de vértices.
        def signature(p):
            dyz = math.hypot(p.c.y - hub.y, p.c.z - hub.z)
            return round(dyz / (0.03 * r)), round(math.log10(max(p.area, 1e-9)) * 20), len(p.verts)
        sig = {}
        for p in rest:
            sig.setdefault(signature(p), []).append(p)
        wheel_only = {m for m in wheel_mats if not RX["paint"].search(m.lower())}
        for p in rest:
            dyz = math.hypot(p.c.y - hub.y, p.c.z - hub.z)
            well_inside = dyz + max(p.e.y, p.e.z) / 2 <= 0.9 * r      # no el borde del paso de rueda
            repeated = len(sig[signature(p)]) >= 3 and well_inside
            same_mat = bool(set(p.mat_area) & wheel_only)
            if repeated or same_mat:
                p.obj["cv_group"] = key
        result[key] = (hub.copy(), r)
        rep.step(f"{LABELS[key]}: buje ({hub.x:+.2f}, {hub.y:+.2f}, {hub.z:+.2f}), radio {r:.3f} m")
    if len(result) < 4:
        rep.warn(rep.say(f"Solo se han encontrado {len(result)} ruedas. Las que falten no girarán; si es un error, "
                         "asígnalas con \"assign\" o en el panel.",
                         f"Solo se han encontrado {len(result)} ruedas. Las que falten no girarán: selecciona sus piezas y "
                         "asígnalas a su rueda en «3. Corregir»."))
    return result


# ---------------------------------------------------------------------------------------------- 4. aperturas


def _is_glass_material(m):
    if m is None:
        return False
    if RX["glass"].search(m.name.lower()):
        return True
    if m.use_nodes:
        b = m.node_tree.nodes.get("Principled BSDF")
        if b:
            tw = b.inputs.get("Transmission Weight") or b.inputs.get("Transmission")
            if tw is not None and not tw.is_linked and tw.default_value > 0.3:
                return True
            al = b.inputs.get("Alpha")
            if al is not None and not al.is_linked and al.default_value < 0.9:
                return True
    return False


def exterior_hits(parts, n=36):
    """Qué material se ve desde fuera: rayos hacia el coche desde los lados, arriba, delante, detrás y en diagonal.
    Devuelve {material: impactos}. Ignora ruedas (ya asignadas)."""
    from mathutils.bvhtree import BVHTree
    verts, polys, pmat = [], [], []
    for p in parts:
        if "cv_group" in p.obj:
            continue
        me = p.obj.data
        base = len(verts)
        verts.extend(map(tuple, p.verts))
        for poly in me.polygons:
            polys.append([base + i for i in poly.vertices])
            m = me.materials[poly.material_index] if me.materials and poly.material_index < len(me.materials) else None
            pmat.append(m.name if m else None)
    if not polys:
        return {}
    bvh = BVHTree.FromPolygons(verts, polys)
    mn, mx = all_bounds(parts)
    c, size = (mn + mx) / 2, mx - mn
    far = max(size) * 2
    dirs = [Vector(d).normalized() for d in ((1, 0, 0), (-1, 0, 0), (0, 0, -1), (0, 1, 0), (0, -1, 0),
                                              (1, 1, -1), (-1, 1, -1), (1, -1, -1), (-1, -1, -1))]
    hits = {}
    for d in dirs:
        # base ortogonal al rayo
        u = d.cross(Vector((0, 0, 1)) if abs(d.z) < 0.9 else Vector((1, 0, 0))).normalized()
        v = d.cross(u).normalized()
        for i in range(n):
            for j in range(n):
                a = (i / (n - 1) - 0.5) * max(size)
                b = (j / (n - 1) - 0.5) * max(size)
                origin = c - d * far + u * a + v * b
                loc, _, idx, _ = bvh.ray_cast(origin, d)
                if idx is not None and pmat[idx]:
                    hits[pmat[idx]] = hits.get(pmat[idx], 0) + 1
    return hits


def find_paint(parts, cfg, rep):
    """Material de la pintura: el que más se ve desde fuera (sin cristales ni ruedas); el nombre ayuda a desempatar."""
    forced = cfg.get("paint_material")
    if forced and bpy.data.materials.get(forced):
        return forced
    score = {}
    for name, h in exterior_hits(parts).items():
        m = bpy.data.materials.get(name)
        low = name.lower()
        if _is_glass_material(m) or RX["wheel"].search(low) or RX["light"].search(low):
            continue
        score[name] = h * (1.5 if RX["paint"].search(low) else 1.0)
    if not score:
        rep.warn("No se ha encontrado el material de la pintura.")
        return None
    paint = max(score, key=score.get)
    top = sorted(score.items(), key=lambda t: -t[1])[:3]
    rep.step(f"pintura: material «{paint}» (lo que más se ve desde fuera: {', '.join(f'{k} {int(v)}' for k, v in top)})")
    return paint


def _painted(p, paint, frac=0.5):
    return paint is not None and p.area > 0 and p.mat_area.get(paint, 0.0) >= frac * p.area


def _inside(p, lo, hi, tol=0.03):
    return all(lo[i] - tol <= p.mn[i] and p.mx[i] <= hi[i] + tol for i in range(3))


def _overlap_frac(p, lo, hi):
    """Parte del volumen de la caja de `p` que cae dentro de la caja lo–hi."""
    ov = 1.0
    for i in range(3):
        d = min(hi[i], p.mx[i]) - max(lo[i], p.mn[i])
        if d <= 0:
            return 0.0
        ov *= d / max(p.e[i], 1e-4)
    return ov


# Piezas que nunca se mueven con una apertura (van fijas al chasis).
NEVER_MOVES_RX = re.compile(r"exhaust|escape|auspuff|muffler|silencios|tailpipe|suspens|engine|motor(?!haube)|chassis|"
                            r"chasis|undercarr|underbody|underside|bajos|axle|(?<![a-z])eje|achse|brake|caliper|baer|brembo|rotor|"
                            r"pinza|wheel|tire|tyre|(?<![a-z])rim")
# Habitáculo: nunca va con el capó ni con el portón (salvo lo que se llama como ellos: guarnecido del portón).
CABIN_RX = re.compile(r"interior|seat|asiento|sitz|floor|suelo|boden|cage|pillar|dash|console|steer|carpet|headliner")
# Piezas del habitáculo que nunca van con una puerta aunque estén pegadas a ella.
NOT_DOOR_RX = re.compile(r"seat|_se\d|asiento|sitz|si[eè]ge|sedile|dash|steer|volante|lenkrad|console|consola|pedal|"
                         r"headliner|techo|carpet|alfombr|gear|shift|cambio|belt|cintur")


def _skin_bvh(skins):
    from mathutils.bvhtree import BVHTree
    verts, polys = [], []
    for sp in skins:
        base = len(verts)
        verts.extend(map(tuple, sp.verts))
        polys.extend([base + i for i in poly.vertices] for poly in sp.obj.data.polygons)
    return BVHTree.FromPolygons(verts, polys)


def _hugs_skin(p, bvh, outward, reach=0.24, need=0.35, top_z=None):
    """¿La chapa (o lo que ya va con ella) tapa la pieza por fuera o la toca? Rayos desde sus vértices: hacia fuera,
    un poco hacia dentro (emblemas y manillas que sobresalen), hacia abajo-fuera (cristales y marcos por encima de la
    chapa) y en vertical (alerones y marcos pegados por arriba o por abajo)."""
    v = p.verts
    if len(v) > 160:
        v = v[np.linspace(0, len(v) - 1, 160).astype(int)]
    up = Vector((0.0, 0.0, 1.0))
    near = [(outward, reach), (-outward, 0.08)]
    above = [((outward * 0.4 - up).normalized(), 0.9)]
    if abs(outward.z) < 0.5:
        near += [(-up, 0.12), (up, 0.12)]
    ok = 0
    for co in v:
        o = Vector(co)
        # Puertas: el rayo en diagonal (cristal y marco sobre la chapa) solo vale por encima de la línea de la
        # ventanilla; por debajo, una pieza tiene que estar pegada a la chapa (si no, se cuela el asiento).
        rays = near + (above if top_z is None or o.z > top_z - 0.05 else [])
        if bvh.find_nearest(o, 0.05)[0] is not None or any(bvh.ray_cast(o, d, r)[0] is not None for d, r in rays):
            ok += 1
    return ok >= need * len(v)


def _carve_door_faces(free, vols, paint, rep, unit=1.0, left_sign=1.0):
    """Recorta de las piezas interiores grandes (forro lateral del habitáculo hecho de una pieza que abarca las dos
    puertas, el pilar y el lateral trasero) las caras que van pegadas por dentro a cada puerta, y las separa en piezas
    nuevas que después se van con la puerta. Sin esto, al abrir la puerta su panel interior se queda en el coche.
    `unit`: metros por unidad del modelo; `left_sign`: +1 si la izquierda es +X (convención del pipeline).
    Cada pieza nueva lleva «cv_carved_for» = la puerta a la que pertenece."""
    m = 1.0 / unit
    import bmesh
    carved = 0
    for key, (lo, hi, sk) in vols.items():
        if key not in DOORS:
            continue
        outward = Vector((left_sign if "left" in key else -left_sign, 0.0, 0.0))
        bvh = _skin_bvh(sk)
        ylo, yhi = min(x.mn.y for x in sk) + 0.02 * m, max(x.mx.y for x in sk) - 0.02 * m
        zlo = min(x.mn.z for x in sk) - 0.02 * m
        ztop = max(x.mx.z for x in sk) + 0.45 * m              # hasta el marco de la ventanilla
        belt = max(x.mx.z for x in sk)                          # línea de la ventanilla (borde superior de la chapa)
        sign = outward.x
        side = max(x.mx.x * sign for x in sk)                   # cara exterior de la chapa (en el eje del costado)
        skin_ids = {id(x.obj) for x in sk}
        for p in list(free):
            o = p.obj
            if id(o) in skin_ids or "cv_group" in o or _painted(p, paint, 0.4) or _inside(p, lo, hi):
                continue
            if NEVER_MOVES_RX.search(p.names) or NOT_DOOR_RX.search(p.names) or _overlap_frac(p, lo, hi) <= 0:
                continue
            me = o.data
            bm = bmesh.new()
            bm.from_mesh(me)
            # Triángulos enormes que cruzan de una puerta a otra: se cortan por los bordes de la puerta.
            for co, no in (((0, ylo, 0), (0, 1, 0)), ((0, yhi, 0), (0, 1, 0)), ((0, 0, ztop), (0, 0, 1))):
                geom = bm.verts[:] + bm.edges[:] + bm.faces[:]
                bmesh.ops.bisect_plane(bm, geom=geom, plane_co=co, plane_no=no)
            take = []
            for f in bm.faces:
                c = f.calc_center_median()
                if not (ylo <= c.y <= yhi and zlo <= c.z <= ztop):
                    continue
                if bvh.ray_cast(c, outward, 0.22 * m)[0] is not None or bvh.find_nearest(c, 0.04 * m)[0] is not None:
                    take.append(f)
                elif c.z >= belt - 0.05 * m and c.x * sign >= side - 0.2 * m:
                    take.append(f)        # por encima de la chapa y pegado al costado: remate y marco de la ventanilla
            n_all = len(bm.faces)
            if not take or len(take) == n_all:
                bm.free()
                continue
            # Pieza nueva con las caras de la puerta; el original se queda con el resto.
            bm2 = bm.copy()
            bm.faces.index_update()
            take_idx = {f.index for f in take}
            bm2.faces.index_update()
            bmesh.ops.delete(bm2, geom=[f for f in bm2.faces if f.index not in take_idx], context='FACES')
            bmesh.ops.delete(bm, geom=take, context='FACES')
            bm.to_mesh(me)
            me.update()
            bm.free()
            me2 = me.copy()
            bm2.to_mesh(me2)
            me2.update()
            bm2.free()
            o2 = o.copy()
            o2.data = me2
            o2.name = o.name + ".puerta"
            o2["cv_carved_for"] = key
            for col in o.users_collection:
                col.objects.link(o2)
            carved += 1
        if carved:
            ids = {id(x.obj) for x in free}
            free = [x for x in measure() if "cv_group" not in x.obj or id(x.obj) in ids]
    if carved:
        rep.step(f"paneles interiores recortados para las puertas: {carved}")
    return free


def _split_straddlers(free, vols, paint, rep):
    """Separa en trozos sueltos las piezas que están a medias dentro de una apertura. Devuelve la lista de piezas
    libres actualizada. No toca la chapa pintada grande ni mallas enormes."""
    to_split = []
    for p in free:
        if "cv_group" in p.obj or _painted(p, paint, 0.4) or len(p.obj.data.polygons) > 30000:
            continue
        for lo, hi, _ in vols.values():
            f = _overlap_frac(p, lo, hi)
            if 0.08 <= f and not _inside(p, lo, hi):
                to_split.append(p.obj)
                break
    if not to_split:
        return free
    before = len(mesh_objects())
    for o in to_split:
        for x in bpy.context.view_layer.objects:
            x.select_set(False)
        o.select_set(True)
        bpy.context.view_layer.objects.active = o
        with bpy.context.temp_override(active_object=o, object=o, selected_objects=[o], selected_editable_objects=[o]):
            bpy.ops.object.mode_set(mode='EDIT')
            bpy.ops.mesh.select_all(action='SELECT')
            bpy.ops.mesh.separate(type='LOOSE')
            bpy.ops.object.mode_set(mode='OBJECT')
    names = {id(p.obj) for p in free}
    out = [p for p in measure() if "cv_group" not in p.obj or id(p.obj) in names]
    rep.step(f"piezas compartidas separadas: {len(to_split)} → {len(mesh_objects()) - before + len(to_split)} trozos")
    return out


def detect_openings(cfg, wheels, rep):
    """Puertas, capó y portón: por nombre (tipo) + geometría (posición), y si no hay nombres, por paneles pintados."""
    parts = measure()
    mn, mx = all_bounds(parts)
    W, L, H = mx.x - mn.x, mx.y - mn.y, mx.z - mn.z
    half_w = W / 2
    paint = find_paint(parts, cfg, rep)
    doors_cfg = int(cfg.get("doors", 5))
    four_doors = doors_cfg >= 4
    hatchback = cfg.get("body_style", "hatchback").lower() in ("hatchback", "estate", "wagon", "suv", "familiar",
                                                              "monovolumen", "mpv", "van")
    free = [p for p in parts if "cv_group" not in p.obj]

    fy = [h.y for k, (h, r) in wheels.items() if k.startswith("front")]
    ry = [h.y for k, (h, r) in wheels.items() if k.startswith("rear")]
    radius = max([r for (_, r) in wheels.values()] or [0.32])
    front_hub_y = sum(fy) / len(fy) if fy else mn.y + 0.2 * L
    rear_hub_y = sum(ry) / len(ry) if ry else mx.y - 0.2 * L

    skins = {}

    # --- por nombre
    named = {"door": [], "hood": [], "trunk": []}
    for p in free:
        if RX["caliper"].search(p.names) or RX["wheel"].search(p.names):
            continue
        if p.e.y > 0.55 * L or p.e.x > 1.1 * W:
            continue                                  # casi tan larga como el coche: no es un panel que se abra
        for kind in ("door", "hood", "trunk"):
            if RX[kind].search(p.names):
                named[kind].append(p)
                break
    if named["door"]:
        # Corte entre puerta delantera y trasera: hueco en Y entre las pieles pintadas de cada lado
        split = None
        if four_doors:
            painted_doors = sorted((p for p in named["door"] if _painted(p, paint, 0.3)), key=lambda p: p.c.y)
            ys = [p.c.y for p in painted_doors]
            gaps = [(ys[i + 1] - ys[i], (ys[i + 1] + ys[i]) / 2) for i in range(len(ys) - 1)]
            if gaps and max(gaps)[0] > 0.3:
                split = max(gaps)[1]
        for p in named["door"]:
            side = "left" if p.c.x > 0 else "right"
            pos = "rear" if (split is not None and p.c.y > split) else "front"
            p.obj["cv_group"] = f"{pos}_{side}_door"
        rep.step(f"puertas por nombre: {len(named['door'])} piezas")
    for p in named["hood"]:
        p.obj["cv_group"] = "hood"
    for p in named["trunk"]:
        p.obj["cv_group"] = "rear_hatch"
    if named["hood"]:
        rep.step(f"capó por nombre: {len(named['hood'])} piezas")
    if named["trunk"]:
        rep.step(f"portón/maletero por nombre: {len(named['trunk'])} piezas")

    # Solo cuenta como encontrada por nombre si entre sus piezas hay un panel pintado (si no, p. ej. un guarnecido
    # llamado «Hatch», se busca además el panel por geometría).
    have = {g for g in OPENINGS if any(p.obj.get("cv_group") == g and _painted(p, paint, 0.3) for p in parts)}
    free = [p for p in parts if "cv_group" not in p.obj
            or (p.obj.get("cv_group") in OPENINGS and p.obj.get("cv_group") not in have)]
    painted = [p for p in free if _painted(p, paint)]

    # --- por geometría: puertas (paneles pintados en el costado, entre los pasos de rueda)
    if not any(g in have for g in DOORS):
        for side, sign in (("left", 1), ("right", -1)):
            cands = [p for p in painted
                     if sign * p.c.x > 0.5 * half_w and p.e.x < 0.35 * p.e.y and 0.45 <= p.e.y <= 1.7
                     and 0.3 <= p.e.z <= 1.4 and 0.2 * H <= p.c.z <= 0.75 * H
                     and front_hub_y + 0.4 * radius < p.c.y < rear_hub_y - 0.1 * radius]
            cands.sort(key=lambda p: -p.area)
            chosen = []
            for p in cands:
                if all(p.mx.y < q.mn.y + 0.05 or p.mn.y > q.mx.y - 0.05 for q in chosen):
                    chosen.append(p)
                if len(chosen) == (2 if four_doors else 1):
                    break
            chosen.sort(key=lambda p: p.c.y)
            for i, p in enumerate(chosen):
                skins[f"{'front' if i == 0 else 'rear'}_{side}_door"] = [p]

    def wide_skin(cands):
        """Panel ancho centrado, o dos mitades simétricas (izquierda + derecha) que juntas lo forman."""
        whole = [p for p in cands if p.e.x > 0.5 * W and abs(p.c.x) < 0.15 * W]
        if whole:
            return [max(whole, key=lambda p: p.area)]
        halves = sorted((p for p in cands if p.e.x > 0.2 * W), key=lambda p: -p.area)
        for a in halves:
            for b in halves:
                if b is a or a.c.x * b.c.x >= 0:
                    continue
                mirror = abs(a.c.x + b.c.x) < 0.1 * W and abs(a.c.y - b.c.y) < 0.15 and abs(a.c.z - b.c.z) < 0.15
                if mirror and abs(a.area - b.area) < 0.4 * max(a.area, b.area):
                    return [a, b]
        return []

    used = set()
    # --- capó
    if "hood" not in have:
        sk = wide_skin([p for p in painted if p.c.y < mn.y + 0.4 * L and p.c.z > 0.5 * H
                        and p.e.z < 0.8 * p.e.y and 0.4 <= p.e.y <= 1.9])
        if sk:
            skins["hood"] = sk
            used.update(id(p) for p in sk)

    # --- portón / maletero
    if "rear_hatch" not in have:
        sk = wide_skin([p for p in painted if p.c.y > mx.y - 0.35 * L and p.c.z > 0.4 * H
                        and p.e.y < 1.8 and p.e.z > 0.12 and id(p) not in used])
        if sk:
            skins["rear_hatch"] = sk

    # --- volumen de cada apertura: lo que cabe entero dentro va con ella (manillas, cristales, guarnecidos)
    vols = {}
    for key, sk in skins.items():
        for s in sk:
            s.obj["cv_group"] = key
        lo = Vector((min(s.mn.x for s in sk), min(s.mn.y for s in sk), min(s.mn.z for s in sk)))
        hi = Vector((max(s.mx.x for s in sk), max(s.mx.y for s in sk), max(s.mx.z for s in sk)))
        s = sk[0]
        if key in DOORS:
            inward = 0.22
            if "left" in key:
                lo.x -= inward
                hi.x += 0.12
            else:
                hi.x += inward
                lo.x -= 0.12
            hi.z = min(mx.z - 0.03, hi.z + 0.6)
            lo.y -= 0.03
            hi.y += 0.03
        elif key == "hood":
            lo -= Vector((0.03, 0.03, 0.15))
            hi += Vector((0.03, 0.03, 0.05))
        else:
            lo -= Vector((0.03, 0.05, 0.05))
            hi += Vector((0.03, 0.05, 0.05))
            if hatchback:
                hi.z = mx.z - 0.02
                lo.y -= 0.45
        vols[key] = (lo, hi, sk)

    # Piezas compartidas entre una apertura y la carrocería (p. ej. un solo guarnecido para las dos puertas de un
    # lado, o el marco de todas las ventanillas): se separan en trozos sueltos para que cada trozo vaya donde toca.
    # Sin esto, el panel interior de la puerta se queda en el coche y la puerta abierta se ve hueca.
    free = _split_straddlers(free, vols, paint, rep)
    free = _carve_door_faces(free, vols, paint, rep)

    for key, (lo, hi, sk) in vols.items():
        if key in DOORS:
            outward = Vector((1.0 if "left" in key else -1.0, 0.0, 0.0))
        elif key == "hood":
            outward = Vector((0.0, 0.0, 1.0))
        else:
            outward = Vector((0.0, 1.0, 0.0))
        skin_ids = {id(x.obj) for x in sk}
        cands = [p for p in free if _inside(p, lo, hi) and id(p.obj) not in skin_ids
                 and ("cv_group" not in p.obj or p.obj.get("cv_group") == key)]
        cands = [p for p in cands if not NEVER_MOVES_RX.search(p.names)]
        if key in ("hood", "rear_hatch"):
            own = RX["hood"] if key == "hood" else RX["trunk"]
            cands = [p for p in cands if not CABIN_RX.search(p.names) or own.search(p.names)]
        if key in DOORS:
            cands = [p for p in cands if p.e.x <= 0.32 and not NOT_DOOR_RX.search(p.names)]
        # Solo va con la apertura lo que su chapa tapa por fuera y está pegado a ella (guarnecido, manilla, cristal,
        # retrovisor, alerón), o lo que está pegado a algo que ya va con ella (en varias pasadas). Así no se van con
        # la puerta el extremo del salpicadero ni con el portón los trocitos de los pilotos de la carrocería.
        accepted = [x for x in free if id(x.obj) in skin_ids]
        top_z = max(x.mx.z for x in accepted) if key in DOORS and accepted else None
        chosen = []
        for _ in range(3):
            bvh = _skin_bvh(accepted)
            new = [p for p in cands if p not in chosen and _hugs_skin(p, bvh, outward, top_z=top_z)]
            if not new:
                break
            chosen += new
            accepted += new
        n = 0
        for p in free:
            if "cv_group" in p.obj and p.obj.get("cv_group") == key and id(p.obj) not in skin_ids and p not in chosen:
                del p.obj["cv_group"]                 # asignado por nombre pero sin contacto con la chapa
        for p in chosen:
            p.obj["cv_group"] = key
            n += 1
        rep.step(f"{LABELS[key]}: panel{'es' if len(sk) > 1 else ''} «{', '.join(x.name[:24] for x in sk)}» + {n} piezas")

    # Una apertura sin chapa pintada no es una apertura: si solo se encontró por nombre el cristal o el guarnecido
    # (la chapa va soldada a la carrocería en el modelo), abrirla movería un cristal suelto. Se queda fija.
    glued = []
    huge = []
    for g in OPENINGS:
        loose = [p for p in parts if p.obj.get("cv_group") == g]
        if loose:
            span = max(p.mx.y for p in loose) - min(p.mn.y for p in loose)
            if span > (0.5 if g in DOORS else 0.6) * L:
                # Una puerta no mide medio coche ni un portón más de medio coche: se ha colado la carrocería.
                for p in loose:
                    del p.obj["cv_group"]
                huge.append(LABELS[g])
                continue
        if g in have or g in vols:
            continue
        total = sum(p.area for p in loose)
        solid = sum(a for p in loose for m, a in p.mat_area.items()
                    if not _is_glass_material(bpy.data.materials.get(m)))
        if loose and solid >= 0.5 * total:
            continue                                  # tiene chapa propia (p. ej. otro material de pintura): vale
        for p in loose:
            del p.obj["cv_group"]
        if loose:
            glued.append(LABELS[g])
    if huge:
        rep.warn("Se han descartado " + ", ".join(huge) + ": arrastraban media carrocería (el modelo junta esa "
                 "apertura con otras piezas). Se quedan cerradas; si el modelo las tiene sueltas, asígnalas a mano.")
    if glued:
        rep.warn("En este modelo la chapa de " + ", ".join(glued) + " va pegada a la carrocería (solo hay cristal o "
                 "guarnecido sueltos): se quedan cerradas para que no se abra un cristal flotando.")

    # --- asignaciones del config (mandan sobre todo)
    for key, patterns in (cfg.get("assign") or {}).items():
        if key not in GROUPS:
            rep.warn(f"assign: grupo desconocido «{key}» (válidos: {', '.join(GROUPS)})")
            continue
        n = 0
        for p in parts:
            if any(str(pat).lower() in p.names for pat in patterns):
                if key == "body":
                    if "cv_group" in p.obj:
                        del p.obj["cv_group"]
                else:
                    p.obj["cv_group"] = key
                n += 1
        rep.step(f"assign {key}: {n} piezas")

    found = sorted({p.obj.get("cv_group") for p in measure() if p.obj.get("cv_group") in OPENINGS})
    missing = [g for g in OPENINGS if g not in found and (four_doors or not g.startswith("rear_") or g == "rear_hatch")]
    if missing:
        rep.warn("No se han encontrado: " + ", ".join(LABELS[g] for g in missing)
                 + ". Si el modelo las tiene, asígnalas; si la carrocería es una sola pieza, el coche funciona sin esas aperturas.")
    rep.info["paint_material"] = paint
    return paint


# ---------------------------------------------------------------------------------------------- 5. materiales


# Paleta de acabados (lineal). Valores probados en el coche hecho a mano (Focus MK3): es la referencia visual.
PALETTE = {
    "glass": dict(color=(0.01, 0.012, 0.015), roughness=0.03, alpha=0.72),
    "chrome": dict(color=(0.92, 0.92, 0.94), metallic=1.0, roughness=0.1),
    "rim": dict(color=(0.32, 0.33, 0.35), metallic=1.0, roughness=0.3),
    "tire": dict(color=(0.02, 0.02, 0.02), roughness=0.92),
    "plastic": dict(color=(0.018, 0.018, 0.02), roughness=0.55),
    "trim_gloss": dict(color=(0.01, 0.01, 0.012), roughness=0.15),
    "interior": dict(color=(0.03, 0.03, 0.032), roughness=0.75),
    "light_red": dict(color=(0.45, 0.01, 0.01), roughness=0.08, emission=(1.0, 0.02, 0.01), emission_strength=0.6),
    "light_red_lens": dict(color=(0.55, 0.02, 0.02), roughness=0.05, alpha=0.8, emission=(1.0, 0.03, 0.02),
                           emission_strength=0.5),
    "light_lens": dict(color=(0.85, 0.87, 0.9), metallic=0.9, roughness=0.06),
    "light_amber": dict(color=(0.85, 0.32, 0.02), roughness=0.1),
    "brake": dict(color=(0.45, 0.45, 0.46), metallic=1.0, roughness=0.45),
    "caliper": dict(color=(0.07, 0.07, 0.08), metallic=0.3, roughness=0.4),
    "steel": dict(color=(0.45, 0.45, 0.46), metallic=1.0, roughness=0.45),
    "white": dict(color=(0.8, 0.8, 0.8), roughness=0.4),
}
# Tipo de pieza por nombre (del material y de los objetos que lo usan), en orden de prioridad.
CAT_RX = [
    ("plastic", re.compile(r"inner_?rim")),
    ("light", re.compile(r"(?<![a-z])tail|taill|tail_?l|lamp|light(?!_?gr[ae]y)|headl(?!in)|reflect|indicator|blinker|turn_?sig|lens|faro|piloto|"
                         r"intermit|clignot|blinke|scheinw|phare|fanale|(?<![a-z])fog|nebel|antiniebla")),
    ("glass", RX["glass"]),
    ("chrome", re.compile(r"chrom|cromo|mirror(?!.*body)|espejo|spiegel|miroir")),
    ("interior", re.compile(r"leather|cloth|seat|asiento|sitz|si[eè]ge|dash|interior|_int|_in\d|headliner|carpet|"
                            r"gauge|steer|volante|lenkrad|console|consola|fabric|alcantara|stitch|cockpit|pedal|"
                            r"headrest|belt|cintur|alfombr|teppich|moquette|upholst|tapiz")),
    ("tire", re.compile(r"tire|tyre|neum|reifen|pneu|(?<![a-z])rubber(?![a-z_]*tri)")),
    ("brake", re.compile(r"brake(?!_?light)|(?<![a-z])dis[ck]|rotor|freno|bremse|frein|baer|brembo")),
    ("caliper", RX["caliper"]),
    ("rim", re.compile(r"(?<![a-z])rim|alloy|llanta|felge|jante|spoke|hubcap|wheel_?cap|tapacubo|cerchio")),
    ("white", re.compile(r"plate|licen|kennz|matr[ií]cul|plaque")),
    ("steel", re.compile(r"metal|steel|acero|exhaust|escape|auspuff|pipe|(?<![a-z])alu(?![a-z])|alumin")),
    ("plastic", re.compile(r"plastic|trim|weatherstrip|seal|grill|grille|matte|black|frame|carbon|diffuser|rubber|"
                           r"pl[aá]stico|kunststoff")),
    ("emblem", re.compile(r"emblem|logo|badge|emblema|insignia|abzeichen")),
]


def _bsdf(m):
    return m.node_tree.nodes.get("Principled BSDF") if m and m.use_nodes and m.node_tree else None


def _mat_look(m):
    """(color lineal, metallic, ¿tiene textura de color?) de un material ya normalizado."""
    b = _bsdf(m)
    if b is None:
        return tuple(m.diffuse_color)[:3], 0.0, False
    bc = b.inputs["Base Color"]
    met = b.inputs["Metallic"]
    return tuple(bc.default_value)[:3], (0.0 if met.is_linked else met.default_value), bc.is_linked


def _hsv(col):
    import colorsys
    srgb = [(c * 12.92 if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055) for c in col]
    return colorsys.rgb_to_hsv(*[min(1, max(0, c)) for c in srgb])


def exterior_hits_by_obj(objs, n=40):
    """Cuántos rayos desde fuera (lados, arriba, delante, detrás, diagonales) tocan primero cada objeto. Los objetos
    tras un cristal o dentro de la carrocería (asientos, salpicadero, guarnecidos) reciben 0."""
    from mathutils.bvhtree import BVHTree
    verts, polys, owner = [], [], []
    for o in objs:
        me = o.data
        base = len(verts)
        verts.extend(tuple(v.co) for v in me.vertices)
        for poly in me.polygons:
            polys.append([base + i for i in poly.vertices])
            owner.append(o.name)
    if not polys:
        return {}
    bvh = BVHTree.FromPolygons(verts, polys)
    arr = np.array(verts)
    mn, mx = Vector(arr.min(axis=0)), Vector(arr.max(axis=0))
    c, size = (mn + mx) / 2, mx - mn
    far = max(size) * 2
    dirs = [Vector(d).normalized() for d in ((1, 0, 0), (-1, 0, 0), (0, 0, -1), (0, 1, 0), (0, -1, 0),
                                              (1, 1, -1), (-1, 1, -1), (1, -1, -1), (-1, -1, -1),
                                              (1, 0, -0.4), (-1, 0, -0.4))]
    hits = {}
    for d in dirs:
        u = d.cross(Vector((0, 0, 1)) if abs(d.z) < 0.9 else Vector((1, 0, 0))).normalized()
        v = d.cross(u).normalized()
        for i in range(n):
            for j in range(n):
                a = (i / (n - 1) - 0.5) * max(size)
                b = (j / (n - 1) - 0.5) * max(size)
                _, _, idx, _ = bvh.ray_cast(c - d * far + u * a + v * b, d)
                if idx is not None:
                    hits[owner[idx]] = hits.get(owner[idx], 0) + 1
    return hits


def classify_materials(cfg, paint, wheels, rep):
    """Pone a cada pieza un acabado coherente con lo que ES (cristal, llanta, neumático, interior, luz, freno…).

    Muchos modelos (sobre todo los convertidos de SketchUp/FBX) traen colores de relleno: asientos verde chillón,
    llantas «negro espejo» (metal al 100 % y color negro), cromados donde no toca. El coche hecho a mano se arregló
    pieza a pieza; aquí se hace de forma automática para cualquier coche, con tres pistas:
      1) el nombre del material y de los objetos (varios idiomas);
      2) dónde está la pieza: en una rueda, junto al buje sin girar (pinza), dentro del habitáculo (no se ve desde
         fuera salvo a través del cristal) o por fuera;
      3) su color y acabado (colores de relleno saturados, metal negro).
    Los materiales con textura conservan la textura (solo se corrige el metal). Con "keep_materials": true no se toca.
    """
    if cfg.get("keep_materials"):
        return
    objs = mesh_objects()
    if not objs:
        return
    parts = {o.name: Part(o) for o in objs}
    mn, mx = all_bounds(list(parts.values()))
    W, H = mx.x - mn.x, mx.z - mn.z
    hits = exterior_hits_by_obj(objs)
    fy = [h.y for k, (h, r) in wheels.items() if k.startswith("front")]
    ry = [h.y for k, (h, r) in wheels.items() if k.startswith("rear")]
    hub_y = (min(fy) if fy else mn.y + 0.2 * (mx.y - mn.y), max(ry) if ry else mx.y - 0.2 * (mx.y - mn.y))

    def where(o, p):
        g = o.get("cv_group", "")
        if g in WHEELS:
            return "wheel"
        for h, r in wheels.values():
            if abs(p.c.x - h.x) < 0.35 and math.hypot(p.c.y - h.y, p.c.z - h.z) < 1.05 * r and max(p.e) < 2.2 * r:
                return "wheel_static"
        hidden = hits.get(o.name, 0) == 0
        cabin = (abs(p.c.x) < W / 2 - 0.05 and p.c.z > 0.22 * H and hub_y[0] < p.c.y < hub_y[1])
        if hidden and cabin:
            return "interior"
        if g in DOORS and hidden:
            return "interior"                  # guarnecido de puerta (tapado por la propia puerta cerrada)
        return "hidden" if hidden else "exterior"

    palette = {}

    def pmat(cat):
        if cat not in palette:
            palette[cat] = cv.material("cv_" + cat, **PALETTE[cat])
        return palette[cat]

    counts = {}
    log = {}
    for o in objs:
        p = parts[o.name]
        place = where(o, p)
        names = (o.get("cv_names", o.name)).lower()
        for slot in o.material_slots:
            m = slot.material
            if m is None or m.name.startswith("cv_"):
                continue
            text = m.name.lower() + " " + names
            col, met, textured = _mat_look(m)
            lum = 0.2126 * col[0] + 0.7152 * col[1] + 0.0722 * col[2]
            h, sat, val = _hsv(col)
            cat = None
            if m.name == paint:
                cat = {"wheel_static": "caliper", "interior": "interior"}.get(place, "paint")
                if cat == "paint" and dict(CAT_RX)["interior"].search(names):
                    cat = "interior"
            else:
                for k, rx in CAT_RX:
                    if rx.search(text):
                        cat = k
                        break
                if cat == "light":
                    if re.search(r"indicator|blinker|turn_?sig|intermit|clignot|blinke", text):
                        cat = "light_amber"
                    elif re.search(r"(?<![a-z])tail|taill|tail_?l|rear_?l|brake|(?<![a-z])stop|r[uü]ckl|piloto|_red|(?<![a-z])red|rojo", text) or (
                            col[0] > 0.25 and col[0] > 3 * col[1]):
                        cat = "light_red"
                    else:
                        cat = "light_lens"
                # Un cristal del tamaño de una luneta o un parabrisas no es un piloto, aunque comparta nombre con él.
                if cat and cat.startswith("light") and _is_glass_material(m) \
                        and p.e.x > 0.45 * (mx.x - mn.x) and p.e.z > 0.2:
                    cat = "glass"
                # El sitio manda sobre nombres ambiguos
                if place == "interior" and cat in (None, "plastic", "steel", "white", "tire"):
                    cat = "interior"
                if place == "wheel" and (cat in (None, "steel", "chrome") or (cat == "plastic" and "rim" not in text)):
                    cat = "tire" if (cat is None and lum < 0.05 and met < 0.5) else "rim"
                if place == "wheel_static" and cat in (None, "steel"):
                    cat = "brake"
                if cat is None and not textured:
                    if sat > 0.75 and val > 0.3 and 70 < h * 360 < 200:
                        cat = "interior" if place in ("interior", "hidden") else "plastic"   # verde/cian de relleno
                    elif met >= 0.5 and lum < 0.12:
                        cat = "plastic"                                                       # «metal negro»
                    elif met >= 0.5:
                        cat = "steel"
                    elif sat < 0.15 and 0.008 < lum < 0.35 and place != "wheel":
                        cat = "interior" if place == "interior" else "plastic"               # gris liso: plástico
            log.setdefault(m.name, {}).setdefault(cat or "original", []).append(names.split(" / ")[0][:40])
            # Tapa de piloto trasero: cristal rojo (si no, el piloto se ve negro a través del cristal tintado).
            if cat == "glass" and ((col[0] > 0.25 and col[0] > 3 * col[1]) or re.search(r"red|rojo|(?<![a-z])tail|taill|tail_?l|r[uü]ckl|piloto", text))                     and p.c.y > (mn.y + mx.y) / 2 \
                    and not (p.e.x > 0.45 * (mx.x - mn.x) and p.e.z > 0.2):   # cristal ancho y alto = luneta
                cat = "light_red_lens"
            if cat in ("paint", "emblem") or cat is None:
                counts["original"] = counts.get("original", 0) + 1
                continue
            if textured and cat not in ("glass", "light_red", "light_red_lens", "light_lens", "light_amber"):
                # Textura propia (salpicadero, matrícula, logotipo…): se respeta; solo se quita el metal falso.
                b = _bsdf(m)
                if b is not None and cat not in ("rim", "chrome", "steel", "brake") and not b.inputs["Metallic"].is_linked:
                    b.inputs["Metallic"].default_value = 0.0
                counts[cat] = counts.get(cat, 0) + 1
                continue
            slot.material = pmat(cat)
            counts[cat] = counts.get(cat, 0) + 1
    for o in objs:
        cv.merge_slots(o)
    # Para revisar o corregir: qué acabado ha recibido cada material del modelo y en qué piezas.
    rep.info["materials"] = {m: {c: sorted(set(v))[:6] for c, v in cats.items()} for m, cats in sorted(log.items())}
    rep.step("acabados por tipo de pieza: " + ", ".join(f"{k} {v}" for k, v in sorted(counts.items(), key=lambda t: -t[1])))


def normalize_materials(rep):
    """Deja cada material con UNA salida conectada a un Principled BSDF (lo único que el exportador glTF entiende).
    Modelos convertidos (SketchUp, FBX antiguos) traen varias salidas o sombreadores «Diffuse»: sin esto salen blancos."""
    fixed = 0
    for m in bpy.data.materials:
        if not m.users:
            continue
        if not m.use_nodes:
            col = tuple(m.diffuse_color)
            m.use_nodes = True
            b = m.node_tree.nodes.get("Principled BSDF")
            if b:
                b.inputs["Base Color"].default_value = col
            fixed += 1
            continue
        nt = m.node_tree
        outs = [n for n in nt.nodes if n.type == 'OUTPUT_MATERIAL']
        active = next((o for o in outs if o.is_active_output), outs[0] if outs else None)
        src = active.inputs["Surface"].links[0].from_node if active and active.inputs["Surface"].is_linked else None
        if src is not None and src.type == 'BSDF_PRINCIPLED' and len(outs) == 1:
            continue
        if src is not None and src.type in ('MIX_SHADER', 'ADD_SHADER') and len(outs) == 1:
            continue                                   # mezclas que el exportador ya sabe leer
        bsdf = src if (src is not None and src.type == 'BSDF_PRINCIPLED') else             next((n for n in nt.nodes if n.type == 'BSDF_PRINCIPLED'), None)
        if bsdf is None:
            bsdf = nt.nodes.new('ShaderNodeBsdfPrincipled')
            diff = next((n for n in nt.nodes if n.type == 'BSDF_DIFFUSE'), None)
            if diff is not None and diff.inputs["Color"].is_linked:
                nt.links.new(diff.inputs["Color"].links[0].from_socket, bsdf.inputs["Base Color"])
            else:
                bsdf.inputs["Base Color"].default_value = (diff.inputs["Color"].default_value if diff is not None
                                                           else tuple(m.diffuse_color))
        # Un Principled que estaba desconectado trae valores de la conversión (p. ej. Metallic 1 en todo):
        # se confía en su color, no en su acabado.
        metal = re.search(r"chrome|cromo|metal|steel|acero|alu|rim|llanta|silver|plata|mirror|espejo", m.name.lower())
        for name, v in (("Metallic", 0.9 if metal else 0.0), ("Specular IOR Level", 0.5)):
            i = bsdf.inputs.get(name)
            if i is not None and not i.is_linked:
                i.default_value = v
        for o in outs:
            nt.nodes.remove(o)
        out = nt.nodes.new('ShaderNodeOutputMaterial')
        out.is_active_output = True
        nt.links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
        fixed += 1
    if fixed:
        rep.step(f"materiales normalizados para glTF: {fixed}")


def fix_materials(cfg, paint, rep):
    # Cristal con transmisión -> translúcido (la app usa transparencia)
    n = 0
    for m in bpy.data.materials:
        if not m.users or not _is_glass_material(m) or not m.use_nodes:
            continue
        b = m.node_tree.nodes.get("Principled BSDF")
        if b is None:
            continue
        tw = b.inputs.get("Transmission Weight") or b.inputs.get("Transmission")
        if tw is not None:
            for l in list(tw.links):
                m.node_tree.links.remove(l)
            tw.default_value = 0.0
        base = b.inputs["Base Color"]
        is_light = RX["light"].search(m.name.lower()) is not None
        if not base.is_linked and not is_light:
            col = base.default_value
            if (col[0] + col[1] + col[2]) / 3 > 0.5:     # cristal claro -> tintado oscuro como el resto de packs
                base.default_value = (0.012, 0.014, 0.018, 1.0)
        al = b.inputs.get("Alpha")
        if al is not None and not al.is_linked:
            al.default_value = 0.85 if is_light else min(al.default_value, 0.72)
        for attr, val in (("surface_render_method", "BLENDED"), ("blend_method", "BLEND")):
            try:
                setattr(m, attr, val)
            except (AttributeError, TypeError):
                pass
        n += 1
    if n:
        rep.step(f"cristales translúcidos: {n} materiales")

    # Acabado de pintura de coche (metalizado con barniz) si la pintura del modelo es un color liso
    pm = bpy.data.materials.get(paint) if paint else None
    pb = pm.node_tree.nodes.get("Principled BSDF") if pm and pm.use_nodes else None
    if pb is not None and not cfg.get("keep_paint_finish"):
        def setv(name, v):
            i = pb.inputs.get(name)
            if i is not None and not i.is_linked:
                i.default_value = v
        if not pb.inputs["Metallic"].is_linked and not 0.2 <= pb.inputs["Metallic"].default_value <= 0.7:
            setv("Metallic", 0.45)
            setv("Roughness", 0.3)
        setv("Specular IOR Level", 0.5)
        setv("Coat Weight", 1.0)
        setv("Coat Roughness", 0.04)
        rep.step("pintura con acabado metalizado y barniz")

    col = cfg.get("paint_color")
    if col and paint and bpy.data.materials.get(paint):
        h = col.lstrip("#")
        lin = [((int(h[i:i + 2], 16) / 255) / 12.92) if int(h[i:i + 2], 16) / 255 <= 0.04045
               else (((int(h[i:i + 2], 16) / 255) + 0.055) / 1.055) ** 2.4 for i in (0, 2, 4)]
        m = bpy.data.materials[paint]
        b = m.node_tree.nodes.get("Principled BSDF") if m.use_nodes else None
        if b:
            for l in list(b.inputs["Base Color"].links):
                m.node_tree.links.remove(l)
            b.inputs["Base Color"].default_value = (*lin, 1.0)
            rep.step(f"pintura recoloreada a {col}")

    # La app cambia el color del coche al instante buscando el material «paint» (manifest → paint.material).
    if paint and bpy.data.materials.get(paint) and paint != "paint":
        other = bpy.data.materials.get("paint")
        if other:
            other.name = "paint_original"
        bpy.data.materials[paint].name = "paint"
        rep.info["paint_material_original"] = paint
        rep.info["paint_material"] = "paint"

    max_tex = int(cfg.get("max_texture", 1024))
    for img in bpy.data.images:
        w, h = img.size
        if max(w, h) > max_tex:
            f = max_tex / max(w, h)
            img.scale(max(1, int(w * f)), max(1, int(h * f)))
    for m in list(bpy.data.materials):
        if m.users == 0:
            bpy.data.materials.remove(m)


# ---------------------------------------------------------------------------------------------- 6. montar


HATCH_STYLES = ("hatchback", "estate", "wagon", "suv", "familiar", "monovolumen", "mpv", "van")


def hinge_for(g, parts, cfg, wheels, paint):
    """Bisagra (o buje) y ángulo de apertura de un grupo a partir de sus piezas (coordenadas actuales).
    La usan el montaje final y el botón «Probar aperturas» del panel, para que coincidan."""
    hinge_cfg = cfg.get("hinges") or {}
    ang_cfg = cfg.get("angles") or {}
    mn = Vector((min(p.mn.x for p in parts), min(p.mn.y for p in parts), min(p.mn.z for p in parts)))
    mx = Vector((max(p.mx.x for p in parts), max(p.mx.y for p in parts), max(p.mx.z for p in parts)))
    c = (mn + mx) / 2
    angle = 0.0
    if g in DOORS:
        painted = [p for p in parts if _painted(p, paint, 0.3)]
        if painted:
            sk = max(painted, key=lambda p: p.area)
            mn, mx = sk.mn, sk.mx
        x_out = mx.x if "left" in g else mn.x
        pivot = Vector((x_out, mn.y, (mn.z + mx.z) / 2))
        angle = -62.0 if "left" in g else 62.0
    elif g == "hood":
        if hinge_cfg.get("hood") == "front":
            pivot, angle = Vector((c.x, mn.y, mn.z)), 40.0
        else:
            pivot, angle = Vector((c.x, mx.y, mx.z)), -48.0
    elif g == "rear_hatch":
        if hinge_cfg.get("rear_hatch") == "rear":
            pivot, angle = Vector((c.x, mx.y, mn.z)), -38.0
        else:
            hatch = cfg.get("body_style", "hatchback").lower() in HATCH_STYLES
            pivot, angle = Vector((c.x, mn.y, mx.z)), (80.0 if hatch else 60.0)
    elif g in WHEELS:
        pivot = wheels[g][0].copy() if g in wheels else c
    else:
        pivot = Vector((0, 0, 0))
    if g in ang_cfg:
        angle = float(ang_cfg[g])
    return pivot, angle


def add_door_card(o, g, paint, depth=0.04):
    """Cara interior de la puerta: copia de la chapa pintada que mira hacia fuera, metida `depth` m hacia dentro, con
    material de interior y caras invertidas. Sin ella, al abrir la puerta se ve el revés de la chapa (del color del
    coche) o un hueco. Si el modelo ya trae guarnecido, esta cara queda detrás y no se ve."""
    import bmesh
    me = o.data
    paint_idx = {i for i, m in enumerate(me.materials) if m is not None and m.name == paint}
    if not paint_idx:
        return 0
    outward = 1.0 if "left" in g else -1.0                  # izquierda = +X
    inner = bpy.data.materials.get("cv_interior") or cv.material("cv_interior", **PALETTE["interior"])
    if inner not in list(me.materials):
        me.materials.append(inner)
    card = list(me.materials).index(inner)
    bm = bmesh.new()
    bm.from_mesh(me)
    bm.normal_update()
    faces = [f for f in bm.faces if f.material_index in paint_idx and f.normal.x * outward > 0.35]
    if not faces:
        bm.free()
        return 0
    ret = bmesh.ops.duplicate(bm, geom=faces)
    nv = [x for x in ret["geom"] if isinstance(x, bmesh.types.BMVert)]
    nf = [x for x in ret["geom"] if isinstance(x, bmesh.types.BMFace)]
    bmesh.ops.translate(bm, verts=nv, vec=(-outward * depth, 0.0, 0.0))
    for f in nf:
        f.material_index = card
    bmesh.ops.reverse_faces(bm, faces=nf)
    bm.to_mesh(me)
    bm.free()
    return len(nf)


def assemble(cfg, wheels, rep):
    """Une cada grupo, coloca bisagras y bujes, crea car_root. Devuelve (root, parts, hinges, angles)."""
    groups = {g: [] for g in GROUPS}
    for o in mesh_objects():
        groups[o.get("cv_group", "body") if o.get("cv_group") in GROUPS else "body"].append(o)
    paint = rep.info.get("paint_material")
    pivots = {g: hinge_for(g, [Part(o) for o in objs], cfg, wheels, paint) for g, objs in groups.items() if objs}
    # Qué piezas (por su nombre en el modelo original) ha ido a cada grupo: sirve para corregir con "assign".
    rep.info["members"] = {g: sorted({o.get("cv_names", o.name).split(" / ")[0] for o in objs})[:80]
                           for g, objs in groups.items() if objs and g != "body"}

    total_tris = sum(sum(len(f.vertices) - 2 for f in o.data.polygons) for o in mesh_objects())
    max_tris = int(cfg.get("max_tris", 300000))
    if total_tris > max_tris:
        ratio = max(0.25, max_tris / total_tris)
        for g, objs in groups.items():
            if g in WHEELS:
                continue
            for o in objs:
                if len(o.data.polygons) < 200:
                    continue
                mod = o.modifiers.new("cv_decimate", 'DECIMATE')
                mod.ratio = ratio
                with bpy.context.temp_override(object=o, active_object=o):
                    bpy.ops.object.modifier_apply(modifier=mod.name)
        rep.step(f"reducido de {_n(total_tris)} triángulos (proporción {ratio:.2f})")

    parts = {g: cv.join(objs, g) for g, objs in groups.items() if objs}
    for g in DOORS:
        if parts.get(g) is not None:
            n = add_door_card(parts[g], g, paint)
            if n:
                rep.step(f"{LABELS[g]}: guarnecido interior ({_n(n)} caras)")
    root = bpy.data.objects.new("car_root", None)
    bpy.context.scene.collection.objects.link(root)
    hinges, angles = {}, {}
    for g, o in parts.items():
        pivot, angle = pivots[g]
        if g in OPENINGS:
            angles[g] = angle
        o.data.transform(Matrix.Translation(-pivot))
        o.location = pivot
        o.parent = root
        if g != "body":
            hinges[g] = pivot
        tris = sum(len(f.vertices) - 2 for f in o.data.polygons)
        rep.step(f"{LABELS[g]}: origen ({pivot.x:+.3f}, {pivot.y:+.3f}, {pivot.z:+.3f}), {_n(tris)} triángulos")
    return root, parts, hinges, angles


# ---------------------------------------------------------------------------------------------- 7. manifiesto


def slug(text):
    t = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode().lower()
    t = re.sub(r"[^a-z0-9]+", "_", t).strip("_")
    return t[:64] if len(t) >= 3 else (t + "_car")[:64]


ID_RX = re.compile(r"^[a-z0-9][a-z0-9_.-]{2,63}$")


def build_manifest(cfg, parts, wheels, angles, has_logo, rep):
    make, model = cfg.get("make", "").strip(), cfg.get("model", "").strip()
    gen = (cfg.get("generation") or "").strip()
    pid = cfg.get("id") or slug(" ".join(x for x in (make, model, gen) if x))
    lic = cfg.get("license") or {}
    manifest = {
        "formatVersion": 1,
        "id": pid,
        "name": cfg.get("name") or " ".join(x for x in (make, model, gen) if x),
        "version": cfg.get("version", "1.0.0"),
        "vehicle": {"make": make or "Mi coche", "model": model, "generation": gen or None,
                    "bodyStyle": cfg.get("body_style", "hatchback"), "doors": int(cfg.get("doors", 5))},
        "model": {"file": "model.glb", "rootNode": "car_root", "forwardYawDeg": 0},
        "parts": [
            {"id": g, "node": g, "signal": SIGNALS[g], "axis": [0, 1, 0] if g in DOORS else [1, 0, 0],
             "openAngleDeg": round(angles[g], 1), "durationMs": 800 if g in DOORS else (1100 if g == "hood" else 1200)}
            for g in OPENINGS if g in parts
        ],
        "wheels": [{"node": g, "radiusM": round(wheels[g][1], 3)} for g in WHEELS if g in parts and g in wheels],
        "camera": {"homeYawDeg": 40, "homePitchDeg": 14, "minPitchDeg": 3, "maxPitchDeg": 55, "fovDeg": 28, "zoom": 1.0},
        "brand": {"name": make or "Mi coche", **({"logo": "brand/logo.png"} if has_logo else {})},
        **({"paint": {"material": "paint"}} if rep.info.get("paint_material") == "paint" else {}),
        "license": {
            "model": lic.get("model", "DESCONOCIDA"),
            "author": lic.get("author", "desconocido"),
            "source": lic.get("source", ""),
            "attribution": lic.get("attribution", ""),
            "commercialRedistribution": bool(lic.get("commercial", False)),
        },
    }
    if manifest["vehicle"]["generation"] is None:
        del manifest["vehicle"]["generation"]
    # Mismas reglas que el validador de la app (VehiclePackParser)
    errors = []
    if not ID_RX.match(pid):
        errors.append(f"id inválido «{pid}»")
    for pt in manifest["parts"]:
        if not -150 <= pt["openAngleDeg"] <= 150:
            errors.append(f"{pt['id']}: ángulo fuera de ±150°")
    for w in manifest["wheels"]:
        if not 0.15 <= w["radiusM"] <= 0.6:
            errors.append(f"{w['node']}: radio {w['radiusM']} fuera de 0,15–0,6 m")
    if not lic:
        rep.warn(rep.say("Falta la licencia del modelo 3D (\"license\"). Sin ella el pack se marca como solo uso personal.",
                         "Falta la licencia del modelo 3D (paso 4). Sin ella el pack será solo para uso personal."))
    elif not manifest["license"]["commercialRedistribution"]:
        rep.warn("La licencia indica que el modelo no se puede redistribuir comercialmente: pack solo para uso personal.")
    for e in errors:
        rep.warn("manifest: " + e)
    rep.info["manifest_errors"] = errors
    return manifest


# ---------------------------------------------------------------------------------------------- 8. renders


def render_previews(root, manifest, out_dir, rep):
    os.makedirs(out_dir, exist_ok=True)
    sc = bpy.context.scene
    engines = {e.identifier for e in bpy.types.RenderSettings.bl_rna.properties['engine'].enum_items}
    sc.render.engine = 'BLENDER_EEVEE_NEXT' if 'BLENDER_EEVEE_NEXT' in engines else (
        'BLENDER_EEVEE' if 'BLENDER_EEVEE' in engines else 'BLENDER_WORKBENCH')
    sc.render.resolution_x, sc.render.resolution_y = 960, 560
    sc.render.film_transparent = False
    sc.render.image_settings.file_format = 'PNG'
    w = bpy.data.worlds.new("cv_preview")
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs[0].default_value = (0.42, 0.44, 0.47, 1)
    sc.world = w
    lights = []
    for rot, energy in (((0.8, 0, 0.5), 3.0), ((1.1, 0, 3.4), 1.6)):
        l = bpy.data.objects.new("cv_sun", bpy.data.lights.new("cv_sun", 'SUN'))
        l.data.energy = energy
        l.rotation_euler = rot
        sc.collection.objects.link(l)
        lights.append(l)
    cam = bpy.data.objects.new("cv_cam", bpy.data.cameras.new("cv_cam"))
    sc.collection.objects.link(cam)
    sc.camera = cam
    nodes = {o.name: o for o in root.children}

    def pose(open_):
        for pt in manifest["parts"]:
            o = nodes.get(pt["node"])
            if o is None:
                continue
            ax, ay, az = pt["axis"]
            axis = Vector((ax, -az, ay))                 # glTF -> Blender
            o.rotation_mode = 'AXIS_ANGLE'
            o.rotation_axis_angle = (math.radians(pt["openAngleDeg"]) if open_ else 0.0, *axis)

    def shot(name, yaw, pitch, dist, open_):
        pose(open_)
        y, p = math.radians(yaw), math.radians(pitch)
        target = Vector((0, 0, 0.6))
        cam.location = target + Vector((dist * math.cos(p) * math.sin(y), -dist * math.cos(p) * math.cos(y), dist * math.sin(p)))
        cam.rotation_euler = (target - cam.location).to_track_quat('-Z', 'Y').to_euler()
        cam.data.lens = 40
        sc.render.filepath = os.path.join(out_dir, name + ".png")
        bpy.ops.render.render(write_still=True)

    shot("1_cerrado_frente_izq", -40, 16, 9.0, False)
    shot("2_abierto_frente_izq", -40, 16, 9.0, True)
    shot("3_abierto_trasera_dcha", 140, 18, 9.0, True)
    shot("4_cerrado_lateral_izq", -90, 4, 9.5, False)
    pose(False)
    for l in lights:
        bpy.data.objects.remove(l)
    bpy.data.objects.remove(cam)
    rep.step(f"renders de comprobación en {out_dir}")


# ---------------------------------------------------------------------------------------------- todo junto


def analyze(cfg, rep, reset=True):
    """Primera mitad: importar, orientar, escalar y detectar ruedas y aperturas. Las piezas quedan sueltas y marcadas
    (propiedad «cv_group») para poder revisarlas o corregirlas. Devuelve (ruedas, pintura)."""
    import_model(os.path.abspath(cfg["input"]), rep, reset=reset)
    remove_named(cfg, rep)
    remove_duplicates(rep)
    remove_junk(rep)
    remove_outliers(rep)
    orient_and_scale(cfg, rep)
    place_template_wheels(rep)
    settle_on_wheels(rep)
    wheels = detect_wheels(rep)
    paint = detect_openings(cfg, wheels, rep)
    return wheels, paint


def wheels_from_groups(wheels):
    """Bujes de las ruedas asignadas a mano (o reasignadas) que no tengan buje calculado: centro de sus piezas."""
    out = dict(wheels)
    for g in WHEELS:
        objs = [o for o in mesh_objects() if o.get("cv_group") == g]
        if objs and g not in out:
            ps = [Part(o) for o in objs]
            big = max(ps, key=lambda p: p.e.z)
            out[g] = (big.c.copy(), big.e.z / 2)
    return {g: v for g, v in out.items() if any(o.get("cv_group") == g for o in mesh_objects())}


def build(cfg, rep=None, analyzed=None):
    """Generador completo con un config (dict). Desde el panel se llama con lo ya analizado y corregido.
    Devuelve el informe (dict)."""
    rep = rep or Report()
    src = os.path.abspath(cfg["input"])
    out = os.path.abspath(cfg.get("output_dir", os.path.splitext(src)[0] + "_carvision"))
    os.makedirs(out, exist_ok=True)

    if analyzed is None:
        wheels, paint = analyze(cfg, rep)
    else:
        wheels, paint = analyzed
        wheels = wheels_from_groups(wheels)
    normalize_materials(rep)
    classify_materials(cfg, paint, wheels, rep)
    fix_materials(cfg, paint, rep)
    if cfg.get("finish"):
        # Acabado a mano de un coche concreto (catálogo): script propio que corrige lo que ese modelo trae mal.
        # Se ejecuta con el coche ya escalado y orientado y las piezas aún sueltas (antes de unirlas por grupos).
        path = os.path.abspath(cfg["finish"])
        code = compile(open(path, encoding="utf-8").read(), path, "exec")
        exec(code, {"__name__": "cv_finish", "__file__": path, "bpy": bpy, "cfg": cfg, "rep": rep, "paint": paint,
                    "wheels": wheels, "E": sys.modules[__name__]})
        rep.step(f"acabado a mano: {os.path.basename(path)}")
    root, parts, hinges, angles = assemble(cfg, wheels, rep)

    pack_dir = os.path.join(out, "pack")
    os.makedirs(pack_dir, exist_ok=True)
    has_logo = False
    if cfg.get("logo"):
        from . import logo as logo_mod
        try:
            info = logo_mod.process_logo(os.path.abspath(cfg["logo"]), os.path.join(pack_dir, "brand", "logo.png"))
            has_logo = True
            rep.step(f"logotipo: {info}")
        except Exception as e:  # noqa: BLE001 - el pack se crea igual, con la marca en texto
            rep.warn(f"No se pudo preparar el logotipo ({e}); la app mostrará la marca en texto.")

    _, sh = cv.bake_contact_shadow(parts, root, os.path.join(out, "_silhouette.png"))
    rep.step(f"sombra de contacto {sh:.2f} m")

    manifest = build_manifest(cfg, parts, wheels, angles, has_logo, rep)
    glb = os.path.join(pack_dir, "model.glb")
    cv.export_pack_glb(root, glb, draco=not cfg.get("no_draco"))
    with open(os.path.join(pack_dir, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
        f.write("\n")

    zip_path = os.path.join(out, manifest["id"] + ".zip")
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
        for root_dir, _, files in os.walk(pack_dir):
            for fn in files:
                full = os.path.join(root_dir, fn)
                z.write(full, os.path.relpath(full, pack_dir).replace("\\", "/"))

    if cfg.get("previews", True):
        render_previews(root, manifest, os.path.join(out, "previews"), rep)
    blend = os.path.join(out, manifest["id"] + "_prepared.blend")
    bpy.ops.wm.save_as_mainfile(filepath=blend)

    tris = sum(sum(len(f.vertices) - 2 for f in o.data.polygons) for o in mesh_objects())
    rep.info.update({
        "id": manifest["id"],
        "zip": zip_path,
        "model_mb": round(os.path.getsize(glb) / 1e6, 2),
        "triangles": tris,
        "groups": {g: LABELS[g] for g in parts},
        "hinges": {g: [round(v, 3) for v in p] for g, p in hinges.items()},
        "angles": angles,
        "wheel_radius_m": {g: round(r, 3) for g, (_, r) in wheels.items()},
        "prepared_blend": blend,
        "ok": not rep.info.get("manifest_errors"),
    })
    with open(os.path.join(out, "report.json"), "w", encoding="utf-8") as f:
        json.dump(rep.as_dict(), f, ensure_ascii=False, indent=2)
    print("CV_DONE", zip_path)
    return rep.as_dict()
