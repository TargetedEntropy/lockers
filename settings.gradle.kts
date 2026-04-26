pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForged"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lockers"

include(
    ":common",
    ":neoforge-1.21.1",
    ":neoforge-1.21.4",
    ":neoforge-26.1.2",
)
