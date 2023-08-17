import org.kingdoms.main.KingdomsGradleCommons.excludeKotlin
import org.kingdoms.main.KingdomsGradleCommons.relocateLibs

plugins {
    commons
    kotlin
    shadowed
}

group = "org.kingdoms.platform.folia"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(project(":platform"))

    // https://github.com/PaperMC/Folia/blob/master/README.md#the-new-rules
    // https://github.com/PaperMC/Folia
    compileOnly("dev.folia:folia-api:1.20.1-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}