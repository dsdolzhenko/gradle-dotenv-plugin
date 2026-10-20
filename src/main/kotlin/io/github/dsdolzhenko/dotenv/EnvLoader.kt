package io.github.dsdolzhenko.dotenv

import io.github.cdimascio.dotenv.Dotenv
import java.io.File

/**
 * Parses `.env` files and applies the system-properties-override rule shared by
 * both the project plugin and the settings plugin.
 */
internal object EnvLoader {

    fun load(
        baseDir: File,
        files: List<String>,
        systemPropertiesOverride: Boolean,
    ): Map<String, String> {
        val envVars = mutableMapOf<String, String>()

        // Load files in order (later files override earlier ones)
        files.forEach { filename ->
            val file = File(baseDir, filename)
            if (file.exists()) {
                envVars.putAll(parseEnvFile(file))
            }
        }

        // System properties override .env values if configured.
        // Only consult properties for keys already loaded from .env files: reading the
        // full system properties table would make the configuration cache invalidate
        // whenever *any* unrelated system property changes.
        if (systemPropertiesOverride) {
            envVars.keys.toList().forEach { key ->
                System.getProperty(key)?.let { value ->
                    envVars[key] = value
                }
            }
        }

        return envVars
    }

    private fun parseEnvFile(file: File): Map<String, String> {
        val dotenv = Dotenv.configure()
            .directory(file.parent)
            .filename(file.name)
            .ignoreIfMissing()
            .ignoreIfMalformed()
            .load()

        return dotenv.entries()
            .associate { it.key to it.value }
    }
}
