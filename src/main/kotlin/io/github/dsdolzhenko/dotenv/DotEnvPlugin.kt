package io.github.dsdolzhenko.dotenv

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.process.ProcessForkOptions
import java.io.File

/**
 * Gradle plugin that loads environment variables from .env files
 * and injects them into tasks and subprocesses.
 */
class DotEnvPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create the extension for configuration
        val extension = project.extensions.create("dotenv", DotEnvExtension::class.java, project)

        // Configure after project evaluation to ensure the extension is configured
        project.afterEvaluate {
            if (extension.enabled.get()) {
                configureProject(project, extension)
            }
        }
    }

    private fun configureProject(project: Project, extension: DotEnvExtension) {
        // Load environment variables from .env files
        val envVars = loadEnvironmentVariables(project, extension)

        if (envVars.isEmpty()) {
            if (extension.required.get()) {
                throw IllegalStateException("No .env file found and dotenv.required is true")
            }
            project.logger.info("No .env file found, skipping environment variable injection")
            return
        }

        project.logger.info("Loaded ${envVars.size} environment variables from .env files")

        // Inject into all relevant tasks
        configureAllTasks(project, envVars, extension)
    }

    private fun loadEnvironmentVariables(
        project: Project,
        extension: DotEnvExtension
    ): Map<String, String> {
        val envVars = mutableMapOf<String, String>()

        // Load files in order (later files override earlier ones)
        extension.files.get().forEach { filename ->
            val file = project.rootProject.file(filename)
            if (file.exists()) {
                project.logger.info("Loading environment variables from: ${file.absolutePath}")
                envVars.putAll(parseEnvFile(file))
            } else {
                project.logger.debug(".env file not found: ${file.absolutePath}")
            }
        }

        // System properties override .env values if configured
        if (extension.systemPropertiesOverride.get()) {
            System.getProperties().stringPropertyNames().forEach { key ->
                System.getProperty(key)?.let { value ->
                    envVars[key] = value
                }
            }
        }

        return envVars
    }

    private fun parseEnvFile(file: File): Map<String, String> {
        return DotEnvParser.create().parseFile(file)
    }

    private fun configureAllTasks(
        project: Project,
        envVars: Map<String, String>,
        extension: DotEnvExtension
    ) {
        // Configure all ProcessForkOptions tasks (JavaExec, Exec, Test, etc.)
        project.tasks.withType(JavaExec::class.java).configureEach { task ->
            injectEnvironment(task, envVars, extension)
        }

        project.tasks.withType(Exec::class.java).configureEach { task ->
            injectEnvironment(task, envVars, extension)
        }

        project.tasks.withType(Test::class.java).configureEach { task ->
            injectEnvironment(task, envVars, extension)
        }
    }

    private fun injectEnvironment(
        task: ProcessForkOptions,
        envVars: Map<String, String>,
        extension: DotEnvExtension
    ) {
        // Get the task name for logging
        val taskName = if (task is org.gradle.api.Task) task.name else "unknown"

        // Filter variables based on inclusion/exclusion patterns
        val filteredVars = envVars.filter { (key, _) ->
            val included = extension.include.get().isEmpty() ||
                    extension.include.get().any { pattern ->
                        key.matches(Regex(pattern))
                    }
            val excluded = extension.exclude.get().any { pattern ->
                key.matches(Regex(pattern))
            }
            included && !excluded
        }

        if (filteredVars.isEmpty()) {
            return
        }

        // Inject environment variables into the task
        task.environment(filteredVars)

        if (task is org.gradle.api.Task) {
            task.logger.debug("Injected ${filteredVars.size} environment variables into task: $taskName")
        }
    }
}
