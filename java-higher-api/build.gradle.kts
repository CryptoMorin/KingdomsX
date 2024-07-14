plugins {
    id("java")
}

group = "org.kingdoms.utils.internal.jdk"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
