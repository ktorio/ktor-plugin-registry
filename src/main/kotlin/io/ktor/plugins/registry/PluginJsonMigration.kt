/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import kotlinx.serialization.json.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.BufferedWriter
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Used for migrating old JSON files to the new YAML format.
 */
fun migratePluginJson(jsonFile: Path) {
    require(jsonFile.exists()) {
        "Could not find JSON file listed under path $jsonFile"
    }

    val yamlFile = jsonFile.parent.resolve("manifest.ktor.yaml")
    yamlFile.bufferedWriter().use { yamlWriter ->
        yamlWriter.writeYamlFile(
            outputDir = jsonFile.parent,
            rootNode = Json.parseToJsonElement(jsonFile.readText()).jsonObject
        )
    }
}

private fun BufferedWriter.writeYamlFile(outputDir: Path, rootNode: JsonObject) {
    for ((fieldName, node) in rootNode.entries) {
        when(fieldName) {
            "id",
            "version",
            "vendor",
            "dependencies",
            "target",
            "ktor_version" -> println("Skipped field $fieldName")
            "name" -> appendLine("name: ${node.stringValue}")
            "short_description" -> appendLine("description: ${node.stringValue}")
            "github" -> appendLine("vcsLink: ${node.stringValue}")
            "copyright" -> appendLine("license: ${node.stringValue}")
            "group" -> appendLine("category: ${node.stringValue}")
            "required_feature_ids" -> {
                val arrayOutput = node.jsonArray.map { it.stringValue }.joinToString("") { "  - $it" }
                appendLine("prerequisites:\n$arrayOutput")
            }
            "documentation" -> writeDocumentationFile(node, outputDir)
            "install_recipe" -> writeInstallRecipeFiles(
                node,
                outputDir,
                categoryName = rootNode["group"]?.stringValue,
                client = rootNode["target"]?.stringValue == "client"
            )
            "gradle_install" -> writeGradleInstall(node)
            "maven_install" -> writeMavenInstall(node)
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
        usage?.takeIf { it.isNotBlank() }?.let { out.appendLine("## Usage\n").appendLine(usage) }
        options?.takeIf { it.isNotBlank() }?.let { out.appendLine("## Options\n").appendLine(options) }
    }
}

private fun BufferedWriter.writeInstallRecipeFiles(
    node: JsonElement,
    outputDir: Path,
    categoryName: String?,
    client: Boolean,
) {
    val installRecipeObject = node.jsonObject
    val (imports, installBlock, templates, testImports) = listOf(
        "imports",
        "install_block",
        "templates",
        "test_imports",
    ).map { installRecipeObject[it] }

    fun JsonElement?.formatImports() =
        (this?.jsonArray?.map { it.stringValue }.orEmpty()).map { "import $it" }

    if (installBlock == null && templates?.jsonArray.orEmpty().isEmpty())
        return

    val hasTemplates = !templates?.jsonArray.isNullOrEmpty()
    if (hasTemplates)
        appendLine("installation:")

    installBlock?.let {
        if (hasTemplates)
            appendLine("  default: install.kt")
        val defaultInstallRecipe = OutputInstallTemplate(
            site = CodeInjectionSite.DEFAULT,
            categoryName = categoryName,
            codeBlock =  installBlock.stringValue,
            imports = imports.formatImports(),
            fileName = CodeInjectionSite.DEFAULT.defaultFileLocation!!
        )
        defaultInstallRecipe.writeInstallRecipe(outputDir, client)
    }

    val installTemplates = templates?.jsonArray.orEmpty().asSequence().map { json ->
        ImportedInstallTemplate(
            json.jsonObject["position"]!!.stringValue,
            json.jsonObject["text"]!!.stringValue,
            json.jsonObject["name"]?.stringValue,
        )
    }.groupBy(ImportedInstallTemplate::position)

    for ((position, templatesAtPosition) in installTemplates) {
        val template = templatesAtPosition.reduce { (position, text, name), right ->
            ImportedInstallTemplate(position, text + "\n\n    " + right.text, name)
        }
        val templateImports = if (position == "test_function") testImports else imports
        val installTemplate = template.asOutputInstallTemplate(templateImports.formatImports())
        appendLine("  ${installTemplate.site.lowercaseName}: ${installTemplate.fileName}")
        installTemplate.writeInstallRecipe(outputDir)
    }
}

/**
 * Basic install template information imported from JSON.
 */
data class ImportedInstallTemplate(
    val position: String,
    val text: String,
    val name: String?,
) {
    fun asOutputInstallTemplate(imports: List<String>): OutputInstallTemplate {
        val site = CodeInjectionSite.valueOf(position.uppercase())
        var fileName = name ?: site.defaultFileLocation
        check(fileName != null) {
            "Missing filename for template $position"
        }
        if (site == CodeInjectionSite.SOURCE_FILE_KT)
            fileName += ".kt"
        return OutputInstallTemplate(
            site = site,
            codeBlock = text,
            imports = imports,
            fileName = fileName.substringAfterLast('/')
        )
    }
}

/**
 * Refined data for outputting install template to separate file.
 */
data class OutputInstallTemplate(
    val site: CodeInjectionSite,
    val categoryName: String? = null,
    val codeBlock: String,
    val imports: List<String>,
    val fileName: String,
)

private fun OutputInstallTemplate.writeInstallRecipe(dir: Path, client: Boolean = false) {

    fun writeInstallBody(startBlockText: String, extraImports: List<String>) {
        dir.resolve(site.defaultFileLocation!!).bufferedWriter().use { out ->
            (imports + extraImports).sorted().distinct().forEach { out.appendLine(it) }
            out.appendLine()
            out.appendLine("$startBlockText {")
            // sometimes code blocks are indented (except first line)
            // sometimes they are not
            out.appendLine(
                when {
                    // "    " in codeBlock -> "    $codeBlock"
                    else -> codeBlock.prependIndent("    ")
                }
            )
            out.appendLine("}")
        }
    }

    fun writeInstallFunctionBody(configMethod: String, extraImports: List<String> = emptyList()) {
        writeInstallBody("public fun $configMethod()", extraImports)
    }

    when(site) {
        CodeInjectionSite.DEFAULT -> {
            if (client) {
                writeInstallFunctionBody("HttpClientConfig<*>.configure", listOf(
                    "import io.ktor.client.*"
                ))
            } else writeInstallFunctionBody("Application.configure${categoryName!!}", listOf(
                "import io.ktor.server.application.*",
                "import io.ktor.server.response.*",
            ))
        }
        CodeInjectionSite.INSIDE_APP -> writeInstallFunctionBody("Application.install", listOf(
            "import io.ktor.server.application.*",
            "import io.ktor.server.response.*",
        ))
        CodeInjectionSite.IN_ROUTING -> writeInstallFunctionBody("Routing.configureRouting", listOf(
            "import io.ktor.server.application.*",
            "import io.ktor.server.response.*",
            "import io.ktor.server.routing.*",
        ))
        CodeInjectionSite.SERIALIZATION_CONFIG ->
            writeInstallFunctionBody("ContentNegotiationConfig.configureContentNegotiation")
        CodeInjectionSite.CALL_LOGGING_CONFIG -> writeInstallFunctionBody(
            "CallLoggingConfig.configureLogging", listOf(
            "import io.ktor.server.plugins.callloging.*",
            "import io.ktor.server.routing.*",
        ))
        CodeInjectionSite.TEST_FUNCTION -> writeInstallBody("class ApplicationTest", listOf(
            "import io.ktor.server.testing.*",
            "import kotlin.test.*",
        ))
        CodeInjectionSite.OUTSIDE_APP -> {
            dir.resolve(site.defaultFileLocation!!).bufferedWriter().use { out ->
                (imports + "import io.ktor.server.application.*").sorted().forEach { out.appendLine(it) }
                out.appendLine()
                out.appendLine(codeBlock)
            }
        }
        CodeInjectionSite.RESOURCES, CodeInjectionSite.SOURCE_FILE_KT -> {
            dir.resolve(fileName).bufferedWriter().use { out ->
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

private fun BufferedWriter.writeGradleInstall(node: JsonElement) {
    appendLine("gradle:")
    node.jsonObject["plugins"]?.let { pluginsNode ->
        pluginsNode.jsonArray.ifNotEmpty {
            appendLine("  plugins:")
            forEach { node ->
                appendLine(
                    """
                        - id: ${node.jsonObject["id"]?.stringValue}
                          version: ${node.jsonObject["version"]?.stringValue}
                    """.trimIndent().prependIndent("    ")
                )
            }
        }
    }
    node.jsonObject["repositories"]?.let { repositoriesNode ->
        repositoriesNode.jsonArray.ifNotEmpty {
            appendLine("  repositories:")
            forEach { node ->
                appendLine("    - url: ${node.jsonObject["url"]?.stringValue}")
            }
        }
    }
}

private fun BufferedWriter.writeMavenInstall(node: JsonElement) {
    appendLine("maven:")
    node.jsonObject["plugins"]?.let { pluginsNode ->
        pluginsNode.jsonArray.ifNotEmpty {
            appendLine("  plugins:")
            forEach { node ->
                appendLine(
                    """
                        - group: ${node.jsonObject["group"]?.stringValue}
                          artifact: ${node.jsonObject["artifact"]?.stringValue}
                          version: ${node.jsonObject["version"]?.stringValue}
                    """.trimIndent().prependIndent("    ")
                )
                node.jsonObject["extra"]?.stringValue?.let { extra ->
                    appendLine("      extra: |-")
                    appendLine("            $extra".trimIndent().prependIndent("        "))
                }
            }
        }
    }
    node.jsonObject["repositories"]?.let { repositoriesNode ->
        repositoriesNode.jsonArray.ifNotEmpty {
            appendLine("  repositories:")
            forEach { node ->
                appendLine("""
                    - id: ${node.jsonObject["id"]?.stringValue}
                      url: ${node.jsonObject["url"]?.stringValue}
                """.trimIndent().prependIndent("    "))
            }
        }
    }
}
