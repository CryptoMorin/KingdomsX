import org.kingdoms.gradle.KingdomsGradleCommons.localDependency

plugins {
    commons
    spigot
}

group = "org.kingdoms.services.vanish"
version = "2.0.0"

repositories {
    //maven("https://repo.essentialsx.net/snapshots/")
    maven("https://repo.essentialsx.net/releases/")
}

dependencies {
    compileOnly(project(":core:service"))

    // https://repo.essentialsx.net/#/snapshots/net/essentialsx/EssentialsX
    // https://repo.essentialsx.net/#/releases/net/essentialsx/EssentialsX
    // https://jd-v2.essentialsx.net/
    // https://github.com/EssentialsX/Essentials/
    compileOnly("net.essentialsx:EssentialsX:2.20.1") { isTransitive = false }

    // https://github.com/Zrips/CMI-API/releases
    // https://www.zrips.net/cmi/api/
    // compileOnly("org.cmi:CMI-API:9.3.1.5") { isTransitive = false }
    // https://jitpack.io/#Zrips/CMI-API Builds are failing...
    compileOnly(localDependency("CMI-API-9.3.1.5.jar"))
}