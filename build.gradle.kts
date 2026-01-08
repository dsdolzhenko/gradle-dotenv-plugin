val gitVersion: groovy.lang.Closure<String> by extra

val githubUsername: String by extra {
    findProperty("githubUsername")?.toString()
        ?: System.getenv("GITHUB_ACTOR")?.ifEmpty { null }
        ?: ""
}

val githubToken: String by extra {
    findProperty("githubToken")?.toString()
        ?: System.getenv("GITHUB_TOKEN")?.ifEmpty { null }
        ?: ""
}

plugins {
    kotlin("jvm") version "2.3.0"
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
    testImplementation(gradleTestKit())
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

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/dsdolzhenko/gradle-dotenv-plugin")
            credentials {
                username = githubUsername
                password = githubToken
            }
        }
    }
}