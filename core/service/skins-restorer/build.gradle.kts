plugins {
    commons
    spigot
}

group = "org.kingdoms.services"
version = "1.0.0"

repositories {
    // https://repo.codemc.io/service/rest/repository/browse/maven-releases/net/skinsrestorer/skinsrestorer-api/
    maven {
        name = "codemc-releases"
        url = uri("https://repo.codemc.org/repository/maven-releases/")
    }
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly("net.skinsrestorer:skinsrestorer-api:14.2.8")
}