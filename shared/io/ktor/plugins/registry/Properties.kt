/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.file.Path
import java.util.Properties
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.io.path.inputStream

fun Path.readVersionProperties() = resolve("gradle.properties").let { versionsFile ->
    try {
        Properties().apply {
            load(versionsFile.inputStream())
        }.entries.associate { (key, value) ->
            key.toString() to value.toString()
        }
    } catch (_: Exception) {
        emptyMap()
    }
}