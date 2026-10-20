package io.github.dsdolzhenko.dotenv

import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring the DotEnv settings plugin.
 *
 * Unlike the project plugin, values are loaded eagerly (on first access) so they can be
 * used for configuration-time concerns such as repository credentials, e.g.:
 *
 * ```
 * // settings.gradle.kts
 * plugins {
 *     id("io.github.dsdolzhenko.dotenv.settings") version "<version>"
 * }
 *
 * dotenv {
 *     files.set(listOf(".env", ".env.local"))
 * }
 * ```
 *
 * ```
 * // build.gradle.kts
 * val spaceUsername = dotenv.get("SPACE_USERNAME") ?: ""
 * ```
 *
 * Configure `files`/`required`/`systemPropertiesOverride` before the first call to [get],
 * since the values are loaded (and cached) lazily from whatever configuration is in place
 * at that point.
 */
abstract class DotEnvSettingsExtension @Inject constructor(
    objects: ObjectFactory,
    private val settings: Settings,
) {

    /**
     * List of .env files to load, relative to the settings directory.
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
     * Default: true
     */
    val systemPropertiesOverride: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

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

    private val values: Map<String, String> by lazy {
        EnvLoader.load(
            baseDir = settings.rootDir,
            files = files.get(),
            systemPropertiesOverride = systemPropertiesOverride.get(),
        ).also {
            if (it.isEmpty() && required.get()) {
                throw IllegalStateException("No .env file found and dotenv.required is true")
            }
        }
    }

    // Lets other settings plugins (e.g. one resolving secret-manager references such as
    // op://...) post-process every value looked up through this extension, so values stay
    // usable as plain dotenv.get(...)/dotenv["..."] calls regardless of what loaded them.
    private var valueResolver: (String) -> String = { it }

    /**
     * Registers a function that every value returned by [get] is passed through, so that
     * references held in the .env file (e.g. secret-manager references) can be resolved
     * transparently. Register this before any call to [get]; the last registration wins.
     */
    fun resolveValuesWith(resolver: (String) -> String) {
        valueResolver = resolver
    }

    /**
     * Looks up a value loaded from the configured .env files.
     * Also usable as `dotenv["KEY"]`.
     */
    operator fun get(key: String): String? = values[key]?.let(valueResolver)
}
