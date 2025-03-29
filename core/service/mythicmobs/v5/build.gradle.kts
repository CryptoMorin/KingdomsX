plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services.mythicmobs"
version = "1.0.0"

repositories {
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":core:service:mythicmobs"))

    // https://git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/API
    // https://mvn.lumine.io/service/rest/repository/browse/maven-public/io/lumine/Mythic-Dist/
    compileOnly("io.lumine:Mythic-Dist:5.8.0")
}