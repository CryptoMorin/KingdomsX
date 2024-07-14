plugins {
    commons
    spigot
}

group = "org.kingdoms.services.worldguard"
version = "1.0.0"

repositories {
    maven { // https://maven.enginehub.org/repo/com/sk89q/
        name = "sk89q-repo"
        url = uri("https://maven.enginehub.org/repo/")
        //        content {
        // https://maven.enginehub.org/repo/com/mojang/authlib/
        //            includeGroup("com.sk89q.worldedit")
        //            includeGroup("com.sk89q.worldguard")
        //        }
    }
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":core"))
    compileOnly(project(":shared"))
    compileOnly("org.checkerframework:checker-qual:3.21.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.1.0-SNAPSHOT")
}