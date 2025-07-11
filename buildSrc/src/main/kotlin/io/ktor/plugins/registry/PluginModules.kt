/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.YamlMap
import java.nio.file.Path
import kotlin.collections.emptyList
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

private val moduleReferencePattern: Regex = Regex("\\s+module:\\s*(\\w+)")

fun Path.moduleReferences(): List<String> {
    val modulesFromVersionsFile = resolve(VERSIONS_FILE).readYamlMap()?.entries?.flatMap { (_, artifacts) ->
        when(artifacts) {
            // Dependency with alias is not a module, so we skip it, for example,
            // "[2.0,)":
            //    dependency: simple:dependency:0.0.1
            //    alias: very-simple:dependency
            is YamlMap -> if (artifacts.entries.keys.any { it.content == "alias" }) {
                emptyList()
            } else {
                artifacts.entries.map { (key) -> key.content }
            }
            else -> emptyList()
        }
    }.orEmpty()

    val modulesFromManifests = listDirectoryEntries().mapNotNull { entry ->
        entry.resolve(MANIFEST_FILE).takeIf { it.exists() }
    }.flatMap { manifest ->
        moduleReferencePattern.findAll(manifest.bufferedReader().readText()).map { match ->
            match.groupValues[1]
        }
    }

    return (modulesFromVersionsFile + modulesFromManifests).distinct()
}