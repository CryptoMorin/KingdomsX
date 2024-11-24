plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services"
version = "unspecified"

repositories {
    maven("https://repo.xenondevs.xyz/releases")
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":platform:bukkit"))
    compileOnly(project(":shared"))

    // https://xenondevs.xyz/docs/nova/api/
    // https://repo.xenondevs.xyz/#/releases/xyz/xenondevs/nova/nova-api/
    // What's "Nova-Api"?
    compileOnly("xyz.xenondevs.nova:nova-api:0.16.1")
}
