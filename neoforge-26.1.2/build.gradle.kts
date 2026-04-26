plugins {
    java
    alias(libs.plugins.moddev)
}

val modId: String = providers.gradleProperty("mod_id").get()
val modVersion: String = providers.gradleProperty("mod_version").get()
val mcVersion = "26.1.2"
val neoforgeVersion: String = providers.gradleProperty("neoforge_version_26_1_2").get()
val curiosVersion: String = providers.gradleProperty("curios_version_26_1_2").get()
// Parchment + Accessories: not yet published for MC 26.1.2.
//   - Parchment will be added back when `parchment-26.1.2` lands at
//     https://maven.parchmentmc.org/org/parchmentmc/data/.
//   - Accessories: BridgeSelector falls through to NoopAccessoryBridge, so the
//     mod loads cleanly without it. Add the dependency + Accessories2Bridge
//     when Wisp Forest publishes a 26.1.2 build.

project.version = "$modVersion+$mcVersion"
base {
    archivesName.set("$modId-neoforge")
}

// MC 26.1.2 ships with javaVersion.majorVersion=25 in its launcher manifest;
// every previous MC line we ship for stays on Java 21. Override here so the
// foojay-resolver-convention plugin auto-fetches a Java 25 toolchain.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

repositories {
    maven("https://maven.theillusivec4.top/") { name = "Illusive Soulworks (Curios)" }
    maven("https://maven.blamejared.com") { name = "BlameJared (fallback)" }
}

sourceSets {
    main {
        resources.srcDir(rootProject.layout.projectDirectory.dir("common-resources"))
    }
    create("gametest") {
        java.srcDir("src/gametest/java")
        resources.srcDir("src/gametest/resources")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations {
    named("gametestImplementation") {
        extendsFrom(configurations.implementation.get())
        extendsFrom(configurations.compileOnly.get())
    }
    named("gametestRuntimeOnly") { extendsFrom(configurations.runtimeOnly.get()) }
}

neoForge {
    setVersion(neoforgeVersion)

    addModdingDependenciesTo(sourceSets.named("gametest").get())

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.named("gametest").get())
        }
    }

    runs {
        register("client") {
            client()
            gameDirectory.set(layout.projectDirectory.dir("run/client"))
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("server") {
            server()
            gameDirectory.set(layout.projectDirectory.dir("run/server"))
            programArgument("--nogui")
        }
        register("gameTestServer") {
            type.set("gameTestServer")
            gameDirectory.set(layout.projectDirectory.dir("run/gameTestServer"))
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("data") {
            data()
            gameDirectory.set(layout.projectDirectory.dir("run/data"))
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", layout.projectDirectory.dir("src/generated/resources").asFile.absolutePath,
                "--existing", layout.projectDirectory.dir("src/main/resources").asFile.absolutePath,
                "--existing", rootProject.layout.projectDirectory.dir("common-resources").asFile.absolutePath,
            )
        }
    }
}

dependencies {
    implementation(project(":common"))

    compileOnly("top.theillusivec4.curios:curios-neoforge:$curiosVersion")
}

// Merge `common` module's compiled classes + resources into this module's jar.
tasks.jar {
    dependsOn(project(":common").tasks.named("jar"))
    from(project(":common").sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    val replacements = mapOf(
        "mod_id" to modId,
        "mod_name" to providers.gradleProperty("mod_name").get(),
        "mod_version" to modVersion,
        "mod_license" to providers.gradleProperty("mod_license").get(),
        "mod_authors" to providers.gradleProperty("mod_authors").get(),
        "mod_description" to providers.gradleProperty("mod_description").get(),
        "mc_version_range" to "[26.1.2,26.2)",
        "neoforge_version_range" to "[26.1.2,)",
        "loader_version_range" to "[4,)",
    )
    inputs.properties(replacements)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(replacements)
    }
}
