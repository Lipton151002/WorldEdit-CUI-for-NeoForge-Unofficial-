pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "WorldEditCUI-NeoForge"

file("versions").listFiles { file -> file.isDirectory && file.resolve("gradle.properties").isFile }
    ?.sortedBy { it.name }
    ?.forEach { dir ->
        include(":${dir.name}")
        project(":${dir.name}").projectDir = dir
    }
