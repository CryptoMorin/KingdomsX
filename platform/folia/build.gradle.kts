import org.kingdoms.gradle.KingdomsGradleCommons.excludeKotlin
import org.kingdoms.gradle.KingdomsGradleCommons.relocateLibs

plugins {
    commons
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
    compileOnly(project(":shared"))

    // https://repo.papermc.io/service/rest/repository/browse/maven-public/dev/folia/folia-api/
    // https://github.com/PaperMC/Folia/blob/master/README.md#the-new-rules
    // https://github.com/PaperMC/Folia
    compileOnly("dev.folia:folia-api:1.21.6-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    excludeKotlin()
    relocateLibs()
}