/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import io.ktor.plugins.registry.*
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ReleaseArtifactsTest {

    private val artifacts = ReleaseArtifactsMapping(
        mapOf(
            "2.3.11" to mapOf(
                "org.group:one:2.4.1" to "build/repository/group/one-2.4.1.jar",
                "org.group:two-jvm:3.2.1" to "build/repository/group/two-3.2.1.jar"
            )
        )
    )
    private val versionVariables = mapOf(
        "one" to "1.2",
        "two" to "3.+",
    )
    private val ktorRelease = KtorRelease("2.3.11")

    @Test
    fun `resolve actual version - range`() {
        "org.group:one:2.+".shouldResolveTo("org.group:one:2.4.1")
        "org.group:one:[2.0.0,3.0.0]".shouldResolveTo("org.group:one:2.4.1")
        "org.group:two:3.2.+".shouldResolveTo("org.group:two-jvm:3.2.1")
        "com.group:one:2.+".shouldNotResolve()
        "org.group:one1:2.+".shouldNotResolve()
        "org.group:one:2.5.+".shouldNotResolve()
    }

    @Test
    fun `resolve actual version - number`() {
        "org.group:one.1.2".shouldResolveTo("org.group:one.1.2")
    }

    @Test
    fun `resolve actual version - variable`() {
        "org.group:one:${'$'}one".shouldResolveTo("org.group:one:1.2")
        "org.group:two:${'$'}two".shouldResolveTo("org.group:two-jvm:3.2.1")
    }

    private fun String.shouldResolveTo(expected: String) {
        val artifactReference = ArtifactReference.parse(this, versionVariables = versionVariables)
        val actualVersion = artifacts[ktorRelease].resolveActualVersion(artifactReference)
        assertEquals(expected, actualVersion.toString())
    }

    private fun String.shouldNotResolve() {
        val artifactReference = ArtifactReference.parse(this, versionVariables = versionVariables)
        assertFailsWith<IllegalStateException>("") {
            artifacts[ktorRelease].resolveActualVersion(artifactReference)
        }
    }

}
