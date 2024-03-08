/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import kotlinx.serialization.json.*
import java.io.BufferedWriter
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.bufferedWriter
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * A script for migrating from the export format JSON to the more readable yaml + external files.
 */
fun main(args: Array<String>) {
    require(args.isNotEmpty()) {
        "Please supply a path to the JSON file as an argument"
    }
    val jsonFile = Paths.get(args[0])
    require(jsonFile.exists()) {
        "Could not find JSON file listed under path ${args[0]}"
    }

    val yamlFile = jsonFile.parent.resolve("manifest.ktor.yaml")
    yamlFile.bufferedWriter().use { yamlWriter ->
        yamlWriter.writeYamlFile(
            outputDir = jsonFile.parent,
            rootNode = Json.parseToJsonElement(jsonFile.readText()).jsonObject
        )
    }
}

fun BufferedWriter.writeYamlFile(outputDir: Path, rootNode: JsonObject) {
    for ((fieldName, node) in rootNode.entries) {
        when(fieldName) {
            "id",
            "version",
            "vendor",
            "dependencies",
            "ktor_version" -> println("Skipped field $fieldName")
            "name" -> appendLine("name: ${node.stringValue}")
            "short_description" -> appendLine("description: ${node.stringValue}")
            "github" -> appendLine("vcsLink: ${node.stringValue}")
            "copyright" -> appendLine("copyright: ${node.stringValue}")
            "group" -> appendLine("category: ${node.stringValue}")
            "required_feature_ids" -> {
                val arrayOutput = node.jsonArray.map { it.stringValue }.joinToString("") { "  - $it" }
                appendLine("prerequisites:\n$arrayOutput")
            }
            "documentation" -> writeDocumentationFile(node, outputDir)
            "install_recipe" -> writeInstallRecipeFiles(node, outputDir)
            else -> System.err.println("Unrecognized JSON field $fieldName; skipping")
        }
    }
}

private val JsonElement.stringValue get() = jsonPrimitive.content

private fun writeDocumentationFile(node: JsonElement, outputDir: Path) {
    val documentationObject = node.jsonObject
    val (description, usage, options) = listOf(
        "description",
        "usage",
        "options"
    ).map { documentationObject[it]?.stringValue }

    outputDir.resolve("documentation.md").bufferedWriter().use { out ->
        out.appendLine().appendLine(description).appendLine()
        usage?.takeIf { it.isNotBlank() }.let { out.appendLine("## Usage\n").appendLine(usage) }
        options?.takeIf { it.isNotBlank() }.let { out.appendLine("## Options\n").appendLine(options) }
    }
}

private fun BufferedWriter.writeInstallRecipeFiles(node: JsonElement, outputDir: Path) {
    val installRecipeObject = node.jsonObject
    fun JsonElement?.formatImports() = this?.jsonArray.orEmpty().joinToString("\n") {
        "import ${it.stringValue}"
    }
    val (imports, installBlock, templates, testImports) = listOf(
        "imports",
        "install_block",
        "templates",
        "test_imports",
    ).map { installRecipeObject[it] }

    if (installBlock == null && templates?.jsonArray.orEmpty().isEmpty())
        return

    appendLine("installation:")

    installBlock?.let {
        appendLine("  default: install.kt")
        writeCodeBlock(
            dir = outputDir,
            CodeInjectionSite.DEFAULT,
            installBlock.stringValue,
            imports = imports.formatImports(),
        )
    }

    val installTemplates = templates?.jsonArray.orEmpty().asSequence().map { json ->
        listOf("position", "text", "name").map { json.jsonObject[it]?.stringValue }
    }.filter {
        it.size >= 2
    }

    for ((position, text, name) in installTemplates) {
        check(position != null && text != null)
        val site = CodeInjectionSite.valueOf(position.uppercase())
        val templateImports = if (position == "test_function") testImports else imports
        val fileName = name ?: site.defaultFileLocation
        check(fileName != null) {
            "Missing filename for template $position"
        }
        appendLine("  ${site.lowercaseName}: $fileName")
        writeCodeBlock(
            dir = outputDir,
            site = site,
            codeBlock = text,
            imports = templateImports.formatImports(),
            fileName = name
        )
    }
}

private fun writeCodeBlock(
    dir: Path,
    site: CodeInjectionSite,
    codeBlock: String,
    imports: String?,
    fileName: String? = null,
) {
    fun writeInstallBody(startBlockText: String) {
        dir.resolve(site.defaultFileLocation!!).bufferedWriter().use { out ->
            imports?.let { out.appendLine(imports).appendLine() }
            out.appendLine("$startBlockText {")
            out.appendLine(codeBlock.prependIndent("    "))
            out.appendLine("}")
        }
    }
    fun writeInstallFunctionBody(configType: String) {
        writeInstallBody("public fun $configType.install()")
    }

    when(site) {
        CodeInjectionSite.DEFAULT,
        CodeInjectionSite.INSIDE_APP -> writeInstallFunctionBody("Application")
        CodeInjectionSite.IN_ROUTING -> writeInstallFunctionBody("Routing")
        CodeInjectionSite.SERIALIZATION_CONFIG -> writeInstallFunctionBody("ContentNegotiationConfig")
        CodeInjectionSite.CALL_LOGGING_CONFIG -> writeInstallFunctionBody("CallLoggingConfig")
        CodeInjectionSite.TEST_FUNCTION -> writeInstallBody("class ApplicationTest")
        CodeInjectionSite.OUTSIDE_APP -> {
            dir.resolve(site.defaultFileLocation!!).bufferedWriter().use { out ->
                imports?.let { out.appendLine(imports).appendLine() }
                out.appendLine(codeBlock)
            }
        }
        CodeInjectionSite.RESOURCES, CodeInjectionSite.SOURCE_FILE_KT -> {
            dir.resolve(fileName!!).bufferedWriter().use { out ->
                out.appendLine(codeBlock)
            }
        }
        CodeInjectionSite.APPLICATION_CONF,
        CodeInjectionSite.APPLICATION_YAML -> {
            dir.resolve(site.defaultFileLocation!!).bufferedWriter().use { out ->
                out.appendLine(codeBlock)
            }
        }
    }
}
