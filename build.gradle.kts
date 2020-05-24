plugins {
    kotlin("jvm") version "1.3.72"
    id("java-gradle-plugin")
    `maven-publish`
}

group = "com.github.arafat1"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.postgresql", "postgresql", "42.2.12")
}

gradlePlugin {
    plugins {
        create("hustlePlugin") {
            id = "${group}.hustle"
            implementationClass = "${id}.HustlePlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "${project.group}"
            artifactId = "hustle"
            version = "${project.version}"
            artifact("build/libs/hustle-${version}.jar")
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}