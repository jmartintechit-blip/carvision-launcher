package com.carvision.vehicle.packs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PackZipImporterTest {

    private val manifest = """
        {"formatVersion":1,"id":"test_pack","name":"Test","version":"1.0.0",
         "vehicle":{"make":"X","model":"Y","bodyStyle":"hatchback","doors":5},
         "model":{"file":"model.glb"},
         "license":{"model":"CC0","author":"a","source":"b","attribution":"c","commercialRedistribution":true}}
    """.trimIndent()

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            entries.forEach { (n, d) -> z.putNextEntry(ZipEntry(n)); z.write(d); z.closeEntry() }
        }
        return bos.toByteArray()
    }

    private fun importer(): Pair<PackZipImporter, File> {
        val root = Files.createTempDirectory("cvpacks").toFile()
        return PackZipImporter(File(root, "packs"), File(root, "tmp").apply { mkdirs() }) to root
    }

    @Test
    fun `importa un pack valido en la raiz o dentro de una carpeta`() {
        val (imp, root) = importer()
        val r1 = imp.import(ByteArrayInputStream(zip("manifest.json" to manifest.toByteArray(), "model.glb" to byteArrayOf(1, 2))))
        assertTrue(r1 is PackImportResult.Success)
        assertTrue(File(root, "packs/test_pack/model.glb").isFile)
        val r2 = imp.import(ByteArrayInputStream(zip("mi_coche/manifest.json" to manifest.toByteArray(), "mi_coche/model.glb" to byteArrayOf(1))))
        assertTrue(r2 is PackImportResult.Success)
    }

    @Test
    fun `rechaza rutas que salen de la carpeta (zip slip)`() {
        val (imp, _) = importer()
        val r = imp.import(ByteArrayInputStream(zip("../../evil.json" to "x".toByteArray())))
        assertTrue(r is PackImportResult.Failure)
    }

    @Test
    fun `rechaza archivos ejecutables o de codigo`() {
        val (imp, _) = importer()
        val r = imp.import(ByteArrayInputStream(zip("manifest.json" to manifest.toByteArray(), "model.glb" to byteArrayOf(1), "payload.dex" to byteArrayOf(1))))
        assertTrue(r is PackImportResult.Failure)
    }

    @Test
    fun `rechaza un pack sin el modelo declarado`() {
        val (imp, _) = importer()
        val r = imp.import(ByteArrayInputStream(zip("manifest.json" to manifest.toByteArray())))
        assertTrue(r is PackImportResult.Failure)
    }
}
