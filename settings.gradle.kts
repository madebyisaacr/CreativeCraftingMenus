pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    kotlin("jvm") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
    id("dev.kikugie.stonecutter") version "0.8.3"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions("1.21.10", "1.21.11")

        vcsVersion = "1.21.11"
    }
}

rootProject.name = "CreativeCraftingMenus"

