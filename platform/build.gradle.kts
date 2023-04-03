import org.kingdoms.main.PredefinedDependency.excludeKotlin
import org.kingdoms.main.PredefinedDependency.relocateLibs

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