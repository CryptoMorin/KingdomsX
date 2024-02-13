plugins {
    commons
    spigot
}

group = "org.kingdoms.services.worldedit"
version = "unspecified"

repositories {
    maven {
        name = "sk89q-repo"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":shared"))

    // https://maven.enginehub.org/repo/com/sk89q/worldedit/worldedit-core/
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT")
}