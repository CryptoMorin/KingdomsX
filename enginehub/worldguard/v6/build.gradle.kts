@file:Suppress("VulnerableLibrariesLocal")

plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services.worldguard"
version = "1.0.0"

repositories {
    maven { // https://maven.enginehub.org/repo/com/sk89q/
        name = "sk89q-repo"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

dependencies {
    compileOnly(project(":shared")) { isTransitive = false }
    compileOnly(project(":core:service")) { isTransitive = false }
    compileOnly(project(":enginehub:worldguard")) { isTransitive = false }
    compileOnly("org.checkerframework:checker-qual:3.21.0")
    compileOnly("com.sk89q.worldedit:worldedit-core:6.1.4-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:6.1.5")
    compileOnly("com.sk89q.worldguard:worldguard-legacy:6.2")
}