/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

// Get all directories using patterns like "plugins/client/*/*"
fun folders(pattern: String): List<Path> {
    val segments = pattern.split("/").iterator()
    if (!segments.hasNext())
        return emptyList()

    var folders = listOf<Path>(Paths.get(segments.next().ifEmpty { "/" }))
    while(segments.hasNext()) {
        val next = segments.next()
        folders = when(next) {
            "*" -> folders.flatMap { folder ->
                when {
                    folder.isDirectory() -> folder.listDirectoryEntries()
                    else -> emptyList()
                }
            }
            else -> folders.mapNotNull { folder -> folder.resolve(Paths.get(next)).takeIf { it.exists() } }
        }
    }
    return folders.filter { it.isDirectory() }
}