package io.ktor.plugins.registry

import kotlin.test.Test
import kotlin.test.assertEquals

class CodeAnalysisTest {

    companion object {
        private const val KOTLIN_CODE = """
            import java.nio.file.Paths
        
            public fun pwd() {
                println(Paths.get("").toAbsolutePath().toString())
            }
        """
    }

    private val codeAnalysis = CodeAnalysis()

    @Test
    fun `read install snippet verbatim`() {
        val result = codeAnalysis.parseInstallSnippet(CodeInjectionSite.SOURCE_FILE_KT, KOTLIN_CODE.trimIndent(), "Test.kt")

        assertEquals(InstallSnippet.RawContent(KOTLIN_CODE.trimIndent(), "Test.kt"), result)
    }

    @Test
    fun `read install snippet from kotlin`() {
        val result = codeAnalysis.parseInstallSnippet(CodeInjectionSite.DEFAULT, KOTLIN_CODE.trimIndent())

        assertEquals(InstallSnippet.Kotlin(
            imports = listOf(
                "java.nio.file.Paths",
            ),
            code = """println(Paths.get("").toAbsolutePath().toString())"""
        ), result)
    }

    @Test
    fun `read install snippet code contents`() {
        val result = codeAnalysis.parseInstallSnippet(CodeInjectionSite.OUTSIDE_APP, KOTLIN_CODE.trimIndent(), "Test.kt")

        assertEquals(InstallSnippet.Kotlin(
            imports = listOf("java.nio.file.Paths"),
            code = """
                public fun pwd() {
                    println(Paths.get("").toAbsolutePath().toString())
                }
            """.trimIndent()
        ), result)
    }

}