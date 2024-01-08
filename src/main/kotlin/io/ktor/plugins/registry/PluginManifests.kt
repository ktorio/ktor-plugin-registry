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
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.outputStream

sealed interface ResolvedPluginManifest {
    fun export(outputFile: Path, json: Json)
}

/**
 * Migrated JSON files from earlier iterations of the project generator backend.
 */
class PrebuiltJsonManifest(
    private val plugin: PluginReference,
    private val path: Path,
    private val logger: KLogger = KotlinLogging.logger("PrebuiltManifestJson")
) : ResolvedPluginManifest {
    companion object {
        fun Path.findPrebuiltManifest(plugin: PluginReference): PrebuiltJsonManifest? =
            resolve(plugin.manifestPrebuiltFile).ifExists()?.let { path ->
                PrebuiltJsonManifest(plugin, path)
            }

        private val PluginReference.manifestPrebuiltFile: String get() =
            "${group.id}/$id/manifest$versionRange.json"

        private fun Path.ifExists(): Path? = takeIf { it.exists() }
    }

    override fun export(outputFile: Path, json: Json) {
        logger.info { "${plugin.identifier} copied from ${path.normalizeAndRelativize()}" }
        Files.copy(path, outputFile)
    }

}

/**
 * manifest.ktor.yaml files imported via gradle.
 */
class ResourceYamlManifest(
    private val plugin: PluginReference,
    private val release: KtorRelease,
    private val model: ImportManifest,
    private val installSnippets: Map<String, InstallSnippet>,
    private val logger: KLogger = KotlinLogging.logger("ResourceYamlManifest")
): ResolvedPluginManifest {

    companion object {
        fun ClassLoader.resolveManifestYaml(snippetExtractor: CodeSnippetExtractor, plugin: PluginReference, release: KtorRelease): ResourceYamlManifest? {
            val model: ImportManifest = getResourceAsStream(plugin.manifestResourceFile)
                ?.use(Yaml.default::decodeFromStream)
                ?: return null
            val installation: Map<String, InstallSnippet> = model.installation.mapValues { (site, installBlock) ->
                val injectionSite = CodeInjectionSite.valueOf(site.uppercase())
                val (code, filename) = when(installBlock) {
                    is CodeSnippetSource.Text -> installBlock.code to null
                    is CodeSnippetSource.Resource -> getResourceAsStream("${plugin.resourcePath}/${installBlock.file}")?.use {
                        it.readAllBytes().toString(Charset.defaultCharset()) to installBlock.file
                    } ?: throw IllegalArgumentException("Missing install snippet ${installBlock.file}")
                }
                snippetExtractor.parseInstallSnippet(injectionSite, code, filename)
            }
            return ResourceYamlManifest(plugin, release, model, installation)
        }

        private val PluginReference.manifestResourceFile: String get() =
            "$resourcePath/manifest.ktor.yaml"

        private val PluginReference.resourcePath: String get() =
            "${group.id.replace('.', '/')}/$id"
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun export(outputFile: Path, json: Json) {
        logger.info { "${plugin.identifier} resolved from dependencies" }
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
            val DEFAULT = mapOf<String, CodeSnippetSource>(CodeBlockLocation.DEFAULT to CodeSnippetSource.Resource("install.kt"))
        }
        data class Text(val code: String): CodeSnippetSource()
        data class Resource(val file: String): CodeSnippetSource()
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
                CodeSnippetSource.Resource(text)
            else
                CodeSnippetSource.Text(text)
        }
    }

}