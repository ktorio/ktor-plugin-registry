/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorReleasesTest {

    private val ktorVersionsDoc by lazy {
        javaClass.classLoader.getResourceAsStream("releases/maven-metadata.xml").use { input ->
            val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            db.parse(input)
        }
    }


    @Test
    fun `fetch latest 1 ktor versions`() {
        assertEquals(
            """
                1.6.8
                2.3.7
                3.0.0-beta-1
            """.trimIndent(),
            fetchKtorVersionsFromMaven(1, ktorVersionsDoc).joinToString("\n")
        )
    }

    @Test
    fun `fetch latest 2 ktor versions`() {
        assertEquals(
            """
                1.5.3
                1.5.4
                1.6.7
                1.6.8
                2.2.3
                2.2.4
                2.3.6
                2.3.7
                3.0.0-beta-1
            """.trimIndent(),
            fetchKtorVersionsFromMaven(2, ktorVersionsDoc).joinToString("\n")
        )
    }

}