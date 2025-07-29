import org.kingdoms.gradle.KingdomsGradleCommons.excludeKotlin
import org.kingdoms.gradle.KingdomsGradleCommons.relocateLibs

plugins {
    commons
    spigotapi
    shadowed
    sublibs
    addon
    buildFiles
}

group = "org.kingdoms.services.maps"
version = "3.1.0"
description = "Adds support to online map viewer plugins"

kingdomsAddon {
    addonName.set("Map-Viewers")
}

dependencies {
    arrayOf("commons", "dynmap", "squaremap", "pl3xmap", "bluemap")
        .forEach { implementation(project(":map-viewers:$it")) }
    // api("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}