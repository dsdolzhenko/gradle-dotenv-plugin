package io.github.dsdolzhenko.dotenv

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

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
abstract class DotEnvExtension @Inject constructor(objects: ObjectFactory)  {

    /**
     * Whether the plugin is enabled. Default: true
     */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    /**
     * List of .env files to load, relative to the project root.
     * Files are loaded in order, later files override earlier ones.
     * Default: [".env"]
     */
    val files: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(listOf(".env"))

    /**
     * Whether to throw an error if no .env file is found.
     * Default: false
     */
    val required: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /**
     * Whether system properties should override .env values.
     * Useful for CI/CD environments.
     * Default: true
     */
    val systemPropertiesOverride: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    /**
     * List of regex patterns for environment variables to include.
     * If empty, all variables are included (subject to exclude list).
     * Default: [] (include all)
     */
    val include: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

    /**
     * List of regex patterns for environment variables to exclude.
     * Takes precedence over the include list.
     * Default: [] (exclude none)
     */
    val exclude: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

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