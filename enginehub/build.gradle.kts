import org.kingdoms.gradle.KingdomsGradleCommons
import org.kingdoms.gradle.KingdomsGradleCommons.excludeKotlin
import org.kingdoms.gradle.KingdomsGradleCommons.relocateLibs

plugins {
    commons
    spigotapi
    addon
    sublibs
    shadowed
    buildFiles
}

group = "org.kingdoms.enginehub"
version = "1.0.1.0.0.1"
description = "Adds support for EngineHub plugins (WorldEdit & WorldGuard) selections & schematic buildings."

kingdomsAddon {
    addonName.set("EngineHub")
}

repositories {
    maven {
        name = "sk89q-repo"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

val examineWorldGuardSix = false

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":core:service"))
    compileOnly(project(":shared"))
    compileOnly(project(":platform:bukkit"))
    compileOnly(KingdomsGradleCommons.XSERIES)

    if (!examineWorldGuardSix) {
        api(project(":enginehub:worldguard"))
        implementation(project(":enginehub:worldguard:v6"))
        implementation(project(":enginehub:worldguard:v7"))
    }

    // compileOnly(project(":schematics"))
    // compileOnly(project(path=":nms:v1.20", configuration="reobf"))

    // https://maven.enginehub.org/repo/com/sk89q/worldedit/worldedit-core/
    if (!examineWorldGuardSix) {
        compileOnly("com.sk89q.worldedit:worldedit-core:7.2.9")
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
    } else {
        compileOnly("com.sk89q.worldedit:worldedit-core:6.1.4-SNAPSHOT")
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:6.1.5")
    }
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}