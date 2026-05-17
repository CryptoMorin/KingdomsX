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
        //        content {
        // https://maven.enginehub.org/repo/com/mojang/authlib/
        //            includeGroup("com.sk89q.worldedit")
        //            includeGroup("com.sk89q.worldguard")
        //        }
    }
}

dependencies {
    compileOnly(project(":shared")) { isTransitive = false }
    compileOnly(project(":core:service")) { isTransitive = false }
    compileOnly(project(":enginehub:worldguard")) { isTransitive = false }
    compileOnly("org.checkerframework:checker-qual:3.21.0")

    // https://maven.enginehub.org/repo/com/sk89q/worldedit/
    // Don't change the version to the latest, because they switched to using record classes.
    // Exclude all org.apache.logging.log4j:log4j-bom:{strictly 2.24.1}
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.9") {
        exclude(group = "org.apache.logging.log4j")
    }

    // https://maven.enginehub.org/repo/com/sk89q/worldguard/worldguard-core/
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.1.0-SNAPSHOT") {
        exclude(group = "org.apache.logging.log4j")
    }
}
