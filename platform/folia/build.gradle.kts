import org.kingdoms.main.KingdomsGradleCommons.excludeKotlin
import org.kingdoms.main.KingdomsGradleCommons.relocateLibs

plugins {
    commons
    kotlin
    shadowed
}

group "org.kingdoms.platform.folia"
version "unspecified"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(project(":platform"))
    compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT") // https://github.com/PaperMC/Folia
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}