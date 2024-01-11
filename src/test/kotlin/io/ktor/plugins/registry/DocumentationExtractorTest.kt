package io.ktor.plugins.registry

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentationExtractorTest {

    @Test
    fun `extracts documentation`() {
        val documentation = DocumentationExtractor.parseDocumentationMarkdown("""
            This plugin does some stuff.
            
            # Usage
            
            First, do the thing with the stuff:
            
            ```kotlin
            doCode()
            ```
        """.trimIndent())

        val (description, usage) = documentation

        assertEquals("""
            This plugin does some stuff.
        """.trimIndent(), description)

        assertEquals("""
            First, do the thing with the stuff:
            
            ```kotlin
            doCode()
            ```
        """.trimIndent(), usage)
    }

    @Test
    fun `usage is required`() {
        assertThrows<IllegalArgumentException> {
            DocumentationExtractor.parseDocumentationMarkdown("""
                Some documentation that forgot usage.
                
                ```kotlin
                doCode
                ```
            """.trimIndent())
        }
    }

}