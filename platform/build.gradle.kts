import org.kingdoms.main.KingdomsGradleCommons.excludeKotlin
import org.kingdoms.main.KingdomsGradleCommons.relocateLibs

plugins {
    commons
    kotlin
    shadowed
}

group "org.kingdoms.platform"
version "unspecified"

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}