/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class CLIUtilsTest {

    @Test
    fun testTree() {
        assertEquals(
            """
                csrf
                 ├── 2.0
                 │   ├── install.kt
                 │   └── manifest.ktor.yaml
                 └── versions.ktor.yaml
            """.trimIndent().trim(),
            CLIUtils.tree("src/test/resources/server/io.ktor/csrf").trim()
        )
    }

    @Test
    fun testAppendHorizontal() {
        val left = """
            [ LEFT ]
            [ SIDE ]
        """.trimIndent()

        val right = """
            [ RIGHT ]
            [ SIDE  ]
        """.trimIndent()

        assertEquals("""
            [ LEFT ]  [ RIGHT ]
            [ SIDE ]  [ SIDE  ]
        """.trimIndent(), CLIUtils.appendHorizontal(left, right, 10))
    }

    @Test
    fun printHeader() {
        println(CLIUtils.ktorScriptHeader())
    }

}
