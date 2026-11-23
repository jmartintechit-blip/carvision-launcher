# SPDX-License-Identifier: GPL-3.0-or-later
# CarVision Pack Builder (complemento de Blender). Ver DECISIONS.md, D-025.
"""Panel «CarVision» de Blender: convierte el modelo 3D de tu coche en un pack para la app, en 5 pasos.

Barra lateral del visor 3D (tecla N) › pestaña CarVision.
  1. Abrir modelo       elige el archivo (.glb, .gltf, .fbx, .obj, .blend) y pulsa Abrir.
  2. Revisar piezas     el coche se colorea por grupos; la lista dice qué se ha encontrado.
  3. Corregir           selecciona piezas en el visor y asígnalas al grupo correcto; «Probar aperturas».
  4. Marca y logotipo   marca, modelo, color, logotipo y licencia del modelo.
  5. Crear pack         genera el .zip para importar en la app (Garaje › Importar .zip).
"""
import json
import os

import bpy
from bpy.props import (BoolProperty, EnumProperty, FloatProperty, FloatVectorProperty, IntProperty, PointerProperty,
                       StringProperty)
from mathutils import Matrix, Vector

from . import engine as E

SHORT = {
    "body": "Carrocería", "front_left_door": "Puerta del. izq.", "front_right_door": "Puerta del. der.",
    "rear_left_door": "Puerta tras. izq.", "rear_right_door": "Puerta tras. der.", "hood": "Capó",
    "rear_hatch": "Portón / maletero", "front_left_wheel": "Rueda del. izq.", "front_right_wheel": "Rueda del. der.",
    "rear_left_wheel": "Rueda tras. izq.", "rear_right_wheel": "Rueda tras. der.",
}
GROUP_ITEMS = [(g, SHORT[g], E.LABELS[g]) for g in E.GROUPS]
COLORS = {
    "body": (0.62, 0.63, 0.66, 1.0),
    "front_left_door": (0.95, 0.33, 0.28, 1.0), "front_right_door": (0.27, 0.56, 0.96, 1.0),
    "rear_left_door": (0.98, 0.66, 0.22, 1.0), "rear_right_door": (0.32, 0.84, 0.90, 1.0),
    "hood": (0.52, 0.86, 0.36, 1.0), "rear_hatch": (0.80, 0.42, 0.92, 1.0),
}
WHEEL_COLOR = (0.12, 0.12, 0.13, 1.0)


# ---------------------------------------------------------------------------------------------- estado


class CVProps(bpy.types.PropertyGroup):
    input_path: StringProperty(name="Modelo 3D", subtype='FILE_PATH',
                               description="Archivo del coche: .glb, .gltf, .fbx, .obj o .blend")
    make: StringProperty(name="Marca", description="Por ejemplo: Seat")
    model: StringProperty(name="Modelo", description="Por ejemplo: León")
    generation: StringProperty(name="Generación / años", description="Opcional. Por ejemplo: MK1 (1999-2005)")
    body_style: EnumProperty(name="Carrocería", items=[
        ('hatchback', "Compacto (con portón)", ""), ('sedan', "Berlina (con maletero)", ""),
        ('estate', "Familiar", ""), ('suv', "SUV / todoterreno", ""), ('coupe', "Coupé", "")])
    doors: IntProperty(name="Puertas", default=5, min=2, max=5, description="Contando el portón: 3 o 5 en compactos")
    length_m: FloatProperty(name="Largo real (m)", default=0.0, min=0.0, max=7.0, precision=2,
                            description="Largo del coche de parachoques a parachoques, en metros (p. ej. 4.18). "
                                        "0 = automático")
    use_color: BoolProperty(name="Cambiar el color", default=False)
    color: FloatVectorProperty(name="Color", subtype='COLOR', size=3, min=0.0, max=1.0, default=(0.70, 0.72, 0.75))
    logo_path: StringProperty(name="Logotipo", subtype='FILE_PATH',
                              description="Imagen del logotipo de la marca (PNG o JPG). Si tiene fondo blanco, se quita solo")
    lic_model: EnumProperty(name="Licencia del modelo", items=[
        ('CC-BY-4.0', "CC BY 4.0 (citar al autor)", ""), ('CC0-1.0', "CC0 (dominio público)", ""),
        ('OTRA', "Otra / no lo sé", "El pack quedará marcado como solo uso personal")])
    lic_author: StringProperty(name="Autor del modelo")
    lic_source: StringProperty(name="Enlace del modelo")
    output_dir: StringProperty(name="Carpeta de salida", subtype='DIR_PATH',
                               description="Vacío = junto al modelo, en una carpeta «…_carvision»")
    assign_group: EnumProperty(name="Grupo", items=GROUP_ITEMS)
    hood_hinge: EnumProperty(name="Bisagra del capó", items=[
        ('rear', "Junto al parabrisas", "Lo normal"), ('front', "Delante", "El capó abre hacia delante")])
    hatch_hinge: EnumProperty(name="Bisagra del portón", items=[
        ('front', "Arriba, en el techo", "Lo normal"), ('rear', "Abajo, detrás", "Abre hacia atrás")])
    previews: BoolProperty(name="Hacer renders de comprobación", default=True)
    preview_open: BoolProperty(default=False)


def props(context):
    return context.scene.carvision


def cfg_from(context):
    p = props(context)
    cfg = {
        "input": bpy.path.abspath(p.input_path),
        "make": p.make.strip(), "model": p.model.strip(), "generation": p.generation.strip(),
        "body_style": p.body_style, "doors": p.doors,
        "hinges": {"hood": p.hood_hinge, "rear_hatch": p.hatch_hinge},
        "previews": p.previews,
    }
    if p.length_m > 0:
        cfg["length_m"] = p.length_m
    if p.use_color:
        cfg["paint_color"] = "#%02X%02X%02X" % tuple(int(round(max(0.0, min(1.0, c)) ** (1 / 2.2) * 255)) for c in p.color)
    if p.logo_path:
        cfg["logo"] = bpy.path.abspath(p.logo_path)
    if p.output_dir:
        cfg["output_dir"] = bpy.path.abspath(p.output_dir)
    commercial = p.lic_model in ('CC-BY-4.0', 'CC0-1.0')
    name = " ".join(x for x in (p.make, p.model, p.generation) if x.strip())
    cfg["license"] = {
        "model": p.lic_model if p.lic_model != 'OTRA' else "DESCONOCIDA",
        "author": p.lic_author or "desconocido", "source": p.lic_source,
        "attribution": (f"\"{name}\" de {p.lic_author or 'autor desconocido'}, {p.lic_model}. "
                        "Modificado con el generador de CarVision.") if commercial else "",
        "commercial": commercial,
    }
    return cfg


def save_state(context, wheels=None, paint=None, rep=None):
    sc = context.scene
    if wheels is not None:
        sc["cv_wheels"] = json.dumps({g: [h.x, h.y, h.z, r] for g, (h, r) in wheels.items()})
    if paint is not None:
        sc["cv_paint"] = paint
    if rep is not None:
        sc["cv_report"] = json.dumps({"steps": rep.steps, "warnings": rep.warnings, "info": {
            k: v for k, v in rep.info.items() if isinstance(v, (str, int, float, bool, list, dict))}})


def load_wheels(context):
    raw = json.loads(context.scene.get("cv_wheels", "{}"))
    return {g: (Vector(v[:3]), v[3]) for g, v in raw.items()}


def report_dict(context):
    return json.loads(context.scene.get("cv_report", '{"steps": [], "warnings": []}'))


def colorize():
    for o in E.mesh_objects():
        g = o.get("cv_group", "body")
        o.color = WHEEL_COLOR if g in E.WHEELS else COLORS.get(g, COLORS["body"])
    for screen in bpy.data.screens:
        for area in screen.areas:
            if area.type == 'VIEW_3D':
                for space in area.spaces:
                    if space.type == 'VIEW_3D':
                        space.shading.type = 'SOLID'
                        space.shading.color_type = 'OBJECT'


def deselect_all():
    for o in bpy.context.view_layer.objects:
        o.select_set(False)


def counts():
    c = {g: 0 for g in E.GROUPS}
    for o in E.mesh_objects():
        g = o.get("cv_group", "body")
        c[g if g in c else "body"] += 1
    return c


def analyzed(context):
    return "cv_wheels" in context.scene and any(o.get("cv_names") for o in E.mesh_objects())


# ---------------------------------------------------------------------------------------------- operadores


class CARVISION_OT_open(bpy.types.Operator):
    bl_idname = "carvision.open"
    bl_label = "Abrir y detectar piezas"
    bl_description = "Importa el coche, lo orienta y escala, y busca ruedas, puertas, capó y portón"
    bl_options = {'REGISTER'}

    def execute(self, context):
        p = props(context)
        path = bpy.path.abspath(p.input_path)
        if not path or not os.path.isfile(path):
            self.report({'ERROR'}, "Elige primero el archivo del modelo 3D")
            return {'CANCELLED'}
        rep = E.Report(ui=True)
        try:
            wheels, paint = E.analyze(cfg_from(context), rep, reset=False)
        except Exception as e:  # noqa: BLE001
            self.report({'ERROR'}, f"No se pudo abrir el modelo: {e}")
            return {'CANCELLED'}
        p.preview_open = False
        save_state(context, wheels, paint or "", rep)
        context.scene.pop("cv_result", None)
        colorize()
        deselect_all()
        try:
            for area in context.screen.areas:
                if area.type == 'VIEW_3D':
                    with context.temp_override(area=area, region=next(r for r in area.regions if r.type == 'WINDOW')):
                        bpy.ops.view3d.view_all()
        except Exception:  # noqa: BLE001 - encuadrar es opcional
            pass
        self.report({'INFO'}, "Coche abierto: revisa los colores de cada grupo")
        return {'FINISHED'}


class CARVISION_OT_flip(bpy.types.Operator):
    bl_idname = "carvision.flip"
    bl_label = "El frontal está detrás: girar 180°"
    bl_description = "Gira el coche y vuelve a buscar las piezas (si el frontal ha quedado al revés)"

    def execute(self, context):
        _close_preview(context)
        E.transform_all(Matrix.Rotation(3.14159265, 4, 'Z'))
        for o in E.mesh_objects():
            o.pop("cv_group", None)
        rep = E.Report(ui=True)
        cfg = cfg_from(context)
        wheels = E.detect_wheels(rep)
        paint = E.detect_openings(cfg, wheels, rep)
        save_state(context, wheels, paint or "", rep)
        colorize()
        deselect_all()
        return {'FINISHED'}


class CARVISION_OT_assign(bpy.types.Operator):
    bl_idname = "carvision.assign"
    bl_label = "Asignar las piezas seleccionadas"
    bl_description = "Las piezas seleccionadas en el visor pasan al grupo elegido"
    bl_options = {'REGISTER', 'UNDO'}

    def execute(self, context):
        _close_preview(context)
        g = props(context).assign_group
        sel = [o for o in context.selected_objects if o.type == 'MESH']
        if not sel:
            self.report({'WARNING'}, "Selecciona antes las piezas en el visor (clic, o B para seleccionar con un recuadro)")
            return {'CANCELLED'}
        for o in sel:
            if g == "body":
                o.pop("cv_group", None)
            else:
                o["cv_group"] = g
        colorize()
        deselect_all()
        self.report({'INFO'}, f"{len(sel)} {'pieza' if len(sel) == 1 else 'piezas'} → {E.LABELS[g]}")
        return {'FINISHED'}


class CARVISION_OT_select_group(bpy.types.Operator):
    bl_idname = "carvision.select_group"
    bl_label = "Seleccionar el grupo"
    bl_description = "Selecciona en el visor todas las piezas del grupo elegido"

    def execute(self, context):
        g = props(context).assign_group
        for o in context.view_layer.objects:
            o.select_set(o.type == 'MESH' and o.get("cv_group", "body") == g)
        return {'FINISHED'}


def _open_preview(context):
    cfg = cfg_from(context)
    wheels = load_wheels(context)
    paint = context.scene.get("cv_paint") or None
    groups = {}
    for o in E.mesh_objects():
        g = o.get("cv_group")
        if g in E.OPENINGS:
            groups.setdefault(g, []).append(o)
    for g, objs in groups.items():
        pivot, angle = E.hinge_for(g, [E.Part(o) for o in objs], cfg, wheels, paint)
        axis = 'Z' if g in E.DOORS else 'X'
        M = Matrix.Translation(pivot) @ Matrix.Rotation(angle * 3.14159265 / 180, 4, axis) @ Matrix.Translation(-pivot)
        for o in objs:
            o.matrix_world = M @ o.matrix_world
    props(context).preview_open = True


def _close_preview(context):
    if not props(context).preview_open:
        return
    for o in E.mesh_objects():
        o.matrix_world = Matrix.Identity(4)      # las piezas están horneadas: su posición de reposo es la identidad
    props(context).preview_open = False


class CARVISION_OT_preview(bpy.types.Operator):
    bl_idname = "carvision.preview"
    bl_label = "Probar aperturas"
    bl_description = "Abre (o cierra) puertas, capó y portón con las bisagras calculadas, para comprobarlas"

    def execute(self, context):
        if props(context).preview_open:
            _close_preview(context)
        else:
            _open_preview(context)
        return {'FINISHED'}


class CARVISION_OT_build(bpy.types.Operator):
    bl_idname = "carvision.build"
    bl_label = "Crear pack"
    bl_description = "Genera el .zip del coche para importarlo en la app (Garaje › Importar .zip)"

    def execute(self, context):
        p = props(context)
        if not analyzed(context):
            self.report({'ERROR'}, "Primero abre el modelo (paso 1)")
            return {'CANCELLED'}
        if not p.make.strip():
            self.report({'ERROR'}, "Escribe la marca del coche (paso 4)")
            return {'CANCELLED'}
        _close_preview(context)
        cfg = cfg_from(context)
        rd = report_dict(context)
        rep = E.Report(ui=True)
        rep.steps, rep.warnings = list(rd.get("steps", [])), list(rd.get("warnings", []))
        rep.info["paint_material"] = context.scene.get("cv_paint") or None
        try:
            result = E.build(cfg, rep, analyzed=(load_wheels(context), rep.info["paint_material"]))
        except Exception as e:  # noqa: BLE001
            self.report({'ERROR'}, f"No se pudo crear el pack: {e}")
            return {'CANCELLED'}
        context.scene["cv_result"] = json.dumps({"zip": result.get("zip"), "ok": result.get("ok"),
                                                 "warnings": result.get("warnings", [])})
        self.report({'INFO'} if result.get("ok") else {'WARNING'}, f"Pack creado: {result.get('zip')}")
        return {'FINISHED'}


class CARVISION_OT_open_folder(bpy.types.Operator):
    bl_idname = "carvision.open_folder"
    bl_label = "Abrir la carpeta del pack"

    def execute(self, context):
        res = json.loads(context.scene.get("cv_result", "{}"))
        if res.get("zip"):
            bpy.ops.wm.path_open(filepath=os.path.dirname(res["zip"]))
        return {'FINISHED'}


# ---------------------------------------------------------------------------------------------- panel


class CARVISION_PT_main(bpy.types.Panel):
    bl_label = "CarVision · Crear mi coche"
    bl_idname = "CARVISION_PT_main"
    bl_space_type = 'VIEW_3D'
    bl_region_type = 'UI'
    bl_category = "CarVision"

    def draw(self, context):
        p = props(context)
        lay = self.layout
        done = analyzed(context)
        built = bool(json.loads(context.scene.get("cv_result", "{}")).get("zip"))

        def field(box, name, label):
            box.label(text=label)
            box.prop(p, name, text="")

        b = lay.box()
        b.label(text="1. Abrir modelo", icon='FILE_3D')
        b.prop(p, "input_path", text="")
        field(b, "length_m", "Largo del coche en metros:")
        b.label(text="De parachoques a parachoques.")
        b.label(text="0 = automático.")
        b.operator("carvision.open", icon='IMPORT', text="Abrir y detectar piezas")

        b = lay.box()
        b.label(text="2. Revisar piezas", icon='VIEWZOOM')
        if not done:
            b.label(text="Abre primero el modelo.")
        elif built:
            b.label(text="Pack creado: el coche ya está montado.", icon='CHECKMARK')
            b.label(text="Para cambiar algo, vuelve al paso 1.")
        else:
            c = counts()
            col = b.column(align=True)
            for g in E.GROUPS:
                n = c.get(g, 0)
                icon = 'CHECKMARK' if n else ('BLANK1' if g == "body" else 'X')
                col.label(text=f"{SHORT[g]}: {n}", icon=icon)
            # Avisos del análisis, salvo los de piezas que faltan: esos se recalculan con lo asignado ahora mismo
            warns = [w for w in report_dict(context).get("warnings", [])
                     if not w.startswith(("No se han encontrado", "Solo se han encontrado"))]
            expected = [g for g in E.OPENINGS if p.doors >= 4 or not g.startswith("rear_") or g == "rear_hatch"]
            missing = [SHORT[g] for g in expected if c.get(g, 0) == 0]
            if missing:
                warns.append("No se han encontrado: " + ", ".join(missing) + ". Haz clic sobre esa parte: si se "
                             "ilumina sola, asígnala; si se ilumina toda la carrocería, no se puede abrir.")
            wheels_found = sum(1 for g in E.WHEELS if c.get(g, 0))
            if wheels_found < 4:
                warns.append(f"Solo hay {wheels_found} ruedas. Selecciona las piezas de la que falta y asígnala "
                             "a su rueda.")
            for w in warns[:4]:
                wc = b.column(align=True)
                wc.alert = True
                first = True
                for line in _wrap(w, 34)[:4]:
                    wc.label(text=line, icon='ERROR' if first else 'BLANK1')
                    first = False
            b.label(text="Gris: carrocería. Negro: ruedas.")
            b.operator("carvision.flip", icon='LOOP_BACK', text="Frontal al revés: girar 180°")

        b = lay.box()
        b.label(text="3. Corregir", icon='TOOL_SETTINGS')
        b.label(text="Selecciona piezas en el visor")
        b.label(text="y elige su grupo:")
        b.prop(p, "assign_group", text="")
        row = b.row(align=True)
        row.operator("carvision.assign", icon='CHECKMARK', text="Asignar")
        row.operator("carvision.select_group", icon='RESTRICT_SELECT_OFF', text="Ver grupo")
        field(b, "hood_hinge", "Bisagra del capó:")
        field(b, "hatch_hinge", "Bisagra del portón:")
        b.operator("carvision.preview", icon='HIDE_OFF' if not p.preview_open else 'HIDE_ON',
                   text="Cerrar aperturas" if p.preview_open else "Probar aperturas")

        b = lay.box()
        b.label(text="4. Marca y logotipo", icon='COLOR')
        field(b, "make", "Marca:")
        field(b, "model", "Modelo:")
        field(b, "generation", "Generación o años (opcional):")
        field(b, "body_style", "Carrocería:")
        b.prop(p, "doors")
        b.prop(p, "use_color")
        if p.use_color:
            b.prop(p, "color", text="")
            b.label(text="Se verá en el pack (aquí no).")
        field(b, "logo_path", "Logotipo (imagen):")
        field(b, "lic_model", "Licencia del modelo 3D:")
        field(b, "lic_author", "Autor del modelo:")
        field(b, "lic_source", "Enlace del modelo:")

        b = lay.box()
        b.label(text="5. Crear pack", icon='PACKAGE')
        field(b, "output_dir", "Carpeta de salida (opcional):")
        b.prop(p, "previews", text="Hacer renders de comprobación")
        b.operator("carvision.build", icon='EXPORT', text="Crear pack")
        res = json.loads(context.scene.get("cv_result", "{}"))
        if res.get("zip"):
            b.label(text="¡Listo!", icon='CHECKMARK')
            b.label(text=os.path.basename(res["zip"]))
            b.label(text="Cópialo a un USB y en la app:")
            b.label(text="Garaje › Importar .zip")
            b.operator("carvision.open_folder", icon='FILE_FOLDER', text="Abrir la carpeta")


def _wrap(text, width):
    words, lines, cur = text.split(), [], ""
    for w in words:
        if len(cur) + len(w) + 1 > width and cur:
            lines.append(cur)
            cur = w
        else:
            cur = f"{cur} {w}".strip()
    if cur:
        lines.append(cur)
    return lines


CLASSES = (CVProps, CARVISION_OT_open, CARVISION_OT_flip, CARVISION_OT_assign, CARVISION_OT_select_group,
           CARVISION_OT_preview, CARVISION_OT_build, CARVISION_OT_open_folder, CARVISION_PT_main)


def register():
    for c in CLASSES:
        bpy.utils.register_class(c)
    bpy.types.Scene.carvision = PointerProperty(type=CVProps)


def unregister():
    del bpy.types.Scene.carvision
    for c in reversed(CLASSES):
        bpy.utils.unregister_class(c)
