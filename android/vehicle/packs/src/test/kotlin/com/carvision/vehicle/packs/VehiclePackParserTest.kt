package com.carvision.vehicle.packs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VehiclePackParserTest {

    private fun manifest(file: String = "model.glb", angle: Float = -65f) = """
        {
          "formatVersion": 1, "id": "test_car", "name": "Test", "version": "1.0.0",
          "vehicle": { "make": "X", "model": "Y", "bodyStyle": "hatchback", "doors": 5 },
          "model": { "file": "$file" },
          "parts": [ { "id": "fl", "node": "front_left_door", "signal": "door.front_left",
                       "axis": [0,1,0], "openAngleDeg": $angle } ],
          "license": { "model": "CC0", "author": "a", "source": "b", "attribution": "c",
                       "commercialRedistribution": true }
        }
    """.trimIndent()

    @Test
    fun `manifiesto correcto es valido`() {
        assertTrue(VehiclePackParser.parse(manifest()) is PackParseResult.Valid)
    }

    @Test
    fun `rechaza rutas que salen del pack`() {
        assertTrue(VehiclePackParser.parse(manifest(file = "../../secret.glb")) is PackParseResult.Invalid)
        assertFalse(VehiclePackParser.isSafeRelativePath("/abs/model.glb"))
    }

    @Test
    fun `rechaza angulos absurdos`() {
        assertTrue(VehiclePackParser.parse(manifest(angle = 400f)) is PackParseResult.Invalid)
    }

    @Test
    fun `los packs del repositorio son validos`() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "vehicle-packs") }.firstOrNull { it.isDirectory } ?: return
        root.listFiles()!!.filter { File(it, "manifest.json").exists() }.forEach { dir ->
            val r = VehiclePackParser.parse(File(dir, "manifest.json").readText())
            assertTrue("${dir.name}: $r", r is PackParseResult.Valid)
        }
    }
}
