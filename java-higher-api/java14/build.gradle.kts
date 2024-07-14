import org.kingdoms.gradle.KingdomsGradleCommons

plugins {
    id("java")
}

group = "org.kingdoms.utils.internal.jdk"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_14
    targetCompatibility = JavaVersion.VERSION_14
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.codemc.org/repository/nms/")
}

dependencies {
    compileOnly(KingdomsGradleCommons.Spigot.LATEST)
}