/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.file.Paths

fun main() {
    val builder = RegistryBuilder()
    for (target in listOf("server", "client")) {
        builder.buildRegistry(
            pluginsRoot = Paths.get("plugins"),
            buildDir = Paths.get("build"),
            target = target
        )
    }
}
