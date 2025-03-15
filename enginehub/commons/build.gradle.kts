import org.kingdoms.gradle.KingdomsGradleCommons.excludeKotlin
import org.kingdoms.gradle.KingdomsGradleCommons.relocateLibs

plugins {
    commons
    spigotapi
    sublibs
    shadowed
}

group = "org.kingdoms.enginehub"
version = "unspecified"

dependencies {
    compileOnly(project(":core"))
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}