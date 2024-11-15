/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import org.gradle.internal.impldep.junit.framework.TestCase.assertTrue
import kotlin.test.Test

class FilesTest {

    @Test
    fun listFolders() {
        val result = folders("../plugins/*/io.ktor/*/2.0").map { it.toString() }
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("../plugins/server/io.ktor/resources/2.0"))
    }

}