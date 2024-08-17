plugins {
    commons
    spigot
}

group = "org.kingdoms.services"
version = "1.0.0"

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(project(":shared"))
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false } // https://github.com/MilkBowl/VaultAPI
}