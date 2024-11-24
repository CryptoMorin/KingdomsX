import org.kingdoms.gradle.KingdomsGradleCommons

plugins {
    commons
}

group = "org.kingdoms.platform.bukkit"
version = "unspecified"

repositories {
    maven {
        url = uri("https://repo.codemc.org/repository/nms/")
        content {
            includeGroup("org.spigotmc")
        }
    }
}

dependencies {
    compileOnly(project(":shared"))
    compileOnly(project(":nbt"))
    compileOnly(project(":platform:bukkit:common"))
    compileOnly(KingdomsGradleCommons.Spigot.v1_12)
    compileOnly(KingdomsGradleCommons.XSERIES)
}