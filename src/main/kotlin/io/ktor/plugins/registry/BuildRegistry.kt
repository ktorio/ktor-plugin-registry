/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import io.ktor.plugins.registry.utils.Files.resolveAndClear
import io.ktor.plugins.registry.RegistryBuilder
import java.nio.file.Paths

/**
 * Builds registry for use in ktor project generator back end.
 */
fun main() {
    val pluginsRoot = Paths.get("plugins")
    val buildDir = Paths.get("build")
    with(RegistryBuilder()) {
        val assetsDir = buildDir.resolveAndClear("registry/assets")
        buildRegistries(pluginsRoot, buildDir, assetsDir)
    }
}
