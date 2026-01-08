package io.github.dsdolzhenko.dotenv

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotEnvPluginTest {

    @TempDir
    lateinit var projectDir: File

    private val buildFile: File
        get() = projectDir.resolve("build.gradle.kts")

    private val envFile: File
        get() = projectDir.resolve(".env")

    @Test
    fun `plugin applies successfully`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.dsdolzhenko.dotenv")
            }

            tasks.register("printEnv") {
                doLast {
                    println("Plugin applied successfully")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("printEnv")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printEnv")?.outcome)
        assertTrue(result.output.contains("Plugin applied successfully"))
    }

    @Test
    fun `plugin loads environment variables from dotenv file`() {
        // Create a .env file
        envFile.writeText(
            """
            TEST_VAR=test_value
            DB_HOST=localhost
            DB_PORT=5432
            """.trimIndent()
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.dsdolzhenko.dotenv")
            }

            tasks.register<Exec>("testExec") {
                commandLine("echo", "test")
                doFirst {
                    println("Environment variables: ${'$'}{environment}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("testExec", "--info")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":testExec")?.outcome)
        assertTrue(result.output.contains("Loaded 3 environment variables from .env files"))
    }

    @Test
    fun `plugin works when disabled`() {
        envFile.writeText("TEST_VAR=test_value")

        buildFile.writeText(
            """
            plugins {
                id("io.github.dsdolzhenko.dotenv")
            }

            dotenv {
                enabled.set(false)
            }

            tasks.register("printStatus") {
                doLast {
                    println("Task executed")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("printStatus")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printStatus")?.outcome)
    }

    @Test
    fun `plugin handles missing dotenv file gracefully`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.dsdolzhenko.dotenv")
            }

            tasks.register("testTask") {
                doLast {
                    println("Task executed successfully")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("testTask", "--info")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":testTask")?.outcome)
        assertTrue(result.output.contains("No .env file found, skipping environment variable injection"))
    }
}
