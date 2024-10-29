/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private const val KTOR_VIOLET = 56
private const val KTOR_MAGENTA = 200

object CLIUtils {

    private val argPattern = Regex("--(\\w+)=(\\S*)")
    private val ansiEscapeSequenceRegex = "\u001B\\[[;\\d]*m".toRegex()
    private val ktorLogo = buildString {
        appendLine(colored("  ▗", KTOR_VIOLET))
        appendLine(colored("▗▟█▙", KTOR_VIOLET))
        appendLine(colored(" ▜", KTOR_VIOLET) + "  " + colored("▙", KTOR_MAGENTA))
        appendLine(colored("  ▜█▛▘", KTOR_MAGENTA))
        appendLine(colored("   ▘", KTOR_MAGENTA))
    }.trim('\n')
    private const val HEADER_POS_LEFT = 8

    private val ktorText = """
          _  ___           
         | |/ / |_ ___ _ _ 
         | ' <|  _/ _ \ '_|
         |_|\_\\__\___/_|
    """.trimIndent()

    /**
     * Color some text using ANSI escape codes.
     */
    fun colored(text: String, colorCode: Int) =
        "\u001B[38;5;${colorCode}m$text\u001B[0m"

    /**
     * Because we keep things professional around here.
     */
    fun ktorScriptHeader() =
        appendHorizontal(ktorLogo, ktorText, HEADER_POS_LEFT)
            .prependIndent("    ") + '\n'

    fun appendHorizontal(left: String, right: String, leftPad: Int): String {
        val leftLines = left.lines()
        val rightLines = right.lines()
        check(leftLines.size >= rightLines.size) {
            "Left must have >= lines as right"
        }
        return leftLines.indices.joinToString("\n") { i ->
            val gap = generateSequence { " " }.take(leftPad - leftLines[i].visualLength()).joinToString("")
            leftLines[i] + gap + rightLines.getOrElse(i) { "" }
        }
    }

    /**
     * Length of a string, omitting ansi escape sequences.
     */
    fun String.visualLength() = replace(ansiEscapeSequenceRegex, "").length

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
