/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.file.Paths

fun main() {
    val pluginsRoot = Paths.get("plugins")
    val buildDir = Paths.get("build")
    with(RegistryBuilder()) {
        processAssets(pluginsRoot, buildDir)
        for (target in listOf("server", "client")) {
            buildRegistry(pluginsRoot, buildDir, target)
        }
    }
}
