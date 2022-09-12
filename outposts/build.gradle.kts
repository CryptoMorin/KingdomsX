plugins {
    id("commons")
    java
    id("com.github.johnrengelman.shadow") version ("7.1.2")
}

group = "org.kingdoms"
version = "2.0.0"
description = "An event similar to KoTH"

buildscript {
    extra.apply {
        set("inputName", "Kingdoms-Addon-Outposts")
        set("outputName", "Kingdoms-Addon-Outposts")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { // For mojang authlib
        name = "minecraft-repo"
        url = uri("https://libraries.minecraft.net")
    }
    maven {
        url = uri("https://repo.codemc.org/repository/nms/")
        content {
            includeGroup("org.spigotmc")
        }
    }
}

dependencies {
    compileOnly(org.kingdoms.main.PredefinedDependency.Spigot.LATEST)
    implementation(project(path = ":core", configuration = "shadow")) // https://imperceptiblethoughts.com/shadow/multi-project/#depending-on-the-shadow-jar-from-another-project
}

tasks {
    val outputName: String? by project.extra
    jar {
        archiveBaseName.set(outputName)
    }
    processResources {
        filesMatching("plugin.yml") {
            expand(
                mapOf(
                    "project" to mapOf( // Maven compatible
                        "version" to project.version,
                        "description" to project.description,
                        "name" to outputName
                    )
                )
            )
        }
    }

    build {
        finalizedBy(named("copyAddon"))
    }
}

