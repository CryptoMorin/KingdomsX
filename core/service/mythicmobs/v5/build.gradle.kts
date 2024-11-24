import org.kingdoms.gradle.KingdomsGradleCommons.localDependency

plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services.mythicmobs"
version = "1.0.0"

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":core:service:mythicmobs"))
    compileOnly(
        localDependency("MythicMobs-5.6.2.jar")
    ) // They didn't upload v5.0.0 to the repo
}