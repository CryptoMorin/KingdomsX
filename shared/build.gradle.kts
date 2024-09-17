plugins {
    commons
    sublibs
    // antlr
}

group = "org.kingdoms"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.antlr/antlr4
    api(project(":nbt"))
    // antlr("org.antlr:antlr4:4.13.1")
    // compileOnly("it.unimi.dsi:fastutil-core:8.5.13")

    compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.2")
    compileOnly("org.ow2.asm:asm:9.4") { isTransitive = false }
    compileOnly("org.ow2.asm:asm-commons:9.4") { isTransitive = false }
}