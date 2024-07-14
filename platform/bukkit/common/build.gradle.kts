import org.kingdoms.gradle.KingdomsGradleCommons

plugins {
    commons
}

group = "org.kingdoms"
version = "unspecified"

dependencies {
    compileOnly(project(":shared"))
    compileOnly(KingdomsGradleCommons.Spigot.LATEST)
    compileOnly(KingdomsGradleCommons.XSERIES)
}
