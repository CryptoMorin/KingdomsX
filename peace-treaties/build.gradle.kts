import org.kingdoms.main.PredefinedDependency.excludeKotlin
import org.kingdoms.main.PredefinedDependency.relocateLibs

plugins {
    commons
    shadowed
    spigot
    addon
    kotlin
    sublibs
}

group = "org.kingdoms.peacetreaties"
version = "1.1.3"
description = "A contract management for neutral relationships."

buildscript {
    extra.apply {
        set("outputName", "Kingdoms-Addon-Peace-Treaties")
    }
}

dependencies {
    compileOnly(project(":core"))
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}