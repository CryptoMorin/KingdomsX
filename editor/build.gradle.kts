plugins {
    commons
}

group = "org.kingdoms.editor"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("com.squareup.okhttp3:okhttp:5.5.1")
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly(project(":kingdoms-annotations"))

    testImplementation("com.squareup.okhttp3:okhttp:5.5.1")
    testImplementation("com.google.code.gson:gson:2.10.1")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}