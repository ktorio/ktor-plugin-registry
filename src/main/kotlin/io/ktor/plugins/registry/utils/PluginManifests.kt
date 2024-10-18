/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import io.ktor.plugins.registry.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.outputStream

sealed interface ResolvedPluginManifest {
    fun validate(release: KtorRelease)
    context(ReleasePluginResolution)
    fun export(outputFile: Path, json: Json)
}

/**
 * Migrated JSON files from earlier iterations of the project generator backend.
 */
data class PrebuiltJsonManifest(private val path: Path) : ResolvedPluginManifest {
    override fun validate(release: KtorRelease) {
        // Assumed to be validated when built
    }

    context(ReleasePluginResolution)
    override fun export(outputFile: Path, json: Json) {
        Files.copy(path, outputFile)
    }
}

/**
 * manifest.ktor.yaml files imported via gradle OR resolved from plugin directory
 */
data class YamlManifest(
    private val plugin: PluginReference,
    private val model: ImportManifest,
    private val codeRefs: List<CodeRef>,
    private val documentationEntry: DocumentationEntry,
    private val logo: Path?,
): ResolvedPluginManifest {
    companion object {
        val DEFAULT = CodeInjectionSite.DEFAULT.lowercaseName
        val TEST = CodeInjectionSite.TEST_FUNCTION.lowercaseName
    }

    override fun validate(release: KtorRelease) {
        require(model.name.isNotBlank()) { "Property 'name' requires a value" }
        require(model.description.isNotBlank()) { "Property 'description' requires a value" }
        model.validateVcsLink()
        require(model.license.isNotBlank()) { "Property 'license' requires a value" }
        require(model.category.uppercase() in PluginCategory.namesUppercase) {
            "Property 'category' must be one of ${PluginCategory.names}"
        }
        require(model.prerequisites.orEmpty().all { it in release.pluginIds }) {
            val missingPrerequisites = model.prerequisites.orEmpty()
                .filter { it !in release.pluginIds }
                .joinToString()
            "Missing prerequisite plugin(s): $missingPrerequisites"
        }
    }

    context(ReleasePluginResolution)
    @OptIn(ExperimentalSerializationApi::class)
    override fun export(outputFile: Path, json: Json) {
        outputFile.outputStream().use { out ->
            json.encodeToStream(model.toExportModel(), out)
        }
    }

    context(ReleasePluginResolution)
    @OptIn(ExperimentalSerializationApi::class)
    private fun ImportManifest.toExportModel(): JsonObject = buildJsonObject {
        val version = when(plugin.versionRange.stripSpecialChars()) {
            "2.0" -> "2.0.0"
            else -> release.versionString
        }
        put("id", plugin.id)
        put("name", name)
        put("version", version)
        put("ktor_version", version)
        put("short_description", description)
        put("github", vcsLink)
        put("copyright", license)
        putJsonObject("vendor") {
            put("name", plugin.group.name)
            put("url", plugin.group.url)
            if (plugin.group.name != "Ktor") {
                put("email", plugin.group.email)
                put("logo", logo?.name ?: plugin.group.outputLogo)
            }
        }
        put("group", PluginCategory.valueOf(category.uppercase()).nameTitleCase)
        prerequisites?.ifNotEmpty {
            putJsonArray("required_feature_ids") {
                addAll(prerequisites)
            }
        }
        putInstallRecipe()
        putGradleInstall()
        putMavenInstall()
        putDependencies(release)
        putDocumentation()
        if (plugin.client)
            put("target", "client")
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
            putJsonArray("imports") {
                codeRefs.asSequence()
                    .filter { it.isMainInjectionSite() }
                    .flatMap { it.importsOrEmpty }
                    .distinct()
                    .forEach(::add)
            }
            codeRefs.find { it.site == CodeInjectionSite.DEFAULT }?.let { ref ->
                put("install_block", ref.code)
            }
            codeRefs.find { it.site == CodeInjectionSite.TEST_FUNCTION }?.let { ref ->
                putJsonArray("test_imports") {
                    ref.importsOrEmpty.forEach(::add)
                }
                // function template will be included in the following section
            }
            codeRefs.filter { it.site != CodeInjectionSite.DEFAULT }.takeIf { it.isNotEmpty() }?.let { refs ->
                putJsonArray("templates") {
                    for (ref in refs)
                        add(buildJsonObject {
                            put("position", ref.site.lowercaseName)
                            put("text", ref.code)
                            when(ref) {
                                is CodeRef.InjectedKotlin -> {}
                                is CodeRef.SourceFile -> {
                                    if (ref.file != null) put("name", ref.file)
                                    if (ref.module != null) put("module", ref.module)
                                    if (ref.test) put("test", true)
                                }
                            }
                        })
                }
            }
        }
    }

    context(ReleasePluginResolution)
    private fun JsonObjectBuilder.putDependencies(release: KtorRelease) {
        putJsonArray("dependencies") {
            val artifacts = plugin.allArtifactsForVersion(release.versionString)
                .map { releaseArtifacts.resolveActualVersion(it) }
            for (dependency in artifacts) {
                addJsonObject {
                    put("group", dependency.group)
                    put("artifact", dependency.name)
                    put("version", when (val version = dependency.version) {
                        is VersionNumber -> version.toString()
                        is VersionRange -> version.toString()
                        is VersionVariable -> version.normalizedName
                        MatchKtor -> "\$ktor_version"
                        else -> throw IllegalArgumentException("Unexpected version type ${version::class}")
                    })
                    if (dependency.version is VersionVariable)
                        put("version_value", dependency.version.toString())
                }
            }
        }
    }

    private fun JsonObjectBuilder.putGradleInstall() {
        model.gradle?.let { gradle ->
            put("gradle_install", buildJsonObject {
                putJsonArray("plugins") {
                    for (plugin in gradle.plugins) {
                        addJsonObject {
                            put("id", plugin.id)
                            put("version", plugin.version)
                        }
                    }
                }
                putJsonArray("repositories") {
                    for (repository in gradle.repositories) {
                        addJsonObject {
                            put("type", "url_based")
                            put("url", repository.url)
                        }
                    }
                }
            })
        }
    }

    private fun JsonObjectBuilder.putMavenInstall() {
        model.maven?.let { maven ->
            putJsonObject("maven_install") {
                if (maven.plugins.isNotEmpty()) {
                    putJsonArray("plugins") {
                        for (plugin in maven.plugins) {
                            addJsonObject {
                                put("group", plugin.group)
                                put("artifact", plugin.artifact)
                                put("version", plugin.version)
                                put("extra", plugin.extra)
                            }
                        }
                    }
                }
                if (maven.repositories.isNotEmpty()) {
                    putJsonArray("repositories") {
                        for (repository in maven.repositories) {
                            addJsonObject {
                                put("id", repository.id)
                                put("url", repository.url)
                            }
                        }
                    }
                }
            }
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
            mapOf(CodeInjectionSite.DEFAULT.lowercaseName to CodeSnippetSource.File("install.kt")),
        val sources: List<CustomSourceFileTemplate> = emptyList(),
        val resources: List<CustomSourceFileTemplate> = emptyList(),
        val gradle: GradleInstallRecipe? = null,
        val maven: MavenInstallRecipe? = null,
    )

    @Serializable
    data class ImportOption(
        val name: String,
        val defaultValue: String,
        val description: String,
    )

    @Serializable
    data class GradleInstallRecipe(
        val repositories: MutableList<GradleRepository> = mutableListOf(),
        val plugins: List<GradlePlugin> = emptyList()
    )

    @Serializable
    data class GradleRepository(
        val url: String? = null
    )

    @Serializable
    data class GradlePlugin(
        val id: String,
        val version: String
    )

    @Serializable
    data class MavenInstallRecipe(
        val repositories: MutableList<MavenRepository> = mutableListOf(),
        val plugins: List<MavenPlugin> = emptyList()
    )

    @Serializable
    data class MavenRepository(
        val id: String,
        val url: String
    )

    @Serializable
    data class MavenPlugin(
        val group: String,
        val artifact: String,
        val version: String? = null,
        val extra: String? = null
    )

    @Serializable
    data class CustomSourceFileTemplate(
        val file: String,
        val module: String? = null,
        val test: Boolean = false,
    )

    @Serializable(with = CodeBlockSerializer::class)
    sealed class CodeSnippetSource {
        data class Text(val code: String) : CodeSnippetSource()
        data class File(val file: String) : CodeSnippetSource()
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
    FRAMEWORKS,
    HTTP(acronym = true),
    MONITORING,
    ROUTING,
    SECURITY,
    SERIALIZATION,
    SOCKETS,
    TEMPLATING;

    companion object {
        val namesUppercase = entries.asSequence().map { it.name }.toSet()
        val names = entries.asSequence().map { it.nameTitleCase }.toSet()
    }

    val nameTitleCase get() = if (acronym) name else name.wordTitleCase()
}

fun String.wordTitleCase() = get(0) + substring(1).lowercase()

val VersionVariable.normalizedName: String get() {
    val variableName = name.replace(Regex("(?<=[a-z])[A-Z]"), "_$0").replace('-', '_').lowercase()
    return '$' + if (variableName.endsWith("_version")) variableName else variableName + "_version"
}

val KtorRelease.pluginIds: Set<String> get() = plugins.mapToSetOrEmpty { it.id }
