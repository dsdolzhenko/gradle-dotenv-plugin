package io.github.dsdolzhenko.dotenv

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the DotEnv plugin.
 *
 * Example usage in build.gradle.kts:
 * ```
 * dotenv {
 *     enabled.set(true)
 *     files.set(listOf(".env", ".env.local"))
 *     systemPropertiesOverride.set(true)
 *     include.set(listOf("APP_.*", "DATABASE_.*"))
 *     exclude.set(listOf(".*_SECRET"))
 * }
 * ```
 */
abstract class DotEnvExtension(private val project: Project) {

    /**
     * Whether the plugin is enabled. Default: true
     */
    abstract val enabled: Property<Boolean>

    /**
     * List of .env files to load, relative to the project root.
     * Files are loaded in order, later files override earlier ones.
     * Default: [".env"]
     */
    abstract val files: ListProperty<String>

    /**
     * Whether to throw an error if no .env file is found.
     * Default: false
     */
    abstract val required: Property<Boolean>

    /**
     * Whether system properties should override .env values.
     * Useful for CI/CD environments.
     * Default: true
     */
    abstract val systemPropertiesOverride: Property<Boolean>

    /**
     * List of regex patterns for environment variables to include.
     * If empty, all variables are included (subject to exclude list).
     * Default: [] (include all)
     */
    abstract val include: ListProperty<String>

    /**
     * List of regex patterns for environment variables to exclude.
     * Takes precedence over the include list.
     * Default: [] (exclude none)
     */
    abstract val exclude: ListProperty<String>

    init {
        // Set defaults
        enabled.convention(true)
        files.convention(listOf(".env"))
        required.convention(false)
        systemPropertiesOverride.convention(true)
        include.convention(emptyList())
        exclude.convention(emptyList())
    }

    /**
     * Convenience method to add a file to the files list
     */
    fun file(filename: String) {
        files.add(filename)
    }

    /**
     * Convenience method to add multiple files
     */
    fun files(vararg filenames: String) {
        files.addAll(*filenames)
    }
}