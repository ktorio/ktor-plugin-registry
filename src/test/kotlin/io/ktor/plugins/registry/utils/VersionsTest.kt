/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import io.ktor.plugins.registry.*
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionsTest {

    @Test
    fun test() {
        assertEquals("${'$'}foo_bar_version", catalogVersion("foo-bar").normalizedName)
        assertEquals("${'$'}foo_bar_version", catalogVersion("fooBar").normalizedName)
    }

    private fun catalogVersion(name: String) =
        VersionVariable(name, VersionNumber("1.2.3"))

}
