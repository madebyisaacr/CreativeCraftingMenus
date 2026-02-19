plugins {
    id("java-library")
    id("idea")
    id("fabric-loom")
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.kikugie.stonecutter")
    id("dev.kikugie.fletching-table.fabric")
}

fun prop(name: String, consumer: (prop: String) -> Unit) {
    (findProperty(name) as? String?)
        ?.let(consumer)
}

class ModData {
    val version = property("mod.version") as String
    val group = property("mod.group") as String
    val id = property("mod.id") as String
    val name = property("mod.name") as String
    val authors = property("mod.authors") as String
    val description = property("mod.description") as String
    val homepage = property("mod.homepage") as String
    val sources = property("mod.sources") as String
    val issues = property("mod.issues") as String
    val license = property("mod.license") as String
}

val mod = ModData()
val minecraft = property("deps.minecraft") as String
val minecraftRange = property("deps.minecraft_range") as String

version = "${mod.version}+${sc.current.version}"
base.archivesName = mod.id

val requiredJava = when {
    sc.current.parsed >= "1.20.6" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

loom {
    fabricModJsonPath = project.file("build/resources/main/fabric.mod.json") // Useful for interface injection
    accessWidenerPath = project.file("build/resources/main/${mod.id}.classtweaker")

    runConfigs.all {
        ideConfigGenerated(false)
    }

    runs {
        register("testClient") {
            client()
            name = "Test Client"
            vmArgs("-Dmixin.debug.export=true")
            runDir = "../../run"
            ideConfigGenerated(true)
        }
    }
}

fletchingTable {
    mixins.create("main") {
        mixin("default", "${mod.id}.mixins.json") {
            env("CLIENT")
        }
    }
}

configurations.configureEach {
    resolutionStrategy {
        // make sure the desired version of loader is used. Sometimes old versions are pulled in transitively.
        force("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        prop("deps.parchment") { parchment("org.parchmentmc.data:parchment-${minecraft}:${it}@zip") }
    })

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("dev.isxander:yet-another-config-lib:${property("deps.yacl")}-fabric")
    modImplementation("eu.pb4:placeholder-api:${property("deps.placeholder_api")}")

    modCompileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    processResources {
        fun inputProps(props: Map<String, Any>): Map<String, Any> {
            inputs.properties(*props.map { entry -> entry.key to entry.value }.toTypedArray() )
            return props
        }

        val props = inputProps(mapOf(
            "mod_id" to mod.id,
            "mod_name" to mod.name,
            "mod_version" to version,
            "mod_group" to mod.group,
            "mod_author" to mod.authors,
            "mod_license" to mod.license,
            "mod_description" to mod.description,
            "mod_homepage" to mod.homepage,
            "mod_sources" to mod.sources,
            "mod_issues" to mod.issues,
            "mod_author_list" to mod.authors.split(", ").joinToString("\",\""),
            "minecraft" to sc.current.version,
            "minecraft_range" to minecraftRange
        ))
        filesMatching("fabric.mod.json") { expand(props) }

        val mixinProps = inputProps(mapOf(
            "compatibility_level" to "JAVA_${requiredJava.majorVersion}"
        ))
        filesMatching("*.mixins.json") { expand(mixinProps) }
    }

    validateAccessWidener {
        dependsOn("processResources")
    }

    register<Copy>("buildAndCollect") {
        group = "build"

        from(layout.buildDirectory.dir("libs"))
        include("*.jar")
        into(rootProject.layout.buildDirectory.file("libs/${mod.version}"))

        dependsOn("build")
    }

    register<Delete>("buildCollectAndClean") {
        group = "build"

        delete(layout.buildDirectory.dir("libs"))
        delete(layout.buildDirectory.dir("devlibs"))

        dependsOn("buildAndCollect")
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}