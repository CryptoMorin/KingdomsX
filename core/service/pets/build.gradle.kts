plugins {
    commons
    spigot
}

group = "org.kingdoms.services"
version = "1.0.0"

repositories {
    maven { url = uri("https://repo.codemc.org/repository/nms/") }
    maven { url = uri("https://libraries.minecraft.net/") }
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(files(rootDir.toPath().resolve("local-dependencies").resolve("MCPets-3.0.2.jar"))) // They don't have an official API.
    compileOnly(files(rootDir.toPath().resolve("local-dependencies").resolve("MyPet-3.12-SNAPSHOT-B1727.jar"))) // Local https://wiki.mypet-plugin.de/hooks/hook-types
}