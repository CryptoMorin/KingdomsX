import org.kingdoms.gradle.KingdomsGradleCommons

plugins {
    commons
    sublibs
    spigot
}

group = "org.kingdoms.platform.bukkit"
version = "unspecified"

dependencies {
    compileOnly(project(":shared"))
    compileOnly(project(":nbt"))
    compileOnly(project(":java-higher-api:java14"))
    api(project(":platform:bukkit:old-bukkit"))
    api(project(":platform:bukkit:common"))
    compileOnly(KingdomsGradleCommons.XSERIES)
}
