"""Compatibilidad: las utilidades comunes viven ahora en el paquete del generador
(blender/carvision_pack_builder/common.py), para que el complemento de Blender y los scripts usen el mismo código."""
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from carvision_pack_builder.common import *  # noqa: E402,F401,F403
from carvision_pack_builder.common import (  # noqa: E402,F401
    add_bevel, bake_contact_shadow, box, cylinder_x, export_glb, export_pack_glb, extrude_profile_x, join,
    material, merge_slots, mesh_object, reset_scene,
)
