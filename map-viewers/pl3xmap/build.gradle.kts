import org.kingdoms.gradle.KingdomsGradleCommons

plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services.maps.pl3xmap"
version = "1.0.0"

repositories {
    // https://docs.modrinth.com/docs/tutorials/maven/
    // https://github.com/modrinth/labrinth/blob/master/src/routes/maven.rs
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven") // Wtf is this? A REST API?
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":platform:bukkit"))
    compileOnly(project(":map-viewers:commons"))
    compileOnly(KingdomsGradleCommons.XSERIES)

    // The version number is automatically uploaded by Modrinth https://modrinth.com/plugin/pl3xmap
    // https://billygalbreath.github.io/Pl3xMap/net/pl3x/map/core/markers/package-summary.html
    // https://github.com/BillyGalbreath/Pl3xMap/wiki/Pl3xMap-API
    // https://api.modrinth.com/maven/maven/modrinth/pl3xmap/maven-metadata.xml
    // https://api.modrinth.com/maven/maven/modrinth/pl3xmap/1.19.4-423/Pl3xMap-1.19.4-423.jar
    // Note: It appears that modrinth automatically removes older versions from their repo.
    compileOnly("maven.modrinth:pl3xmap:1.21.4-521")
    //    compileOnly( // Modrinth's repo isn't being nice to be for some reasons :<
    //        files(
    //            rootDir.toPath().resolve("local-dependencies").resolve("Pl3xMap-1.19.4-423.jar")
    //        )
    //    )
    // compileOnly("com.github.NeumimTo:Pl3xMap:1.19-6") { isTransitive = false }
}
