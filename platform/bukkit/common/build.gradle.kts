import org.kingdoms.gradle.KingdomsGradleCommons

plugins {
    commons
    spigotapi
}

group = "org.kingdoms"
version = "unspecified"

dependencies {
    compileOnly(project(":shared"))
    compileOnly(KingdomsGradleCommons.XSERIES)
}
