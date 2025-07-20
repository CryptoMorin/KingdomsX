import org.kingdoms.gradle.KingdomsGradleCommons

plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services.maps.dynmap"
version = "1.0.0"

repositories {
    maven {
        url = uri("https://repo.mikeprimm.com/")
        content { includeGroup("us.dynmap") } // Remember, not org.dynmap that one is outdated
    }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":platform:bukkit"))
    compileOnly(project(":map-viewers:commons"))
    compileOnly(KingdomsGradleCommons.XSERIES)

    // https://github.com/webbukkit/dynmap/wiki/Dynmap-API
    // https://github.com/webbukkit/dynmap/tree/v3.0/DynmapCoreAPI/src/main/java/org/dynmap/markers
    // https://repo.mikeprimm.com/us/dynmap/DynmapCoreAPI/
    compileOnly("us.dynmap:DynmapCoreAPI:3.6") { isTransitive = false }
}
