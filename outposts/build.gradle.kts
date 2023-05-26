plugins {
    commons
    shadowed
    sublibs
    spigot
    addon
}

group = "org.kingdoms"
version = "3.0.1.4"
description = "An event similar to KoTH"

buildscript {
    extra.apply {
        set("outputName", "Kingdoms-Addon-Outposts")
    }
}

dependencies {
    compileOnly(project(":core"))
    implementation(project(":core:service"))
    compileOnly(project(":core:service:worldguard"))
    compileOnly(project(":core:service:worldguard:v6"))
    compileOnly(project(":core:service:worldguard:v7"))
}