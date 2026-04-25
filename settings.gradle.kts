pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForged"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "lockers"

include(
    ":common",
    ":neoforge-1.21.1",
    ":neoforge-1.21.4",
)
