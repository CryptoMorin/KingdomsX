plugins {
    commons
    shadowed
    spigot
    addon
    kotlin
    sublibs
}

group = "org.kingdoms.peacetreaties"
version = "1.2.6.0.2"
description = "A contract management for neutral relationships."

buildscript {
    extra.apply {
        set("outputName", "Kingdoms-Addon-Peace-Treaties")
    }
}

dependencies {
    compileOnly(project(":core"))
}