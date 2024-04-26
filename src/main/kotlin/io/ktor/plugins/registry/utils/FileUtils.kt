/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

private const val IMAGE_EXT = ".{svg,png,jpg,jpeg,gif}"

object FileUtils {

    fun Path.listSubDirectories() = listDirectoryEntries().filter { it.isDirectory() }

    fun Path.listImages(prefix: String = "*") = listDirectoryEntries(prefix + IMAGE_EXT).filter { it.isRegularFile() }

}
