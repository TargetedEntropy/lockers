plugins {
    base
}

// Intentionally NOT setting project.version at the root or in allprojects — it
// clashes with the `version` property inside MDG's `neoForge { ... }` extension
// block in Kotlin DSL. Per-module build files set their own versions.
allprojects {
    group = providers.gradleProperty("mod_group_id").get()

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
            withSourcesJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(21)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
