/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(KtorReleasesTest::class.java)


    @Test
    fun `fetch latest 1 ktor versions`() {
        assertEquals(
            """
                2.3.7
                3.0.0-beta-1
            """.trimIndent(),
            fetchKtorVersionsFromMaven(1, logger, ktorVersionsDoc).joinToString("\n")
        )
    }

    @Test
    fun `fetch latest 2 ktor versions`() {
        assertEquals(
            """
                2.2.3
                2.2.4
                2.3.6
                2.3.7
                3.0.0-beta-1
            """.trimIndent(),
            fetchKtorVersionsFromMaven(2, logger, ktorVersionsDoc).joinToString("\n")
        )
    }

}