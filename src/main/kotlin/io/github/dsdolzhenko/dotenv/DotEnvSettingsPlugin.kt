package io.github.dsdolzhenko.dotenv

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Gradle settings plugin that loads environment variables from .env files and exposes them
 * for configuration-time use (e.g. repository credentials), in `settings.gradle.kts` and in
 * every project's `build.gradle.kts` via the shared `dotenv` extension.
 */
class DotEnvSettingsPlugin : Plugin<Settings> {

    override fun apply(settings: Settings) {
        val extension = settings.extensions.create(
            "dotenv",
            DotEnvSettingsExtension::class.java,
            settings,
        )

        // Share the same extension instance with every project so build scripts can also
        // call dotenv.get(...)/dotenv["..."] for configuration-time values such as
        // repository credentials.
        settings.gradle.beforeProject { project ->
            project.extensions.add(DotEnvSettingsExtension::class.java, "dotenv", extension)
        }
    }
}
