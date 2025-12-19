package io.github.dsdolzhenko.dotenv

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotEnvParserTest {

    private val parser = DotEnvParser.Companion.create()

    // ========== Basic Parsing Tests ==========

    @Test
    fun `parse simple key-value pairs`() {
        val lines = listOf("KEY1=value1", "KEY2=value2")
        val result = parser.parse(lines)

        assertEquals(2, result.size)
        assertEquals("value1", result["KEY1"])
        assertEquals("value2", result["KEY2"])
    }

    @Test
    fun `parse multiple key-value pairs with various formats`() {
        val lines = listOf(
            "SIMPLE=value",
            "WITH_UNDERSCORE=test_value",
            "NUMERIC=12345",
            "MIXED=abc123"
        )
        val result = parser.parse(lines)

        assertEquals(4, result.size)
        assertEquals("value", result["SIMPLE"])
        assertEquals("test_value", result["WITH_UNDERSCORE"])
        assertEquals("12345", result["NUMERIC"])
        assertEquals("abc123", result["MIXED"])
    }

    @Test
    fun `skip empty lines`() {
        val lines = listOf("KEY1=value1", "", "KEY2=value2", "   ", "KEY3=value3")
        val result = parser.parse(lines)

        assertEquals(3, result.size)
        assertEquals("value1", result["KEY1"])
        assertEquals("value2", result["KEY2"])
        assertEquals("value3", result["KEY3"])
    }

    @Test
    fun `trim whitespace around keys and values`() {
        val lines = listOf(
            "  KEY1  =  value1  ",
            "KEY2=  value2",
            "  KEY3=value3  "
        )
        val result = parser.parse(lines)

        assertEquals(3, result.size)
        assertEquals("value1", result["KEY1"])
        assertEquals("value2", result["KEY2"])
        assertEquals("value3", result["KEY3"])
    }

    @Test
    fun `handle empty value`() {
        val lines = listOf("EMPTY_VALUE=")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("", result["EMPTY_VALUE"])
    }

    // ========== Comment Tests ==========

    @Test
    fun `skip full-line comments`() {
        val lines = listOf(
            "# This is a comment",
            "KEY1=value1",
            "# Another comment",
            "KEY2=value2"
        )
        val result = parser.parse(lines)

        assertEquals(2, result.size)
        assertEquals("value1", result["KEY1"])
        assertEquals("value2", result["KEY2"])
    }

    @Test
    fun `handle inline comments in unquoted values`() {
        val lines = listOf("KEY=value # this is a comment")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value", result["KEY"])
    }

    @Test
    fun `hash symbol in double-quoted values is not a comment`() {
        val lines = listOf("KEY=\"value#with#hashes\"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value#with#hashes", result["KEY"])
    }

    @Test
    fun `hash symbol in single-quoted values is not a comment`() {
        val lines = listOf("KEY='value#with#hashes'")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value#with#hashes", result["KEY"])
    }

    @Test
    fun `empty comment line`() {
        val lines = listOf("#", "KEY=value")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value", result["KEY"])
    }

    // ========== Double-Quoted Value Tests ==========

    @Test
    fun `parse double-quoted values`() {
        val lines = listOf("KEY=\"value\"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value", result["KEY"])
    }

    @Test
    fun `parse empty double-quoted string`() {
        val lines = listOf("KEY=\"\"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("", result["KEY"])
    }

    @Test
    fun `process escape sequence newline`() {
        val lines = listOf("KEY=\"line1\\nline2\"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\nline2", result["KEY"])
    }

    @Test
    fun `process escape sequence tab`() {
        val lines = listOf("KEY=\"col1\\tcol2\"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("col1\tcol2", result["KEY"])
    }

    @Test
    fun `process escape sequence escaped quote`() {
        val lines = listOf("KEY=\"say \\\"hello\\\"\"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("say \"hello\"", result["KEY"])
    }

    @Test
    fun `process multiple escape sequences`() {
        val lines = listOf("KEY=\"line1\\nline2\\ttab\\\"quote\"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\nline2\ttab\"quote", result["KEY"])
    }

    @Test
    fun `single quotes inside double quotes are literal`() {
        val lines = listOf("KEY=\"it's working\"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("it's working", result["KEY"])
    }

    @Test
    fun `double-quoted value with spaces`() {
        val lines = listOf("KEY=\"  value with spaces  \"")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("  value with spaces  ", result["KEY"])
    }

    // ========== Single-Quoted Value Tests ==========

    @Test
    fun `parse single-quoted values`() {
        val lines = listOf("KEY='value'")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value", result["KEY"])
    }

    @Test
    fun `parse empty single-quoted string`() {
        val lines = listOf("KEY=''")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("", result["KEY"])
    }

    @Test
    fun `escape sequences NOT processed in single quotes`() {
        val lines = listOf("KEY='line1\\nline2'")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\\nline2", result["KEY"])
    }

    @Test
    fun `double quotes inside single quotes are literal`() {
        val lines = listOf("KEY='say \"hello\"'")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("say \"hello\"", result["KEY"])
    }

    @Test
    fun `single-quoted value with spaces`() {
        val lines = listOf("KEY='  value with spaces  '")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("  value with spaces  ", result["KEY"])
    }

    // ========== Unquoted Value Tests ==========

    @Test
    fun `parse simple unquoted value`() {
        val lines = listOf("KEY=unquoted_value")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("unquoted_value", result["KEY"])
    }

    @Test
    fun `unquoted value with inline comment`() {
        val lines = listOf("KEY=value # comment here")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value", result["KEY"])
    }

    @Test
    fun `unquoted numeric values`() {
        val lines = listOf(
            "PORT=8080",
            "TIMEOUT=300",
            "VERSION=1.2.3"
        )
        val result = parser.parse(lines)

        assertEquals(3, result.size)
        assertEquals("8080", result["PORT"])
        assertEquals("300", result["TIMEOUT"])
        assertEquals("1.2.3", result["VERSION"])
    }

    @Test
    fun `unquoted value with special characters`() {
        val lines = listOf(
            "URL=http://example.com",
            "PATH=/usr/local/bin",
            "EMAIL=test@example.com"
        )
        val result = parser.parse(lines)

        assertEquals(3, result.size)
        assertEquals("http://example.com", result["URL"])
        assertEquals("/usr/local/bin", result["PATH"])
        assertEquals("test@example.com", result["EMAIL"])
    }

    // ========== Multiline Value Tests ==========

    @Test
    fun `parse multiline value spanning two lines`() {
        val lines = listOf(
            "KEY=\"line1",
            "line2\""
        )
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\nline2", result["KEY"])
    }

    @Test
    fun `parse multiline value spanning three lines`() {
        val lines = listOf(
            "KEY=\"line1",
            "line2",
            "line3\""
        )
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\nline2\nline3", result["KEY"])
    }

    @Test
    fun `escape sequences in multiline values`() {
        val lines = listOf(
            "KEY=\"line1\\n",
            "line2\\t",
            "line3\""
        )
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\\n\nline2\\t\nline3", result["KEY"])
    }

    @Test
    fun `multiline with empty lines`() {
        val lines = listOf(
            "KEY=\"line1",
            "",
            "line3\""
        )
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\n\nline3", result["KEY"])
    }

    @Test
    fun `multiline with indentation`() {
        val lines = listOf(
            "KEY=\"line1",
            "  indented line",
            "line3\""
        )
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\nindented line\nline3", result["KEY"])
    }

    @Test
    fun `multiline with trailing whitespace on closing quote`() {
        val lines = listOf(
            "KEY=\"line1",
            "line2\"   "
        )
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("line1\nline2", result["KEY"])
    }

    @Test
    fun `multiple keys with multiline values`() {
        val lines = listOf(
            "KEY1=\"multiline1",
            "second line\"",
            "KEY2=single",
            "KEY3=\"multiline2",
            "another line\""
        )
        val result = parser.parse(lines)

        assertEquals(3, result.size)
        assertEquals("multiline1\nsecond line", result["KEY1"])
        assertEquals("single", result["KEY2"])
        assertEquals("multiline2\nanother line", result["KEY3"])
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `skip line without separator`() {
        val lines = listOf("KEY1=value1", "INVALID_LINE", "KEY2=value2")
        val result = parser.parse(lines)

        assertEquals(2, result.size)
        assertEquals("value1", result["KEY1"])
        assertEquals("value2", result["KEY2"])
    }

    @Test
    fun `handle multiple equals signs`() {
        val lines = listOf("KEY=value=with=equals")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value=with=equals", result["KEY"])
    }

    @Test
    fun `whitespace around equals sign`() {
        val lines = listOf(
            "KEY1 = value1",
            "KEY2= value2",
            "KEY3 =value3"
        )
        val result = parser.parse(lines)

        assertEquals(3, result.size)
        assertEquals("value1", result["KEY1"])
        assertEquals("value2", result["KEY2"])
        assertEquals("value3", result["KEY3"])
    }

    @Test
    fun `unicode characters`() {
        val lines = listOf(
            "MESSAGE=\"Hello 世界\"",
            "EMOJI=\"🎉✨\""
        )
        val result = parser.parse(lines)

        assertEquals(2, result.size)
        assertEquals("Hello 世界", result["MESSAGE"])
        assertEquals("🎉✨", result["EMOJI"])
    }

    @Test
    fun `special characters in values`() {
        val lines = listOf(
            "SYMBOLS=\"@$%^&*\"",
            "BRACKETS=\"[]{}()\"",
            "PUNCTUATION=\"!?,.:;\""
        )
        val result = parser.parse(lines)

        assertEquals(3, result.size)
        assertEquals("@$%^&*", result["SYMBOLS"])
        assertEquals("[]{}()", result["BRACKETS"])
        assertEquals("!?,.:;", result["PUNCTUATION"])
    }

    @Test
    fun `parse empty file`() {
        val lines = emptyList<String>()
        val result = parser.parse(lines)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `file with only comments`() {
        val lines = listOf("# Comment 1", "# Comment 2", "# Comment 3")
        val result = parser.parse(lines)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `file with only empty lines`() {
        val lines = listOf("", "   ", "\t")
        val result = parser.parse(lines)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `very long value`() {
        val longValue = "a".repeat(10000)
        val lines = listOf("KEY=$longValue")
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals(longValue, result["KEY"])
    }

    @Test
    fun `many lines stress test`() {
        val lines = (1..1000).map { "KEY$it=value$it" }
        val result = parser.parse(lines)

        assertEquals(1000, result.size)
        assertEquals("value1", result["KEY1"])
        assertEquals("value500", result["KEY500"])
        assertEquals("value1000", result["KEY1000"])
    }

    // ========== Error Handling Tests (Strict Mode) ==========

    @Test
    fun `strict mode throws on unclosed multiline`() {
        val lines = listOf("KEY=\"unclosed")

        val exception = assertThrows<ParsingException> {
            parser.parse(lines, strict = true)
        }

        assertTrue(exception.message!!.contains("Unclosed multiline"))
        assertEquals(1, exception.lineNumber)
    }

    @Test
    fun `strict mode throws on line without separator`() {
        val lines = listOf("INVALID_LINE")

        val exception = assertThrows<ParsingException> {
            parser.parse(lines, strict = true)
        }

        assertTrue(exception.message!!.contains("does not contain '=' separator"))
        assertEquals(1, exception.lineNumber)
    }

    @Test
    fun `strict mode throws on empty key`() {
        val lines = listOf("=value")

        val exception = assertThrows<ParsingException> {
            parser.parse(lines, strict = true)
        }

        assertTrue(exception.message!!.contains("Empty key"))
        assertEquals(1, exception.lineNumber)
    }

    @Test
    fun `non-strict mode skips invalid lines`() {
        val lines = listOf(
            "KEY1=value1",
            "INVALID_LINE",
            "=value",
            "KEY2=value2"
        )
        val result = parser.parse(lines, strict = false)

        assertEquals(2, result.size)
        assertEquals("value1", result["KEY1"])
        assertEquals("value2", result["KEY2"])
    }

    @Test
    fun `strict mode reports correct line number`() {
        val lines = listOf(
            "KEY1=value1",
            "KEY2=value2",
            "INVALID_LINE",
            "KEY3=value3"
        )

        val exception = assertThrows<ParsingException> {
            parser.parse(lines, strict = true)
        }

        assertEquals(3, exception.lineNumber)
    }

    // ========== Integration Tests ==========

    @Test
    fun `parse real-world dotenv file`() {
        val lines = listOf(
            "# Application Configuration",
            "",
            "# Database",
            "DB_HOST=localhost",
            "DB_PORT=5432",
            "DB_NAME=myapp",
            "DB_USER=admin",
            "DB_PASSWORD=\"p@ssw0rd#123\"",
            "",
            "# API Keys",
            "API_KEY=\"sk-1234567890abcdef\" # production key",
            "SECRET_KEY='my-secret-key-!@#$'",
            "",
            "# Feature Flags",
            "ENABLE_FEATURE_X=true",
            "ENABLE_FEATURE_Y=false",
            "",
            "# Multiline Certificate",
            "CERTIFICATE=\"-----BEGIN CERTIFICATE-----",
            "MIIBkTCB+wIJAKHHCgVZU7cvMA0GCSqGSIb3DQEBCwUAMBMxETAPBgNVBAMMCHRl",
            "c3QuY29tMB4XDTE3MDEwMTAwMDAwMFoXDTE4MDEwMTAwMDAwMFowEzERMA8GA1UE",
            "-----END CERTIFICATE-----\""
        )
        val result = parser.parse(lines)

        assertEquals(10, result.size)
        assertEquals("localhost", result["DB_HOST"])
        assertEquals("5432", result["DB_PORT"])
        assertEquals("p@ssw0rd#123", result["DB_PASSWORD"])
        assertEquals("sk-1234567890abcdef", result["API_KEY"])
        assertEquals("my-secret-key-!@#$", result["SECRET_KEY"])
        assertEquals("true", result["ENABLE_FEATURE_X"])
        assertTrue(result["CERTIFICATE"]!!.contains("BEGIN CERTIFICATE"))
    }

    @Test
    fun `all features combined`() {
        val lines = listOf(
            "UNQUOTED=value",
            "DOUBLE_QUOTED=\"value\"",
            "SINGLE_QUOTED='value'",
            "WITH_ESCAPE=\"line1\\nline2\"",
            "INLINE_COMMENT=value # comment",
            "MULTILINE=\"line1",
            "line2\"",
            "# Comment",
            "",
            "EMPTY=",
            "UNICODE=\"世界\"",
            "SPECIAL=\"@#$%\""
        )
        val result = parser.parse(lines)

        assertEquals(9, result.size)
        assertEquals("value", result["UNQUOTED"])
        assertEquals("value", result["DOUBLE_QUOTED"])
        assertEquals("value", result["SINGLE_QUOTED"])
        assertEquals("line1\nline2", result["WITH_ESCAPE"])
        assertEquals("value", result["INLINE_COMMENT"])
        assertEquals("line1\nline2", result["MULTILINE"])
        assertEquals("", result["EMPTY"])
        assertEquals("世界", result["UNICODE"])
        assertEquals("@#$%", result["SPECIAL"])
    }

    // ========== File-Based Tests ==========

    @Test
    fun `parseFile reads from file correctly`() {
        val tempFile = File.createTempFile("test", ".env")
        try {
            tempFile.writeText("KEY1=value1\nKEY2=value2")
            val result = parser.parseFile(tempFile)

            assertEquals(2, result.size)
            assertEquals("value1", result["KEY1"])
            assertEquals("value2", result["KEY2"])
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `parseFile handles empty file`() {
        val tempFile = File.createTempFile("test", ".env")
        try {
            tempFile.writeText("")
            val result = parser.parseFile(tempFile)

            assertTrue(result.isEmpty())
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `parseFile with multiline values`() {
        val tempFile = File.createTempFile("test", ".env")
        try {
            tempFile.writeText("KEY=\"line1\nline2\nline3\"")
            val result = parser.parseFile(tempFile)

            assertEquals(1, result.size)
            assertEquals("line1\nline2\nline3", result["KEY"])
        } finally {
            tempFile.delete()
        }
    }

    // ========== Later Override Tests ==========

    @Test
    fun `later values override earlier values`() {
        val lines = listOf(
            "KEY=value1",
            "KEY=value2",
            "KEY=value3"
        )
        val result = parser.parse(lines)

        assertEquals(1, result.size)
        assertEquals("value3", result["KEY"])
    }

    @Test
    fun `case sensitivity in keys`() {
        val lines = listOf(
            "key=lowercase",
            "KEY=uppercase",
            "Key=mixedcase"
        )
        val result = parser.parse(lines)

        assertEquals(3, result.size)
        assertEquals("lowercase", result["key"])
        assertEquals("uppercase", result["KEY"])
        assertEquals("mixedcase", result["Key"])
    }
}