plugins {
    id("java")
}

group = "org.kingdoms.services"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.codemc.org/repository/nms/") }
    maven { url = uri("https://libraries.minecraft.net/") }
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(org.kingdoms.main.PredefinedDependency.Spigot.LATEST)
    compileOnly(files(rootDir.toPath().resolve("local-dependencies").resolve("MCPets-3.0.2.jar")))
}