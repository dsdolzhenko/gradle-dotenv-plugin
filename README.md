# Gradle DotEnv Plugin

A Gradle plugin that loads environment variables from `.env` files and injects them into Gradle tasks and their subprocesses. Perfect for local development with tools like Docker, databases, and external APIs.

## Features

- **Automatic subprocess injection** - Environment variables are available in `JavaExec`, `Exec`, and `Test` tasks  
- **Multiple file support** - Load from multiple `.env` files with override priority  
- **Multiline values** - Supports quoted multiline strings  
- **Comments and formatting** - Handles comments, empty lines, and various quote styles  
- **Variable filtering** - Include/exclude variables using regex patterns  
- **CI/CD friendly** - System properties can override `.env` values  
- **Type-safe configuration** - Kotlin DSL extension for easy configuration

## Installation

You will need a GitHub personal access token with `read:packages` scope to access the plugin's Maven repository.
See [Authenticating to GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages) for details.

Assuming that you have `githubUsername` and `githubToken` variables set in your `gradle.properties`, add the following to your `settings.gradle.kts`:

```kotlin
pluginManagement {
  val githubUsername: String? by settings
  val githubToken: String? by settings

  repositories {
    maven {
      url = uri("https://maven.pkg.github.com/dsdolzhenko/gradle-dotenv-plugin")
      credentials {
        username = githubUsername ?: System.getenv("GITHUB_USERNAME")
        password = githubToken ?: System.getenv("GITHUB_TOKEN")
      }
    }
    gradlePluginPortal()
  }
}
```

Then apply the plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.dsdolzhenko.dotenv") version "<version>"
}
```

## Usage

### Create a `.env` file in your project root

**.env:**
```bash
DATABASE_URL=postgresql://localhost:5432/mydb
API_KEY=sk_test_1234567890
DEBUG=true
PORT=3000
```

### Run your application

```bash
./gradlew run
```

Your application will now have access to all environment variables defined in `.env`!

```kotlin
fun main() {
    val dbUrl = System.getenv("DATABASE_URL")
    val apiKey = System.getenv("API_KEY")
    val debug = System.getenv("DEBUG")?.toBoolean() ?: false
    
    println("Database: $dbUrl")
    println("Debug mode: $debug")
}
```

## Configuration

The plugin can be configured using the `dotenv` extension:

```kotlin
dotenv {
    // Enable/disable the plugin (default: true)
    enabled.set(true)
    
    // Load multiple .env files (later files override earlier ones)
    files.set(listOf(".env", ".env.local"))
    
    // Require at least one .env file to exist (default: false)
    required.set(false)
    
    // Allow system properties to override .env values (default: true)
    systemPropertiesOverride.set(true)
    
    // Only include variables matching these regex patterns
    include.set(listOf("APP_.*", "DATABASE_.*"))
    
    // Exclude variables matching these regex patterns
    exclude.set(listOf(".*_SECRET", ".*_PASSWORD"))
}
```

## .env File Format

The plugin supports the standard `.env` file format:

```bash
# Comments are supported
VAR_NAME=value

# Quoted values (with escape sequences)
MESSAGE="Hello\nWorld"

# Single-quoted values (literal, no escaping)
PATH='/usr/local/bin'

# Multiline values
DESCRIPTION="This is a
multiline value
that spans multiple lines"

# Inline comments
DEBUG=true # This enables debug mode

# Empty values
EMPTY_VALUE=

# Values with special characters
SPECIAL="@#$%^&*()"

# Escape sequences in double quotes
ESCAPED="Line 1\nLine 2\tTabbed"
```

### Supported Features

- Comments (`#`)
- Empty lines
- Double-quoted values with escape sequences (`\n`, `\t`, `\"`)
- Single-quoted values (literal, no escaping)
- Unquoted values
- Inline comments
- Multiline values (in double quotes)
- Special characters

## How It Works

### Configuration Phase
- Plugin reads `.env` files during Gradle's configuration phase
- Parses and validates environment variables
- Applies filtering rules (include/exclude patterns)

### Task Configuration
- Plugin hooks into all `ProcessForkOptions` tasks:
    - `JavaExec` (e.g., `run` task)
    - `Exec` (shell commands)
    - `Test` (unit/integration tests)
- Injects environment variables into task configurations

### Subprocess Execution
- When tasks spawn subprocesses, they inherit the configured environment
- Variables are available via `System.getenv()` in Java/Kotlin
- Variables are available as `$VAR_NAME` in shell scripts

## Security Considerations

### Don't Commit Secrets

Add `.env.local` and environment-specific files to `.gitignore`:

**.gitignore:**
```
.env.local
.env.*.local
.env.development
.env.production
```

Commit `.env` with example values:

**.env:**
```bash
# Example configuration - copy to .env.local and customize
DATABASE_URL=postgresql://localhost:5432/mydb
API_KEY=your_api_key_here
```

### Use Filtering

Exclude sensitive variables from logs:

```kotlin
dotenv {
    exclude.set(listOf(
        ".*_SECRET",
        ".*_PASSWORD",
        ".*_KEY",
        ".*_TOKEN"
    ))
}
```

### Use gradle-secrets-plugin

Use [gradle-secrets-plugin](https://github.com/dsdolzhenko/gradle-secrets-plugin) to inject secrets from a secret store (such as 1Password) and avoid having sensitive values in `.env` files on disk.

### CI/CD Best Practices

- Use your CI/CD platform's secret management
- Override sensitive values with system properties
- Never print environment variables in logs

## Troubleshooting

### Variables not appearing in subprocess

1. **Check plugin is applied**: Ensure plugin is in `plugins` block
2. **Check file location**: `.env` must be in project root (or as configured)
3. **Check task type**: Plugin only works with `JavaExec`, `Exec`, and `Test` tasks
4. **Enable debug logging**: Run with `./gradlew run --debug`

### File parsing issues

1. **Check file encoding**: Use UTF-8 encoding
2. **Check quote matching**: Ensure quotes are properly closed
3. **Check for special characters**: Escape special characters in double quotes

### Variables being overridden

1. **Check file order**: Later files in `files` list override earlier ones
2. **Check system properties**: System properties override by default (unless disabled)
3. **Review .env.local**: This file typically overrides .env

## Advanced Usage

### Conditional Loading

```kotlin
dotenv {
    files.set(when (System.getenv("CI")) {
        "true" -> listOf(".env.ci")
        else -> listOf(".env", ".env.local")
    })
}
```

### Dynamic Configuration

```kotlin
dotenv {
    val envName = project.findProperty("env")?.toString() ?: "local"
    files.set(listOf(".env", ".env.$envName"))
}
```

Run with: `./gradlew run -Penv=production`

### Validation

```kotlin
tasks.named("run") {
    doFirst {
        val required = listOf("DATABASE_URL", "API_KEY")
        required.forEach { key ->
            requireNotNull(System.getenv(key)) {
                "Required environment variable not set: $key"
            }
        }
    }
}
```

## Development

### Building the plugin

```bash
cd plugin
./gradlew build
```

### Publishing locally

```bash
./gradlew publishToMavenLocal
```

### Testing

```bash
./gradlew test
```

## License

The project is licensed under the [MIT license](./LICENSE.txt).