import org.kingdoms.main.PredefinedDependency.excludeKotlin
import org.kingdoms.main.PredefinedDependency.relocateLibs

plugins {
    commons
    kotlin
    shadowed
}

group = "org.kingdoms.bedrock"
version = "1.0.0"
description = "Used for using bedrock specific features from Geyser."


repositories {
    // https://github.com/GeyserMC/Cumulus
    // https://wiki.geysermc.org/geyser/using-geyser-or-floodgate-as-a-dependency/
    maven {
        name = "opencollab-snapshot"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
    maven {
        name = "opencollab-snapshot-repo"
        url = uri("https://repo.opencollab.dev/main/")
    }
}

dependencies {
    // Idk this gives an error
    // compileOnly("org.geysermc.geyser:api:2.1.0-SNAPSHOT")

    // https://wiki.geysermc.org/floodgate/api/
    compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
    compileOnly(project(":core"))
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}