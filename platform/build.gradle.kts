import org.kingdoms.gradle.KingdomsGradleCommons.excludeKotlin
import org.kingdoms.gradle.KingdomsGradleCommons.relocateLibs

plugins {
    commons
    shadowed
}

group = "org.kingdoms.platform"
version = "unspecified"

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}