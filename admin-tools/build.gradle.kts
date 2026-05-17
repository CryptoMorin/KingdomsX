import org.kingdoms.gradle.KingdomsGradleCommons.compileOnlyXSeries

plugins {
    commons
    // For ItemEditor's GameProfile authlib
    spigot
    addon
    sublibs
    shadowed
    buildFiles
}

group = "org.kingdoms"
version = "1.2.3.2"
description = "Adds support for EngineHub plugins (WorldEdit & WorldGuard) selections & schematic buildings."

kingdomsAddon {
    addonName.set("Admin-Tools")
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":core:service"))
    compileOnly(project(":shared"))
    compileOnly(project(":platform:bukkit"))
    compileOnlyXSeries()
}
