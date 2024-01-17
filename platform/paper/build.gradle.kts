plugins {
    java
    kotlin
    sublibs
}

group = "org.kingdoms.utils.paper"
description = "Used for defining unique optimization techniques from Paper."
version = "1.0.0"

repositories {
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // https://papermc.io/using-the-api#gradle
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
}

// Paper is compiled with the latest LTS, so we "trick" gradle into using the latest Java version,
// but we compile with Java 8
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    disableAutoTargetJvm()
}
tasks.compileJava {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(8)
}