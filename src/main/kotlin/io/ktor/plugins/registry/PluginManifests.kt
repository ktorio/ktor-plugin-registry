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
    private val snippetExtractor: CodeSnippetExtractor,
    private val release: KtorRelease,
    private val classLoader: ClassLoader,
    private val pluginsDir: Path,
) {

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

    private fun resolvePrebuiltJson(plugin: PluginReference) =
        plugin.versionPath.resolve("manifest.json").ifExists()?.let { path ->
            PrebuiltJsonManifest(plugin, path)
        }

    private fun resolveYamlFile(plugin: PluginReference) =
        plugin.versionPath.resolve("manifest.ktor.yaml").ifExists()?.let { path ->
            val model: YamlManifest.ImportManifest =
                path.inputStream().use(Yaml.default::decodeFromStream)

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
                sourceDescription = "plugin files"
            )
        }

    private fun resolveYamlFromClasspath(plugin: PluginReference): YamlManifest? {
        val model: YamlManifest.ImportManifest = classLoader.getResourceAsStream(plugin.manifestResourceFile)
            ?.use(Yaml.default::decodeFromStream)
            ?: return null

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
            sourceDescription = "classpath"
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
        return snippetExtractor.parseInstallSnippet(injectionSite, code, filename)
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
class PrebuiltJsonManifest(
    private val plugin: PluginReference,
    private val path: Path,
    private val logger: KLogger = KotlinLogging.logger("PrebuiltJsonManifest")
) : ResolvedPluginManifest {
    override fun export(outputFile: Path, json: Json) {
        logger.info { "${plugin.identifier} copied from ${path.normalizeAndRelativize()}" }
        Files.copy(path, outputFile)
    }
}

/**
 * manifest.ktor.yaml files imported via gradle OR resolved from plugin directory
 */
class YamlManifest(
    private val plugin: PluginReference,
    private val release: KtorRelease,
    private val model: ImportManifest,
    private val installSnippets: Map<String, InstallSnippet>,
    private val documentationEntry: DocumentationEntry,
    private val sourceDescription: String,
    private val logger: KLogger = KotlinLogging.logger("YamlManifest")
): ResolvedPluginManifest {
    init {
        model.validateImportModel()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun export(outputFile: Path, json: Json) {
        logger.info { "${plugin.identifier} resolved from $sourceDescription" }
        outputFile.outputStream().use { out ->
            json.encodeToStream(model.toExportModel(release), out)
        }
    }

    private fun ImportManifest.toExportModel(
        release: KtorRelease
    ): JsonObject = buildJsonObject {
        put("id", plugin.id)
        put("name", name)
        put("version", plugin.versionRange) // TODO assuming version doesn't really matter
        put("ktor_version", release.versionString)
        put("short_description", description)
        put("github", vcsLink)
        put("copyright", license)
        put("group", category.titleCase())
        putJsonObject("vendor") {
            put("name", plugin.group.name)
            put("url", plugin.group.url)
        }
        putJsonArray("required_feature_ids") {
            prerequisites?.forEach(::add)
        }
        putJsonObject("documentation") {
            put("description", documentationEntry.description)
            put("usage", documentationEntry.usage)
            put("options", options.orEmpty().joinToString("\n") { (name, defaultValue, description) ->
                "* `$name` (default $defaultValue): $description"
            })
        }
        putJsonObject("install_recipe") {
            installSnippets[CodeBlockLocation.DEFAULT]?.let { defaultInstall ->
                putJsonArray("imports") {
                    (defaultInstall as? InstallSnippet.Kotlin)?.imports?.forEach(::add)
                }
                put("install_block", defaultInstall.code)
            }
            putJsonArray("templates") {
                for ((position, snippet) in installSnippets.entries.filter { (key, _) -> key != CodeBlockLocation.DEFAULT })
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

    private fun ImportManifest.validateImportModel() {
        require(name.isNotBlank()) { "Property 'name' requires a value" }
        require(description.isNotBlank()) { "Property 'description' requires a value" }
        validateVcsLink()
        require(license.isNotBlank()) { "Property 'license' requires a value" }
        require(category.uppercase() in PluginCategory.namesUppercase) { "Property 'category' must be one of ${PluginCategory.names}" }
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

    object CodeBlockLocation {
       const val DEFAULT = "default"
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