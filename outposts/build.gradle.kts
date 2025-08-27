plugins {
    commons
    shadowed
    sublibs
    spigotapi
    addon
    buildFiles
}

group = "org.kingdoms"
version = "3.1.2.1"
description = "An event similar to KoTH"

kingdomsAddon {
    addonName.set("Outposts")
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":enginehub"))
    compileOnly(project(":core:service"))
    compileOnly(project(":enginehub"))
}