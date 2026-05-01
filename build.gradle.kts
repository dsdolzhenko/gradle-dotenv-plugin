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
    kotlin("jvm") version "2.3.21"
    id("com.palantir.git-version") version "5.0.0"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "io.github.dsdolzhenko"
version = gitVersion()

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    website = "https://github.com/dsdolzhenko/gradle-dotenv-plugin"
    vcsUrl = "https://github.com/dsdolzhenko/gradle-dotenv-plugin"

    plugins {
        register("dotenv") {
            id = "io.github.dsdolzhenko.dotenv"
            implementationClass = "io.github.dsdolzhenko.dotenv.DotEnvPlugin"
            displayName = "Gradle DotEnv Plugin"
            description = "Loads environment variables from .env files into Gradle tasks (JavaExec, Exec, Test)"
            tags.addAll("dotenv", "environment", "env")
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
