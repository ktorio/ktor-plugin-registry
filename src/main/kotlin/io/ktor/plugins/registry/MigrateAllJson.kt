/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrNull

private val pluginsDir = Paths.get("plugins")

/**
 * Migrates all JSON plugin files >= 2.0 version.
 */
fun main() {
    for (targetDir in Files.list(pluginsDir)) {
        if (!targetDir.isDirectory())
            continue
        for (groupDir in Files.list(targetDir)) {
            if (!groupDir.isDirectory())
                continue
            for (pluginsDir in Files.list(groupDir)) {
                if (!pluginsDir.isDirectory())
                    continue
                val latestVersionDir = Files.list(pluginsDir).filter { it.name.startsWith("2") }.findFirst().getOrNull()
                if (latestVersionDir == null) {
                    println("No 2.0 version for plugin ${pluginsDir.name}")
                    continue
                }
                val jsonFile = latestVersionDir.resolve("manifest.json")
                if (!jsonFile.exists())
                    continue

                migratePluginJson(jsonFile)
                jsonFile.deleteIfExists()
            }
        }
    }
}
