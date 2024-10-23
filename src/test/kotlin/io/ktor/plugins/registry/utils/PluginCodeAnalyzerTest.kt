/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import io.ktor.util.reflect.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginCodeAnalyzerTest {

    companion object {
        private const val KOTLIN_CODE = """
            import java.nio.file.Paths
        
            public fun pwd() {
                println(Paths.get("").toAbsolutePath().toString())
            }
        """
    }

    private val pluginCodeAnalyzer = PluginCodeAnalyzer()

    @Test
    fun `read install snippet verbatim`() {
        val result = pluginCodeAnalyzer.parseInstallSnippet(
            KOTLIN_CODE.trimIndent(),
            CodeInjectionSite.SOURCE_FILE_KT.asMeta(file = "Test.kt"),
        )

        assertEquals(KOTLIN_CODE.trimIndent(), result.code)
        assertEquals(CodeInjectionSite.SOURCE_FILE_KT, result.site)
        assertEquals(emptyList(), result.importsOrEmpty)
        assertEquals("Test.kt", result.file)
    }

    @Test
    fun `read install snippet from kotlin`() {
        val result = pluginCodeAnalyzer.parseInstallSnippet(
            KOTLIN_CODE.trimIndent(),
            CodeInjectionSite.DEFAULT.asMeta()
        )

        assertEquals("""println(Paths.get("").toAbsolutePath().toString())""", result.code)
        assertEquals(CodeInjectionSite.DEFAULT, result.site)
        assertEquals(listOf("java.nio.file.Paths"), result.importsOrEmpty)
    }

    @Test
    fun `read install snippet code contents`() {
        val result = pluginCodeAnalyzer.parseInstallSnippet(
            KOTLIN_CODE.trimIndent(),
            CodeInjectionSite.OUTSIDE_APP.asMeta(file = "Test.kt"),
        )

        assertEquals("""
            public fun pwd() {
                println(Paths.get("").toAbsolutePath().toString())
            }
        """.trimIndent(), result.code)
        assertEquals(CodeInjectionSite.OUTSIDE_APP, result.site)
        assertEquals(listOf("java.nio.file.Paths"), result.importsOrEmpty)
    }
}
