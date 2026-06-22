pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from("no.nordicsemi.android.gradle:version-catalog:2.6.1")
        }
    }
}
rootProject.name = "nRF FanFrigo"

include(":app")
include(":scanner")
include(":blinky:spec")
include(":blinky:ble")
include(":blinky:ui", ":blinky:ble")


// Clone https://github.com/NordicPlayground/Android-Common-Libraries and
// uncomment the following lines to modify source code of the Nordic Common library:
//if (file("../Android-Common-Libraries").exists()) {
//    includeBuild("../Android-Common-Libraries")
//}

// Clone https://github.com/NordicSemiconductor/Android-BLE-Library and
// uncomment the following lines to modify source code of the BLE library:
//if (file("../Android-BLE-Library").exists()) {
//    includeBuild("../Android-BLE-Library")
//}
