plugins {
    commons
    spigot
}

group = "org.kingdoms.services.mythicmobs"
version = "1.0.0"

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":core:service:mythicmobs"))
    compileOnly(
        files(
            rootDir.toPath().resolve("local-dependencies").resolve("MythicMobs-5.1.0.jar")
        )
    ) // They didn't upload v5.0.0 to the repo
}