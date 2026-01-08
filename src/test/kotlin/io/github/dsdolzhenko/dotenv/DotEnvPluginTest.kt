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
        // Create a settings.gradle.kts file
        projectDir.resolve("settings.gradle.kts").writeText("")

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

        // Create a settings.gradle.kts file
        projectDir.resolve("settings.gradle.kts").writeText("")

        buildFile.writeText(
            """
            plugins {
                id("io.github.dsdolzhenko.dotenv")
            }

            tasks.register<Exec>("testExec") {
                commandLine("printenv", "TEST_VAR")
                doLast {
                    val testVar = environment["TEST_VAR"]
                    val dbHost = environment["DB_HOST"]
                    val dbPort = environment["DB_PORT"]

                    println("ENV_CHECK:TEST_VAR=${'$'}testVar")
                    println("ENV_CHECK:DB_HOST=${'$'}dbHost")
                    println("ENV_CHECK:DB_PORT=${'$'}dbPort")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("testExec")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":testExec")?.outcome)
        assertTrue(result.output.contains("ENV_CHECK:TEST_VAR=test_value"),
            "Expected TEST_VAR to be set to test_value, but output was: ${result.output}")
        assertTrue(result.output.contains("ENV_CHECK:DB_HOST=localhost"))
        assertTrue(result.output.contains("ENV_CHECK:DB_PORT=5432"))
    }

    @Test
    fun `plugin works when disabled`() {
        envFile.writeText("TEST_VAR=test_value")

        // Create a settings.gradle.kts file
        projectDir.resolve("settings.gradle.kts").writeText("")

        buildFile.writeText(
            """
            plugins {
                id("io.github.dsdolzhenko.dotenv")
            }

            dotenv {
                enabled.set(false)
            }

            tasks.register<Exec>("testDisabled") {
                commandLine("echo", "test")
                doLast {
                    val testVar = environment["TEST_VAR"]
                    println("ENV_CHECK:TEST_VAR=${'$'}testVar")
                    println("Task executed")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("testDisabled")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":testDisabled")?.outcome)
        assertTrue(result.output.contains("Task executed"))
        // When disabled, environment variables should NOT be injected
        assertTrue(result.output.contains("ENV_CHECK:TEST_VAR=null"))
    }

    @Test
    fun `plugin handles missing dotenv file gracefully`() {
        // Create a settings.gradle.kts file
        projectDir.resolve("settings.gradle.kts").writeText("")

        buildFile.writeText(
            """
            plugins {
                id("io.github.dsdolzhenko.dotenv")
            }

            tasks.register<Exec>("testTask") {
                commandLine("echo", "test")
                doLast {
                    val testVar = environment["TEST_VAR"]
                    println("ENV_CHECK:TEST_VAR=${'$'}testVar")
                    println("Task executed successfully")
                }
            }
            """.trimIndent()
        )

        // Don't create .env file - test that plugin handles missing file gracefully
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("testTask")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":testTask")?.outcome)
        // Verify that the task runs successfully even without .env file
        assertTrue(result.output.contains("Task executed successfully"))
        // Verify that TEST_VAR is not set (should be null)
        assertTrue(result.output.contains("ENV_CHECK:TEST_VAR=null"))
    }
}
