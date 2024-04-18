/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private const val KTOR_ORANGE = 208
private const val KTOR_BLUE = 105

object CLIUtils {

    private val argPattern = Regex("--(\\w+)=(\\S*)")

    /**
     * Color some text using ANSI escape codes.
     */
    fun colored(text: String, colorCode: Int) =
        "\u001B[38;5;${colorCode}m$text\u001B[0m"

    /**
     * Because we keep things professional around here.
     */
    fun ktorLogo() = buildString {
        appendLine(colored("  ▗", KTOR_ORANGE))
        appendLine(colored("▗▟█▙", KTOR_ORANGE))
        appendLine(colored(" ▜", KTOR_ORANGE) + "  " + colored("▙", KTOR_BLUE))
        appendLine(colored("  ▜█▛▘", KTOR_BLUE))
        appendLine(colored("   ▘", KTOR_BLUE))
    }

    /**
     * Builds a map from command line args from formatted args like "--arg=value"
     *
     * @param defaults any default values that can be assumed
     * @param args args supplied from command line
     */
    fun argsToMap(defaults: Map<String, String>, args: Array<String>): Map<String, String> = buildMap {
        putAll(defaults)
        for (arg in args) {
            val (key, value) = argPattern.matchEntire(arg)?.destructured ?: continue
            put(key, value)
        }
    }

    /**
     * Formats directory structure like:
     *
     * plugin-dir
     *  ├── 2.0
     *  │   ├── manifest.ktor.yaml
     *  │   └── install.kt
     *  └── versions.ktor.yaml
     */
    fun tree(dir: String): String = tree(Paths.get(dir))

    fun tree(dir: Path): String = "${dir.name}\n${tree(dir, " ")}"

    private fun tree(dir: Path, prefix: String): String = buildString {
        val children = dir.listDirectoryEntries().sortedBy { it.name }
        for (i in children.indices) {
            val child = children[i]
            val isLast = (i == children.size - 1)
            val nextPrefix = if (isLast) "$prefix    " else "$prefix│   "
            appendLine("$prefix${if (isLast) "└── " else "├── "}${child.name}")
            if (child.isDirectory()) {
                append(tree(child, nextPrefix))
            }
        }
    }

}
