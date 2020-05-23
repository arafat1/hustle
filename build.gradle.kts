plugins {
    kotlin("jvm") version "1.3.72"
    id("java-gradle-plugin")
}

group = "com.github.arafat1"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.postgresql.postgresql:42.2.12")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}