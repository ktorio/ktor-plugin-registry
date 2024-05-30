/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

/**
 * Builds registry for use in ktor project generator back end.
 */
@OptIn(ExperimentalPathApi::class)
fun main() {
    val pluginsRoot = Paths.get("plugins")
    val buildDir = Paths.get("build")
    with(RegistryBuilder()) {
        val assetsDir = buildDir.resolve("registry/assets").also {
            it.deleteRecursively()
        }
        for (target in listOf("server", "client"))
            buildRegistry(pluginsRoot, buildDir, assetsDir, target)
    }
}
