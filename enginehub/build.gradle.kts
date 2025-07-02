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
version = "1.3.0"
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


// This fixes Guava dependency conflict between our project and WorldEdit.
// configurations.all {
//     resolutionStrategy.force("com.google.guava:guava:33.1.0-jre")
// }

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":core:service"))
    compileOnly(project(":shared"))
    compileOnly(project(":platform:bukkit"))
    compileOnly(KingdomsGradleCommons.XSERIES)

    api(project(":enginehub:commons"))
    if (!examineWorldGuardSix) {
        api(project(":enginehub:worldguard"))
        implementation(project(":enginehub:worldguard:v6"))
        implementation(project(":enginehub:worldguard:v7"))
    }

    // compileOnly(project(":schematics"))
    // compileOnly(project(path=":nms:v1.20", configuration="reobf"))

    // https://maven.enginehub.org/repo/com/sk89q/worldedit/worldedit-core/
    if (!examineWorldGuardSix) {
        // We won't use the latest version for now because WorldEdit switched to records for
        // some of their classes (such as BlockVector) and deprecated the getter methods.
        // We will add support for the newer one once they entirely remove those methods.
        val worldEditVersion = "7.2.9"
        compileOnly("com.sk89q.worldedit:worldedit-core:$worldEditVersion") { isTransitive = false }
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:$worldEditVersion") { isTransitive = false }
    } else {
        compileOnly("com.sk89q.worldedit:worldedit-core:6.1.4-SNAPSHOT")
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:6.1.5")
    }
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}