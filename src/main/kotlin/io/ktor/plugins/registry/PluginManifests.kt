/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

sealed interface ResolvedPluginManifest {
    fun export(outputFile: Path, json: Json)
}

class PluginResolutionContext(
    private val codeAnalysis: CodeAnalysis,
    private val release: KtorRelease,
    private val classLoader: ClassLoader,
    private val pluginsDir: Path,
) {
    private val logger = KotlinLogging.logger("PluginResolutionContext")

    /**
     * Preference goes:
     * - prebuilt JSON
     * - local YAML
     * - YAML from artifacts
     */
    fun resolveManifest(plugin: PluginReference): ResolvedPluginManifest? =
        resolvePrebuiltJson(plugin)
            ?: resolveYamlFile(plugin)
            ?: resolveYamlFromClasspath(plugin)

    private fun resolvePrebuiltJson(plugin: PluginReference): ResolvedPluginManifest? {
        return plugin.versionPath.resolve("manifest.json").ifExists()?.let { path ->
            logger.info { "${plugin.identifier} resolved from JSON" }
            PrebuiltJsonManifest(path)
        }
    }

    private fun resolveYamlFile(plugin: PluginReference) =
        plugin.versionPath.resolve("manifest.ktor.yaml").ifExists()?.let { path ->
            val model: YamlManifest.ImportManifest =
                path.inputStream().use(Yaml.default::decodeFromStream)

            logger.info { "${plugin.identifier} resolved from YAML" }
            YamlManifest(
                plugin = plugin,
                release = release,
                model = model,
                installSnippets = model.installation.mapValues { (site, installBlock) ->
                    readCodeSnippet(site, installBlock) {
                        plugin.readFileFromVersionPath(it)
                    }
                },
                documentationEntry = readDocumentation(model.documentation) {
                    plugin.readFileFromVersionPath(it)
                },
            )
        }

    private fun resolveYamlFromClasspath(plugin: PluginReference): YamlManifest? {
        val model: YamlManifest.ImportManifest = classLoader.getResourceAsStream(plugin.manifestResourceFile)
            ?.use(Yaml.default::decodeFromStream)
            ?: return null

        logger.info { "${plugin.identifier} resolved from classpath" }
        return YamlManifest(
            plugin = plugin,
            release = release,
            model = model,
            installSnippets = model.installation.mapValues { (site, installBlock) ->
                readCodeSnippet(site, installBlock) {
                    plugin.readFileFromClasspath(it)
                }
            },
            documentationEntry = readDocumentation(model.documentation) {
                plugin.readFileFromClasspath(it)
            },
        )
    }

    private val PluginReference.versionPath: Path get() =
        pluginsDir.resolve("${group.id}/$id/${versionRange.stripSpecialChars()}")

    private fun PluginReference.readFileFromVersionPath(filename: String) =
        versionPath.resolve(filename).ifExists()?.inputStream()

    private fun PluginReference.readFileFromClasspath(filename: String) =
        classLoader.getResourceAsStream("$resourcePath/${filename}")

    private fun Path.ifExists(): Path? = takeIf { it.exists() }

    private val PluginReference.manifestResourceFile: String get() =
        "$resourcePath/manifest.ktor.yaml"

    private val PluginReference.resourcePath: String get() =
        "${group.id.replace('.', '/')}/$id"

    private fun readCodeSnippet(
        site: String,
        codeSnippet: YamlManifest.CodeSnippetSource,
        findCodeInput: (String) -> InputStream?
    ): InstallSnippet {
        val injectionSite = CodeInjectionSite.valueOf(site.uppercase())
        val (code: String, filename: String?) = when (codeSnippet) {
            is YamlManifest.CodeSnippetSource.Text -> codeSnippet.code to null
            is YamlManifest.CodeSnippetSource.File -> {
                val code = findCodeInput(codeSnippet.file)?.use { it.readAllBytes().toString(Charset.defaultCharset()) }
                    ?: throw IllegalArgumentException("Missing install snippet ${codeSnippet.file}")
                code to codeSnippet.file
            }
        }
        return codeAnalysis.parseInstallSnippet(injectionSite, code, filename)
    }

    private fun readDocumentation(
        codeSnippet: YamlManifest.CodeSnippetSource,
        findCodeInput: (String) -> InputStream?
    ) = DocumentationExtractor.parseDocumentationMarkdown(when(codeSnippet) {
        is YamlManifest.CodeSnippetSource.Text -> codeSnippet.code
        is YamlManifest.CodeSnippetSource.File -> {
            findCodeInput(codeSnippet.file)?.use { it.readAllBytes().toString(Charset.defaultCharset()) }
                ?: throw IllegalArgumentException("Missing documentation file ${codeSnippet.file}")
        }
    })
}

/**
 * Migrated JSON files from earlier iterations of the project generator backend.
 */
data class PrebuiltJsonManifest(private val path: Path) : ResolvedPluginManifest {
    override fun export(outputFile: Path, json: Json) {
        Files.copy(path, outputFile)
    }
}

/**
 * manifest.ktor.yaml files imported via gradle OR resolved from plugin directory
 */
data class YamlManifest(
    private val plugin: PluginReference,
    private val release: KtorRelease,
    private val model: ImportManifest,
    private val installSnippets: Map<String, InstallSnippet>,
    private val documentationEntry: DocumentationEntry,
): ResolvedPluginManifest {
    init {
        model.validateImportModel()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun export(outputFile: Path, json: Json) {
        outputFile.outputStream().use { out ->
            json.encodeToStream(model.toExportModel(release), out)
        }
    }

    private fun ImportManifest.toExportModel(
        release: KtorRelease
    ): JsonObject = buildJsonObject {
        put("id", plugin.id)
        put("name", name)
        put("version", plugin.versionRange.stripSpecialChars())
        put("ktor_version", release.versionString)
        put("short_description", description)
        put("github", vcsLink)
        put("copyright", license)
        put("group", category.titleCase())
        putJsonObject("vendor") {
            put("name", plugin.group.name)
            put("url", plugin.group.url)
            put("email", plugin.group.email)
        }
        putJsonArray("required_feature_ids") {
            prerequisites?.forEach(::add)
        }
        putDocumentation()
        putInstallRecipe()
        putDependencies(release)
    }

    private fun JsonObjectBuilder.putDocumentation() {
        putJsonObject("documentation") {
            put("description", documentationEntry.description)
            put("usage", documentationEntry.usage)
            put("options", model.options.orEmpty().joinToString("\n") { (name, defaultValue, description) ->
                "* `$name` (default $defaultValue): $description"
            })
        }
    }

    private fun JsonObjectBuilder.putInstallRecipe() {
        putJsonObject("install_recipe") {
            installSnippets[CodeInjectionSite.DEFAULT.lowercaseName]?.let { defaultInstall ->
                putJsonArray("imports") {
                    (defaultInstall as? InstallSnippet.Kotlin)?.imports?.forEach(::add)
                }
                put("install_block", defaultInstall.code)
            }
            installSnippets[CodeInjectionSite.TEST_FUNCTION.lowercaseName]?.let { testFunctionInstall ->
                putJsonArray("imports") {
                    (testFunctionInstall as? InstallSnippet.Kotlin)?.imports?.forEach(::add)
                }
                // function template will be included in the following section
            }
            putJsonArray("templates") {
                val templateFiles = installSnippets.entries.filter { (key, _) ->
                    key != CodeInjectionSite.DEFAULT.lowercaseName
                }
                for ((position, snippet) in templateFiles)
                    add(buildJsonObject {
                        put("position", position)
                        put("text", snippet.code)
                        (snippet as? InstallSnippet.RawContent)?.filename?.let {
                            put("name", it)
                        }
                    })
            }
        }
    }

    private fun JsonObjectBuilder.putDependencies(release: KtorRelease) {
        putJsonArray("dependencies") {
            for (dependency in plugin.allArtifactsForVersion(release.versionString)) {
                addJsonObject {
                    put("group", dependency.group)
                    put("artifact", dependency.name)
                    put("version", when (val version = dependency.version) {
                        is VersionNumber -> version.toString()
                        is VersionRange -> version.toString()
                        MatchKtor -> "\$ktor_version"
                        else -> throw IllegalArgumentException("Unexpected version type ${version::class}")
                    })
                }
            }
        }
    }

    private fun ImportManifest.validateImportModel() {
        require(name.isNotBlank()) { "Property 'name' requires a value" }
        require(description.isNotBlank()) { "Property 'description' requires a value" }
        validateVcsLink()
        require(license.isNotBlank()) { "Property 'license' requires a value" }
        require(category.uppercase() in PluginCategory.namesUppercase) {
            "Property 'category' must be one of ${PluginCategory.names}"
        }
    }

    private fun ImportManifest.validateVcsLink() {
        try {
            URL(vcsLink)
        } catch (e: MalformedURLException) {
            throw IllegalArgumentException("Invalid VCS link \"$vcsLink\"", e)
        }
    }

    @Serializable
    data class ImportManifest(
        val name: String,
        val description: String,
        val vcsLink: String,
        val license: String,
        val category: String,
        val prerequisites: List<String>? = null,
        val documentation: CodeSnippetSource =
            CodeSnippetSource.File("documentation.md"),
        val options: List<ImportOption>? = null,
        val installation: Map<String, CodeSnippetSource> =
            mapOf(CodeInjectionSite.DEFAULT.name to CodeSnippetSource.File("install.kt"))
    )

    @Serializable
    data class ImportOption(
        val name: String,
        val defaultValue: String,
        val description: String,
    )

    @Serializable(with = CodeBlockSerializer::class)
    sealed class CodeSnippetSource {
        data class Text(val code: String): CodeSnippetSource()
        data class File(val file: String): CodeSnippetSource()
    }

    object CodeBlockSerializer : KSerializer<CodeSnippetSource> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("CodeBlock", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: CodeSnippetSource) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): CodeSnippetSource {
            val text = decoder.decodeString()
            return if (text.matches(Regex("\\S+\\.\\S{1,5}")))
                CodeSnippetSource.File(text)
            else
                CodeSnippetSource.Text(text)
        }
    }

}

enum class PluginCategory(val acronym: Boolean = false) {
    ADMINISTRATION,
    DATABASES,
    HTTP(acronym = true),
    MONITORING,
    ROUTING,
    SECURITY,
    SERIALIZATION,
    SOCKETS,
    TEMPLATING;

    companion object {
        val namesUppercase = entries.asSequence().map { it.name }.toSet()
        val names = entries.asSequence().map { if (it.acronym) it.name else it.name.titleCase() }.toSet()
    }
}

private fun String.titleCase() = get(0) + substring(1).lowercase()
