plugins {
    commons
    sublibs
    spigotapi
}

group = "org.kingdoms.services.maps.abstraction"
version = "1.0.0"

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":shared"))
}
