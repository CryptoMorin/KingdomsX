plugins {
    java
    commons
}

group = "org.kingdoms.services"
version = "1.0.0"

repositories {
    maven {
        name = "codemc-repo"
        url = uri("https://repo.codemc.org/repository/maven-public/")
        content {
            includeGroup("fr.xephi") // https://repo.codemc.io/service/rest/repository/browse/maven-public/fr/xephi/authme/
        }
    }
}

dependencies {
    compileOnly(project(":core:service"))
    compileOnly(org.kingdoms.main.PredefinedDependency.Spigot.LATEST)
    compileOnly("fr.xephi:authme:5.6.0-SNAPSHOT") { isTransitive = false } // https://github.com/AuthMe/AuthMeReloaded
}