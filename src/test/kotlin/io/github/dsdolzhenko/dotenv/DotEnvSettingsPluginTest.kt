package io.github.dsdolzhenko.dotenv

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotEnvSettingsPluginTest {

    @TempDir
    lateinit var projectDir: File

    private val settingsFile: File
        get() = projectDir.resolve("settings.gradle.kts")

    private val buildFile: File
        get() = projectDir.resolve("build.gradle.kts")

    private val envFile: File
        get() = projectDir.resolve(".env")

    @Test
    fun `settings plugin exposes dotenv values at configuration time in build script`() {
        envFile.writeText(
            """
            USERNAME=alice
            TOKEN=secret-token
            """.trimIndent()
        )

        settingsFile.writeText(
            """
            plugins {
                id("io.github.dsdolzhenko.dotenv.settings")
            }
            """.trimIndent()
        )

        buildFile.writeText(
            """
            val username: String = dotenv.get("USERNAME") ?: ""
            val token: String = dotenv["TOKEN"] ?: ""

            tasks.register("printCreds") {
                doLast {
                    println("CRED_CHECK:username=${'$'}username")
                    println("CRED_CHECK:token=${'$'}token")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("printCreds")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printCreds")?.outcome)
        assertTrue(result.output.contains("CRED_CHECK:username=alice"))
        assertTrue(result.output.contains("CRED_CHECK:token=secret-token"))
    }
}
