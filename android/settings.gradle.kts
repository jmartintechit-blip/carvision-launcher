pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CarVisionLauncher"

include(":app")
include(":core:model")
include(":core:designsystem")
include(":core:maintenance")
include(":vehicle:data")
include(":vehicle:packs")
include(":render3d")
