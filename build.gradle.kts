val gitVersion: groovy.lang.Closure<String> by extra

plugins {
    kotlin("jvm") version "2.2.21"
    id("com.palantir.git-version") version "4.2.0"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "io.github.dsdolzhenko"
version = gitVersion()

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        register("dotenv") {
            id = "io.github.dsdolzhenko.dotenv"
            implementationClass = "io.github.dsdolzhenko.dotenv.DotEnvPlugin"
            tags.addAll("dotenv")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        mavenLocal()
    }
}