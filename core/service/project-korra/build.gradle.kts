import org.kingdoms.gradle.KingdomsGradleCommons.localDependency

plugins {
    java
    commons
    spigotapi
    kotlin
}

group = "org.kingdoms.services"
version = "1.0.0"

// Because they said they can't handle the API changes.
dependencies {
    // https://github.com/ProjectKorra/ProjectKorra/wiki
    // https://projectkorra.com/docs/
    compileOnly(project(":core:service"))
    compileOnly(
        localDependency("ProjectKorra-1.11.2.jar")
    ) // They don't have an official API.
}