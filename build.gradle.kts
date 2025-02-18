plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "refactoring"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}