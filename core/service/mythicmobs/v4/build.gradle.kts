plugins {
    java
    commons
}

group = "org.kingdoms.services.mythicmobs"
version = "1.0.0"

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":core:service:mythicmobs"))
    compileOnly(org.kingdoms.main.PredefinedDependency.Spigot.LATEST)

    // https://mvn.lumine.io/service/rest/repository/browse/maven-public/io/lumine/xikage/MythicMobs/
    // The latest is 4.14.1 which they didn't upload, but it doesn't matter for what we want.
    compileOnly("io.lumine.xikage:MythicMobs:4.12.0") { isTransitive = false }
}