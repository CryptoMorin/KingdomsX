plugins {
    id("java")
    id("com.gradleup.shadow")
}

group = "org.kingdoms.remapper"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":libmanager"))
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    withType<Jar> {
        manifest {
            attributes["Main-Class"] = "org.kingdoms.remapper.KingdomsRemapper"
        }
    }
    register<JavaExec>("runJarSplitter") {
        description = "Run the JarPackageSplitter with input JAR and output directory"
        group = "application"
        mainClass.set("org.kingdoms.remapper.KingdomsRemapper")
        classpath = sourceSets.main.get().runtimeClasspath
        args("output_directory/input.jar", "output_directory/output.jar") // Default args; override with --args
    }
    test {
        useJUnitPlatform()
    }
    build {
        dependsOn(shadowJar)
    }
}