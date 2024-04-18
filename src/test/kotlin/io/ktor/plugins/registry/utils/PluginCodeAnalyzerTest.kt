/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

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
            CodeInjectionSite.SOURCE_FILE_KT,
            KOTLIN_CODE.trimIndent(),
            "Test.kt"
        )

        assertEquals(InstallSnippet.RawContent(KOTLIN_CODE.trimIndent(), "Test.kt"), result)
    }

    @Test
    fun `read install snippet from kotlin`() {
        val result = pluginCodeAnalyzer.parseInstallSnippet(CodeInjectionSite.DEFAULT, KOTLIN_CODE.trimIndent())

        assertEquals(
            InstallSnippet.Kotlin(
            imports = listOf(
                "java.nio.file.Paths",
            ),
            code = """println(Paths.get("").toAbsolutePath().toString())"""
        ), result)
    }

    @Test
    fun `read install snippet code contents`() {
        val result = pluginCodeAnalyzer.parseInstallSnippet(
            CodeInjectionSite.OUTSIDE_APP,
            KOTLIN_CODE.trimIndent(),
            "Test.kt"
        )

        assertEquals(
            InstallSnippet.Kotlin(
            imports = listOf("java.nio.file.Paths"),
            code = """
                public fun pwd() {
                    println(Paths.get("").toAbsolutePath().toString())
                }
            """.trimIndent()
        ), result)
    }
}
