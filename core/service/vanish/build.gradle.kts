plugins {
    commons
    spigot
}

group = "org.kingdoms.services.vanish"
version = "1.0.0"

repositories {
    maven { url = uri("https://repo.essentialsx.net/snapshots/") }
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly("net.essentialsx:EssentialsX:2.20.0-SNAPSHOT") {
        isTransitive = false
    } // https://repo.essentialsx.net/snapshots/net/essentialsx/EssentialsX
    compileOnly("org.cmi:CMI-API:9.0.0") { isTransitive = false } // Local https://github.com/Zrips/CMI-API/issues/6
}