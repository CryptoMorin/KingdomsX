plugins {
    commons
    sublibs
}

group = "org.kingdoms.platform.geyser"
version = "unspecified"

repositories {
    // https://github.com/GeyserMC/Cumulus
    // https://wiki.geysermc.org/geyser/using-geyser-or-floodgate-as-a-dependency/
    maven {
        name = "opencollab-snapshot"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
}

dependencies {
    compileOnly(project(":shared"))

    // Idk this gives an error
    // compileOnly("org.geysermc.geyser:api:2.1.0-SNAPSHOT")
    // https://wiki.geysermc.org/floodgate/api/
    compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
}