package io.ktor.plugins.registry

import kotlin.test.Test
import kotlin.test.assertEquals

class CodeAnalysisTest {

    companion object {
        private const val KOTLIN_CODE = """
            import io.ktor.server.application.*
            import io.ktor.server.plugins.csrf.*
        
            public fun Application.install() {
                install(CSRF) {
                    // tests Origin is an expected value
                    allowOrigin("http://localhost:8080")
        
                    // tests Origin matches Host header
                    originMatchesHost()
        
                    // custom header checks
                    checkHeader("X-CSRF-Token")
                }
            }
        """
    }

    private val snippetExtractor = CodeSnippetExtractor()

    @Test
    fun `read install snippet verbatim`() {
        val result = snippetExtractor.parseInstallSnippet(CodeInjectionSite.SOURCE_FILE_KT, KOTLIN_CODE.trimIndent(), "Test.kt")

        assertEquals(InstallSnippet.RawContent(KOTLIN_CODE.trimIndent(), "Test.kt"), result)
    }

    @Test
    fun `read install snippet from kotlin`() {
        val result = snippetExtractor.parseInstallSnippet(CodeInjectionSite.DEFAULT, KOTLIN_CODE.trimIndent())

        assertEquals(InstallSnippet.Kotlin(
            imports = listOf(
                "import io.ktor.server.application.*",
                "import io.ktor.server.plugins.csrf.*",
            ),
            code = """
                install(CSRF) {
                    // tests Origin is an expected value
                    allowOrigin("http://localhost:8080")

                    // tests Origin matches Host header
                    originMatchesHost()

                    // custom header checks
                    checkHeader("X-CSRF-Token")
                }
            """.trimIndent()
        ), result)
    }

    @Test
    fun `read install snippet code contents`() {
        val result = snippetExtractor.parseInstallSnippet(CodeInjectionSite.OUTSIDE_APP, KOTLIN_CODE.trimIndent(), "Test.kt")

        assertEquals(InstallSnippet.Kotlin(
            imports = listOf(
                "import io.ktor.server.application.*",
                "import io.ktor.server.plugins.csrf.*",
            ),
            code = """
                public fun Application.install() {
                    install(CSRF) {
                        // tests Origin is an expected value
                        allowOrigin("http://localhost:8080")
    
                        // tests Origin matches Host header
                        originMatchesHost()
    
                        // custom header checks
                        checkHeader("X-CSRF-Token")
                    }
                }
            """.trimIndent()
        ), result)
    }

}