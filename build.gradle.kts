import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension

plugins {
    base
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.148" apply false
}

val distDir = layout.projectDirectory.dir("dist")

val collectJars = tasks.register<Copy>("collectJars") {
    into(distDir)
}

tasks.named("build") {
    finalizedBy(collectJars)
}

tasks.named<Delete>("clean") {
    delete(distDir)
}

subprojects {
    fun prop(name: String): String = findProperty(name)?.toString() ?: error("Missing property $name in ${project.path}")
    fun optionalProp(name: String): String = findProperty(name)?.toString() ?: ""

    val minecraftVersion = prop("minecraft_version")
    val legacyForge = optionalProp("legacy_forge_version").isNotEmpty()
    val cuiProtocolVersion = optionalProp("cui_protocol_version")
    val cuiProtocolMinecraft = optionalProp("cui_protocol_minecraft").ifEmpty { minecraftVersion }
    val worldEditDevArtifact = optionalProp("worldedit_dev_artifact")
    val accessTransformer = file("src/main/resources/META-INF/accesstransformer.cfg")
    val modVersion = "$minecraftVersion+${prop("mod_revision")}"
    val javaVersion = prop("java_version").toInt()

    apply(plugin = "java-library")
    apply(plugin = if (legacyForge) "net.neoforged.moddev.legacyforge" else "net.neoforged.moddev")

    group = prop("mod_group_id")
    version = modVersion

    extensions.configure<BasePluginExtension> {
        archivesName = "WorldEditCUI-NeoForge"
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    repositories {
        maven("https://maven.enginehub.org/repo/") {
            name = "enginehub"
            content {
                includeGroup("org.enginehub.worldeditcui-protocol")
                includeGroup("com.sk89q.worldedit")
            }
        }
        maven("https://api.modrinth.com/maven") {
            name = "modrinth"
            content { includeGroup("maven.modrinth") }
        }
    }

    val localRuntime = configurations.create("localRuntime")
    configurations.named("runtimeClasspath") { extendsFrom(localRuntime) }

    val mainSourceSet = the<SourceSetContainer>().named("main")

    if (legacyForge) {
        extensions.configure<LegacyForgeExtension> {
            version = "$minecraftVersion-${prop("legacy_forge_version")}"
            if (accessTransformer.exists()) {
                accessTransformers.from(accessTransformer)
            }
            runs {
                register("client") {
                    client()
                    gameDirectory = file("run")
                }
            }
            mods {
                register(prop("mod_id")) { sourceSet(mainSourceSet.get()) }
            }
        }
    } else {
        extensions.configure<NeoForgeExtension> {
            version = prop("neo_version")
            if (accessTransformer.exists()) {
                accessTransformers.from(accessTransformer)
            }
            runs {
                register("client") {
                    client()
                    gameDirectory = file("run")
                }
            }
            mods {
                register(prop("mod_id")) { sourceSet(mainSourceSet.get()) }
            }
        }
    }

    dependencies {
        if (cuiProtocolVersion.isNotEmpty()) {
            val cuiProtocol = "org.enginehub.worldeditcui-protocol:worldeditcui-protocol-neoforge-mc$cuiProtocolMinecraft:$cuiProtocolVersion@jar"
            "implementation"(cuiProtocol)
            "jarJar"(cuiProtocol) {
                (this as ExternalModuleDependency).version {
                    strictly("[$cuiProtocolVersion,)")
                    prefer(cuiProtocolVersion)
                }
            }
        }
        val irisCompileArtifact = optionalProp("iris_compile_artifact")
        if (irisCompileArtifact.isNotEmpty()) {
            "compileOnly"(irisCompileArtifact) {
                (this as ExternalModuleDependency).isTransitive = false
            }
        }
        if (worldEditDevArtifact.isNotEmpty()) {
            val worldEditDevConfiguration = optionalProp("worldedit_dev_configuration").ifEmpty { "localRuntime" }
            worldEditDevConfiguration(worldEditDevArtifact) {
                (this as ExternalModuleDependency).isTransitive = false
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = javaVersion
    }

    val metadata = mapOf(
        "mod_id" to prop("mod_id"),
        "mod_version" to modVersion,
        "minecraft_version_range" to prop("minecraft_version_range"),
        "loader_version_range" to prop("loader_version_range"),
        "platform_mod_id" to if (legacyForge) "forge" else "neoforge",
        "platform_version_range" to prop("platform_version_range"),
        "cui_protocol_version" to cuiProtocolVersion,
        "access_transformer" to accessTransformer.exists(),
        "legacy_metadata" to (optionalProp("legacy_metadata") == "true"),
        "mixin_config" to optionalProp("mixin_config"),
    )
    val metadataFileName = optionalProp("metadata_file").ifEmpty { "neoforge.mods.toml" }

    val generateModMetadata = tasks.register<Copy>("generateModMetadata") {
        inputs.properties(metadata)
        from(rootProject.file("templates/neoforge.mods.toml")) {
            rename { metadataFileName }
            into("META-INF")
            expand(metadata)
        }
        into(layout.buildDirectory.dir("generated/sources/modMetadata"))
    }
    mainSourceSet.get().resources.srcDir(generateModMetadata)
    if (legacyForge) {
        extensions.getByType<LegacyForgeExtension>().ideSyncTask(generateModMetadata)
    } else {
        extensions.getByType<NeoForgeExtension>().ideSyncTask(generateModMetadata)
    }

    val releaseJar = if (legacyForge) tasks.named("reobfJar") else tasks.named("jar")
    collectJars.configure { from(releaseJar) }
    rootProject.tasks.named("build") { dependsOn(tasks.named("build")) }
}
