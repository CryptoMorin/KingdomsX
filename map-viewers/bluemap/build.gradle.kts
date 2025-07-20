plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services.maps.bluemap"
version = "1.0.0"

repositories {
    maven {
        name = "bluecoloredReleases"
        url = uri("https://repo.bluecolored.de/releases")
    }
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":platform:bukkit"))
    compileOnly(project(":map-viewers:commons"))

    // https://github.com/BlueMap-Minecraft/BlueMapAPI/wiki
    // v2.2.1 ain't working...
    // https://jitpack.io/#BlueMap-Minecraft/BlueMapAPI
    // https://repo.bluecolored.de/#/releases
    // Note: Jitpack can't provide builds that are "failed" (red paper icon on jitpack repo)
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:v2.7.0") // We need the math classes { isTransitive = false }
    // Doesn't work, it doesn't even search in the repo. implementation("de.bluecolored.bluemap:BlueMapAPI:2.7.2")
}
