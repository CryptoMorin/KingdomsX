plugins {
    commons
    shadowed
    spigotapi
    kotlin
    sublibs
    addon
    buildFiles
}

group = "org.kingdoms.peacetreaties"
version = "1.2.7"
description = "A contract management for neutral relationships."

kingdomsAddon {
    addonName.set("Peace-Treaties")
}

dependencies {
    compileOnly(project(":core"))
}