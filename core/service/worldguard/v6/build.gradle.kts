plugins {
    java
    commons
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
    compileOnly(project(":core:service"))
    compileOnly(project(":core:service:worldguard"))
    compileOnly(org.kingdoms.main.PredefinedDependency.Spigot.LATEST)
    compileOnly("com.sk89q.worldedit:worldedit-core:6.1.4-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-legacy:6.2")
}