plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services"
version = "1.0.0"

repositories {
    // For Vault
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://jitpack.io")
            }
        }

        filter {
            includeModule("com.github.MilkBowl", "VaultAPI")
        }
    }
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":shared"))

    // https://jitpack.io/#Milkbowl/VaultAPI/1.7.1
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") { isTransitive = false } // https://github.com/MilkBowl/VaultAPI
}