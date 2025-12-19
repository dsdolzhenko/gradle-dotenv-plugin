package io.github.dsdolzhenko.dotenv

import java.io.File

/**
 * Parser for .env files.
 *
 * Supports:
 * - Comments (lines starting with #) and empty lines
 * - Double-quoted values with escape sequences (\n, \t, \")
 * - Single-quoted literal values (no escaping)
 * - Unquoted values with inline comments
 * - Multiline values in double quotes
 */
internal class DotEnvParser {

    /**
     * Parses a .env file from the given file.
     *
     * @param file The .env file to parse
     * @param strict If true, throws ParsingException on invalid format
     * @return Map of parsed key-value pairs
     */
    fun parseFile(file: File, strict: Boolean = false): Map<String, String> {
        return parse(file.readLines(), strict)
    }

    /**
     * Parses .env content from a list of lines.
     *
     * Handles:
     * - Comments (#) and empty lines
     * - Double-quoted values with escape sequences (\n, \t, \")
     * - Single-quoted literal values (no escaping)
     * - Unquoted values with inline comments
     * - Multiline values in double quotes
     *
     * @param lines The lines from a .env file
     * @param strict If true, throws ParsingException on invalid format
     * @return Map of parsed key-value pairs
     */
    fun parse(lines: List<String>, strict: Boolean = false): Map<String, String> {
        val state = ParsingState()

        lines.forEachIndexed { index, line ->
            state.lineNumber = index + 1
            parseLine(line.trim(), state, strict)
        }

        // Check for unclosed multiline at end of file
        if (state.inMultiline && strict) {
            throw ParsingException(
                "Unclosed multiline value for key '${state.currentKey}'",
                state.lineNumber
            )
        }

        return state.result
    }

    private fun parseLine(trimmedLine: String, state: ParsingState, strict: Boolean) {
        // Handle multiline values
        if (state.inMultiline) {
            parseMultilineContinuation(trimmedLine, state)
            return
        }

        // Skip empty lines and comments
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
            return
        }

        // Parse key=value
        val separatorIndex = trimmedLine.indexOf('=')
        if (separatorIndex == -1) {
            if (strict) {
                throw ParsingException(
                    "Line does not contain '=' separator: '$trimmedLine'",
                    state.lineNumber
                )
            }
            return
        }

        val key = trimmedLine.substring(0, separatorIndex).trim()
        val rawValue = trimmedLine.substring(separatorIndex + 1).trim()

        if (key.isEmpty()) {
            if (strict) {
                throw ParsingException(
                    "Empty key in line: '$trimmedLine'",
                    state.lineNumber
                )
            }
            return
        }

        // Parse the value
        val result = parseValue(rawValue)
        when (result) {
            is ValueParseResult.Complete -> {
                state.result[key] = result.value
            }
            is ValueParseResult.MultilineStart -> {
                state.currentKey = key
                state.currentValue = StringBuilder(result.initialContent)
                state.inMultiline = true
            }
        }
    }

    private fun parseMultilineContinuation(line: String, state: ParsingState) {
        if (line.endsWith("\"")) {
            // End of multiline value
            state.currentValue.append("\n").append(line.dropLast(1))
            state.result[state.currentKey!!] = state.currentValue.toString()
            state.currentKey = null
            state.currentValue = StringBuilder()
            state.inMultiline = false
        } else {
            // Continue accumulating
            state.currentValue.append("\n").append(line)
        }
    }

    private fun parseValue(rawValue: String): ValueParseResult {
        return when {
            // Double-quoted value
            rawValue.startsWith("\"") -> {
                // Find the closing quote
                val closingQuoteIndex = findClosingQuote(rawValue, 1)
                if (closingQuoteIndex != -1) {
                    // Complete quoted value on one line
                    val content = rawValue.substring(1, closingQuoteIndex)
                    ValueParseResult.Complete(processEscapeSequences(content))
                } else {
                    // Start of multiline value
                    ValueParseResult.MultilineStart(rawValue.substring(1))
                }
            }
            // Single-quoted value (literal, no escaping)
            rawValue.startsWith("'") -> {
                val closingQuoteIndex = rawValue.indexOf('\'', 1)
                if (closingQuoteIndex != -1) {
                    ValueParseResult.Complete(rawValue.substring(1, closingQuoteIndex))
                } else {
                    // Treat as unquoted if no closing quote
                    ValueParseResult.Complete(rawValue.split("#")[0].trim())
                }
            }
            // Unquoted value (trim inline comments)
            else -> {
                ValueParseResult.Complete(rawValue.split("#")[0].trim())
            }
        }
    }

    private fun findClosingQuote(value: String, startIndex: Int): Int {
        var i = startIndex
        while (i < value.length) {
            val char = value[i]
            if (char == '\"') {
                // Check if it's escaped
                if (i > 0 && value[i - 1] == '\\') {
                    // Check if the backslash itself is escaped
                    var backslashCount = 0
                    var j = i - 1
                    while (j >= 0 && value[j] == '\\') {
                        backslashCount++
                        j--
                    }
                    // If odd number of backslashes, the quote is escaped
                    if (backslashCount % 2 == 1) {
                        i++
                        continue
                    }
                }
                return i
            }
            i++
        }
        return -1
    }

    private fun processEscapeSequences(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
    }

    companion object {
        fun create(): DotEnvParser = DotEnvParser()
    }
}

/**
 * Internal state for parsing a .env file.
 */
internal class ParsingState {
    var currentKey: String? = null
    var currentValue: StringBuilder = StringBuilder()
    var inMultiline: Boolean = false
    val result: MutableMap<String, String> = mutableMapOf()
    var lineNumber: Int = 0
}

/**
 * Result of parsing a value from a .env line.
 */
internal sealed class ValueParseResult {
    /**
     * A complete value (single-line).
     */
    data class Complete(val value: String) : ValueParseResult()

    /**
     * The start of a multiline value.
     */
    data class MultilineStart(val initialContent: String) : ValueParseResult()
}

/**
 * Exception thrown when parsing fails in strict mode.
 */
internal class ParsingException(
    message: String,
    val lineNumber: Int,
    cause: Throwable? = null
) : RuntimeException("Line $lineNumber: $message", cause)
