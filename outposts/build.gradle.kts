plugins {
    commons
    shadowed
    sublibs
    spigot
    addon
}

group = "org.kingdoms"
version = "3.0.1.6.3"
description = "An event similar to KoTH"

buildscript {
    extra.apply {
        set("outputName", "Kingdoms-Addon-Outposts")
    }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":enginehub"))
    compileOnly(project(":core:service"))
    compileOnly(project(":enginehub"))
}