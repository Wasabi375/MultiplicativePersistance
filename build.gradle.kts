plugins {
    kotlin("jvm") version "1.4-M1"
    application
}

version = "1.0-SNAPSHOT"

application {
    mainClassName = "MainKt"
}

repositories {
    mavenCentral()
    maven ("https://dl.bintray.com/kotlin/kotlin-eap")
    maven ("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5-1.4-M1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}