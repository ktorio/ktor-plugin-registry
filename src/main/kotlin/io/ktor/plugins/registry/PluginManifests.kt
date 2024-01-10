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
            val model: YamlManifest.ImportManifest = path.inputStream().use(Yaml.default::decodeFromStream)
            val installation: Map<String, InstallSnippet> = model.installation.mapValues { (site, installBlock) ->
                readCodeSnippet(site, installBlock) { filename ->
                    plugin.versionPath.resolve(filename).ifExists()?.inputStream()
                }
            }
            YamlManifest(plugin, release, model, installation, source = "plugin files")
        }

    private fun resolveYamlFromClasspath(plugin: PluginReference): YamlManifest? {
        val model: YamlManifest.ImportManifest = classLoader.getResourceAsStream(plugin.manifestResourceFile)
            ?.use(Yaml.default::decodeFromStream)
            ?: return null
        val installation: Map<String, InstallSnippet> = model.installation.mapValues { (site, installBlock) ->
            readCodeSnippet(site, installBlock) { filename ->
                classLoader.getResourceAsStream("${plugin.resourcePath}/${filename}")
            }
        }
        return YamlManifest(plugin, release, model, installation, source = "classpath")
    }

    private val PluginReference.versionPath: Path
        get() = pluginsDir.resolve("${group.id}/$id/${versionRange.stripSpecialChars()}")

    private fun Path.ifExists(): Path? = takeIf { it.exists() }

    private val PluginReference.manifestResourceFile: String get() =
        "$resourcePath/manifest.ktor.yaml"

    private val PluginReference.resourcePath: String get() =
        "${group.id.replace('.', '/')}/$id"

    private fun String.stripSpecialChars() =
        Regex("[^a-zA-Z0-9-.]").replace(this, "")

    private fun readCodeSnippet(
        site: String,
        installBlock: YamlManifest.CodeSnippetSource,
        findCodeInput: (String) -> InputStream?
    ): InstallSnippet {
        val injectionSite = CodeInjectionSite.valueOf(site.uppercase())
        val (code: String, filename: String?) = when (installBlock) {
            is YamlManifest.CodeSnippetSource.Text -> installBlock.code to null
            is YamlManifest.CodeSnippetSource.File -> {
                val code = findCodeInput(installBlock.file)?.use { it.readAllBytes().toString(Charset.defaultCharset()) }
                    ?: throw IllegalArgumentException("Missing install snippet ${installBlock.file}")
                code to installBlock.file
            }
        }
        return snippetExtractor.parseInstallSnippet(injectionSite, code, filename)
    }
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
    private val source: String,
    private val logger: KLogger = KotlinLogging.logger("YamlManifest")
): ResolvedPluginManifest {

    @OptIn(ExperimentalSerializationApi::class)
    override fun export(outputFile: Path, json: Json) {
        logger.info { "${plugin.identifier} resolved from $source" }
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
        put("copyright", copyright)
        put("group", category.titleCase())
        putJsonObject("vendor") {
            put("name", plugin.group.name)
            put("url", plugin.group.url)
        }
        putJsonArray("required_feature_ids") {
            prerequisites?.forEach(::add)
        }
        putJsonObject("documentation") {
            put("description", documentation.description)
            put("usage", documentation.usage)
            put("options", documentation.options.orEmpty().joinToString("\n") { "* $it" })
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

    private fun String.titleCase() = get(0) + substring(1).lowercase()

    @Serializable
    data class ImportManifest(
        val name: String,
        val description: String,
        val vcsLink: String,
        val copyright: String,
        val category: String,
        val documentation: ImportDocumentation,
        val prerequisites: List<String>? = null,
        val installation: Map<String, CodeSnippetSource> = CodeSnippetSource.DEFAULT
    )

    @Serializable
    data class ImportDocumentation(
        val description: String? = null,
        val usage: String? = null,
        val options: List<String>? = null
    )

    @Serializable(with = CodeBlockSerializer::class)
    sealed class CodeSnippetSource {
        companion object {
            val DEFAULT = mapOf<String, CodeSnippetSource>(CodeBlockLocation.DEFAULT to CodeSnippetSource.File("install.kt"))
        }
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