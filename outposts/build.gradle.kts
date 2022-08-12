plugins {
    id("commons")
    java
}

group = "org.kingdoms"
version = "1.0.0"
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
    maven {
        url = uri("https://repo.codemc.org/repository/nms/")
        content {
            includeGroup("org.spigotmc")
        }
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.19-R0.1-SNAPSHOT")
    compileOnly(project(":core"))
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

