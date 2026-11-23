# SPDX-License-Identifier: GPL-3.0-or-later
# CarVision Pack Builder (complemento de Blender). Ver DECISIONS.md, D-025.
"""CarVision Pack Builder: convierte el modelo 3D de cualquier coche en un pack para la app CarVision.

- Panel en Blender: barra lateral del visor 3D (tecla N) › pestaña «CarVision» (5 pasos).
- Línea de comandos: blender/scripts/build_pack.py -- config.json (para una IA con acceso a archivos).
Diseño y estado: docs/GENERADOR_COCHES.md.
"""
bl_info = {
    "name": "CarVision Pack Builder",
    "author": "CarVision",
    "version": (0, 1, 0),
    "blender": (4, 2, 0),
    "location": "Visor 3D › barra lateral (N) › CarVision",
    "description": "Convierte el modelo 3D de tu coche en un pack para la app CarVision",
    "category": "Import-Export",
}


def register():
    from . import ui
    ui.register()


def unregister():
    from . import ui
    ui.unregister()
