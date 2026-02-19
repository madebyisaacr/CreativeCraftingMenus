plugins {
    id("fabric-loom") version "1.14-SNAPSHOT" apply false
    id("dev.kikugie.stonecutter")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22" apply false
}

stonecutter active "1.21.11"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
            replace(".location()", ".identifier()")
            replace("(Object2IntMap.Entry<Holder<Enchantment>>) Map.entry", "Object2IntMap.entry")
        }
    }
}

stonecutter handlers {
    inherit("accesswidener", "classtweaker")
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.parchmentmc.org")
        maven("https://maven.isxander.dev/releases")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.nucleoid.xyz/")
    }
}