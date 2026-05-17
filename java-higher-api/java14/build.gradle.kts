@file:Suppress("VulnerableLibrariesLocal")

import org.kingdoms.gradle.KingdomsGradleCommons

plugins {
    id("java")
}

group = "org.kingdoms.utils.internal.jdk"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_14
    targetCompatibility = JavaVersion.VERSION_14
}

repositories {
    mavenLocal()

    maven {
        url = uri("https://repo.codemc.org/repository/nms/")
        content {
            includeGroup("org.spigotmc")
        }
    }
    maven { url = uri("https://libraries.minecraft.net/") } // For GameProfile Authlib

    mavenCentral()
}

dependencies {
    // Because Gradle complains about Authlib being compiled with JDK 17,
    // this is the latest version that supported JDK 14
    compileOnly(KingdomsGradleCommons.Spigot.v1_21) {
        exclude(group = "com.mojang", module = "authlib")
    }

    compileOnly("com.mojang:authlib:2.1.28")
}