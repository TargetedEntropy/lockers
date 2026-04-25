plugins {
    java
    alias(libs.plugins.moddev)
}

val modId: String = providers.gradleProperty("mod_id").get()
val modVersion: String = providers.gradleProperty("mod_version").get()
val mcVersion = "1.21.4"
val neoforgeVersion: String = providers.gradleProperty("neoforge_version_1_21_4").get()
val parchmentMc: String = providers.gradleProperty("parchment_mc_1_21_4").get()
val parchmentVer: String = providers.gradleProperty("parchment_version_1_21_4").get()
val curiosVersion: String = providers.gradleProperty("curios_version_1_21_4").get()
val accessoriesVersion: String = providers.gradleProperty("accessories_version_1_21_4").get()

base {
    archivesName.set("$modId-$mcVersion")
}

repositories {
    maven("https://maven.theillusivec4.top/") { name = "Illusive Soulworks (Curios)" }
    maven("https://maven.wispforest.io/") { name = "Wisp Forest (Accessories)" }
    maven("https://maven.su5ed.dev/releases") {
        name = "Sinytra (forgified-fabric-api, transitive of Accessories)"
    }
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

    // Put NeoForge + Minecraft on the gametest source set's classpath.
    addModdingDependenciesTo(sourceSets.named("gametest").get())

    parchment {
        minecraftVersion.set(parchmentMc)
        mappingsVersion.set(parchmentVer)
    }

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
    compileOnly("io.wispforest:accessories-neoforge:$accessoriesVersion")
}

tasks.processResources {
    val replacements = mapOf(
        "mod_id" to modId,
        "mod_name" to providers.gradleProperty("mod_name").get(),
        "mod_version" to modVersion,
        "mod_license" to providers.gradleProperty("mod_license").get(),
        "mod_authors" to providers.gradleProperty("mod_authors").get(),
        "mod_description" to providers.gradleProperty("mod_description").get(),
        "mc_version_range" to "[1.21.4,1.21.5)",
        "neoforge_version_range" to "[21.4,21.5)",
        "loader_version_range" to "[4,)",
    )
    inputs.properties(replacements)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(replacements)
    }
}
