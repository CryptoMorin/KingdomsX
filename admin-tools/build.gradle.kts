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

group = "org.kingdoms"
version = "0.2.0"
description = "Adds support for EngineHub plugins (WorldEdit & WorldGuard) selections & schematic buildings."

kingdomsAddon {
    addonName.set("AdminTools")
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":core:service"))
    compileOnly(project(":shared"))
    compileOnly(project(":platform:bukkit"))
    compileOnly(KingdomsGradleCommons.XSERIES)
    compileOnly(KingdomsGradleCommons.Spigot.LATEST) // For ItemEditor's GameProfile authlib
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}