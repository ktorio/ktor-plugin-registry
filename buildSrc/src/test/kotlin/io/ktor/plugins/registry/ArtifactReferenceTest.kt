/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import kotlin.test.*

class ArtifactReferenceTest {

    @Test
    fun parsesReferences() {
        val input = "org.jetbrains:kotlinx-html:1.7.3"
        val reference = ArtifactReference.parse(input)
        assertEquals(input, reference.toString())
        assertEquals("org.jetbrains", reference.group)
        assertEquals("kotlinx-html", reference.name)
        assertEquals("1.7.3", reference.version.toString())
    }

    @Test
    fun parsesFunctionReferences() {
        val input = "npm(htmx.org:2.0.3)"
        val reference = ArtifactReference.parse(input)
        assertEquals(input, reference.toString())
        assertEquals(null, reference.group)
        assertEquals("htmx.org", reference.name)
        assertEquals("2.0.3", reference.version.toString())
    }

}