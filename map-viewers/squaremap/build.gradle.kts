plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services.maps.squaremap"
version = "1.0.0"

dependencies {
    // https://repo.mikeprimm.com/us/dynmap/DynmapCoreAPI/
    compileOnly(project(":core"))
    compileOnly(project(":platform:bukkit"))
    compileOnly(project(":map-viewers:commons"))

    // https://github.com/jpenilla/squaremap
    // https://repo1.maven.org/maven2/xyz/jpenilla/squaremap-api/
    compileOnly("xyz.jpenilla", "squaremap-api", "1.2.5") { isTransitive = false }
}
