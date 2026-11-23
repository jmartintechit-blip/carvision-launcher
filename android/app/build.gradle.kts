import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.carvision.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.carvision.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86") }
    }

    // Firma de release. Se busca primero FUERA del proyecto (%USERPROFILE%\.carvision\keystore.properties, lo crea
    // scripts/crear_clave_firma.ps1) y si no, en android/keystore.properties. Ninguno se versiona ni va en el .zip del
    // código. Sin ninguno, la release se firma con la clave de depuración: vale para probar, no para vender.
    val keystoreFile = listOf(
        File(System.getProperty("user.home"), ".carvision/keystore.properties"),
        rootProject.file("keystore.properties"),
    ).firstOrNull { it.isFile }
    val hasReleaseKey = keystoreFile != null
    signingConfigs {
        if (keystoreFile != null) {
            val p = Properties().apply { keystoreFile.inputStream().use { load(it) } }
            create("release") {
                storeFile = File(keystoreFile.parentFile, p.getProperty("storeFile")).takeIf { it.isFile }
                    ?: File(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    // UNA sola APK para todas las radios (D-028): el cliente no tiene que elegir arquitectura.
    // arm64-v8a: radios modernas · armeabi-v7a: radios antiguas de 32 bits · x86: emuladores y las pocas radios Intel.
    // x86_64 queda fuera: no hay radios con ese chip y solo añadiría peso.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    androidResources { noCompress += listOf("glb", "ktx") }
    // Librerías nativas comprimidas dentro de la APK: la descarga pesa mucho menos (Android las extrae al instalar).
    packaging { jniLibs { useLegacyPackaging = true } }

    // Los packs de vehículos viven en /vehicle-packs (raíz del repositorio) y se empaquetan como assets.
    // El pack incluido se carga con el mismo mecanismo que un pack importado (DECISIONS.md, D-007).
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/packAssets"))
}

// Coches que van DENTRO de la app: el de por defecto y el Focus de prueba (D-024). Los del catálogo (Golf…) se
// descargan aparte desde el Área de cliente (D-026), para que la app no crezca con cada coche.
val bundledPacks = listOf("carvision_default", "ford_focus_mk3_5p")

val copyVehiclePacks by tasks.registering(Sync::class) {
    from(rootProject.file("../vehicle-packs")) {
        // Solo lo que usa un pack en tiempo de ejecución (mismos tipos que admite el importador de .zip).
        // Así nunca se cuelan en la APK descargas originales, archivos de Blender ni material de trabajo.
        val types = listOf("json", "glb", "png", "webp", "jpg", "jpeg", "md", "txt")
        bundledPacks.forEach { pack -> types.forEach { ext -> include("$pack/**/*.$ext") } }
        exclude("**/source/**")
    }
    into(layout.buildDirectory.dir("generated/packAssets/vehicle-packs"))
}
tasks.named("preBuild") { dependsOn(copyVehiclePacks) }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:maintenance"))
    implementation(project(":vehicle:data"))
    implementation(project(":vehicle:packs"))
    implementation(project(":render3d"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)
}
