/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

private const val IMAGE_EXT = ".{svg,png,jpg,jpeg,gif}"

object Files {

    fun Path.listSubDirectories() = listDirectoryEntries().filter { it.isDirectory() }

    fun Path.listImages(prefix: String = "*") = listDirectoryEntries(prefix + IMAGE_EXT).filter { it.isRegularFile() }

    @OptIn(ExperimentalPathApi::class)
    fun Path.resolveAndClear(path: String) = resolve(path).also {
        it.deleteRecursively()
    }

    fun Path.ifExists(): Path? = takeIf { it.exists() }

}
