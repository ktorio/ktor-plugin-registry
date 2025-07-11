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
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.outputStream

sealed interface ResolvedPluginManifest {
    fun validate()
    fun export(outputFile: Path, json: Json)
}

/**
 * manifest.ktor.yaml files imported via gradle OR resolved from plugin directory
 */
data class PluginManifestData(
    private val plugin: PluginConfiguration,
    private val group: PluginGroup,
    private val model: ImportManifest,
    private val modules: Collection<ProjectModule>,
    private val codeRefs: List<CodeRef>,
    private val documentationEntry: DocumentationEntry,
    private val logo: Path?,
    private val versionProperties: Map<String, String>,
) : ResolvedPluginManifest {
    override fun validate() {
        require(model.name.isNotBlank()) { "Property 'name' requires a value" }
        require(model.description.isNotBlank()) { "Property 'description' requires a value" }
        model.validateVcsLink()
        require(model.license.isNotBlank()) { "Property 'license' requires a value" }
        require(model.category.uppercase() in PluginCategory.namesUppercase) {
            "Property 'category' must be one of ${PluginCategory.names}"
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun export(outputFile: Path, json: Json) {
        outputFile.outputStream().use { out ->
            json.encodeToStream(model.toExportModel(), out)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun ImportManifest.toExportModel(): JsonObject = buildJsonObject {
        put("id", plugin.id)
        put("name", name)
        put("version", plugin.release)
        put("ktor_version", plugin.release)
        put("short_description", description)
        put("github", vcsLink)
        put("copyright", license)
        putJsonObject("vendor") {
            put("name", group.name)
            put("url", group.url)
            if (group.name != "Ktor") {
                put("email", group.email)
                put("logo", logo?.name ?: group.outputLogo)
            }
        }
        put("group", PluginCategory.valueOf(category.uppercase()).nameTitleCase)
        prerequisites?.ifNotEmpty {
            putJsonArray("required_feature_ids") {
                addAll(prerequisites)
            }
        }
        putJsonArray("modules") {
            addAll(modules.map { it.name})
        }
        putInstallRecipe()
        putGradleInstall()
        putMavenInstall()
        putAmperInstall()
        putDependencies()
        putDocumentation()
        if (plugin.type == "client")
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

    @OptIn(ExperimentalSerializationApi::class)
    private fun JsonObjectBuilder.putInstallRecipe() {
        putJsonObject("install_recipe") {
            codeRefs.find { it.site == CodeInjectionSite.DEFAULT }?.let { ref ->
                putJsonArray("imports") {
                    addAll(ref.importsOrEmpty)
                }
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
                            when (ref) {
                                is CodeRef.InjectedKotlin -> {
                                    putJsonArray("imports") {
                                        addAll(ref.importsOrEmpty)
                                    }
                                }
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

    private fun JsonObjectBuilder.putDependencies() {
        putJsonArray("dependencies") {
            for (dependency in plugin.artifacts) {
                addJsonObject {
                    put("artifact", dependency.name)
                    putVersion(dependency.version)
                    if (dependency.module != null) {
                        put("module", dependency.module?.name)
                    }
                    if (dependency.group != null) {
                        put("group", dependency.group)
                    }
                    if (dependency.function != null) {
                        put("function", dependency.function)
                    }
                    put("alias", dependency.alias)
                }
            }
        }
    }

    private fun JsonObjectBuilder.putVersion(version: ArtifactVersion) {
        put("version", version.toExportString())
        if (version is VersionVariable) {
            version.asNumber()?.let { versionValue ->
                put("version_value", versionValue.toString())
            }
        }
        version.asRange()?.let { range ->
            put("version_range", range.toString())
        }
    }

    private fun JsonObjectBuilder.putGradleInstall() {
        model.gradle?.let { gradle ->
            put("gradle_install", buildJsonObject {
                if (gradle.disabled) {
                    put("disabled", true)
                } else {
                    putJsonArray("plugins") {
                        for (plugin in gradle.plugins) {
                            addJsonObject {
                                put("id", plugin.id)
                                putVersion(plugin.version)
                                put("module", plugin.module)
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
                }
            })
        }
    }

    private fun JsonObjectBuilder.putMavenInstall() {
        model.maven?.let { maven ->
            putJsonObject("maven_install") {
                if (maven.disabled) {
                    put("disabled", true)
                } else {
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

    private fun JsonObjectBuilder.putAmperInstall() {
        model.amper?.let { amper ->
            putJsonObject("amper_install") {
                put("disabled", amper.disabled)
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
        val sources: List<CodeSnippetSource> = emptyList(),
        val resources: List<CodeSnippetSource> = emptyList(),
        val gradle: GradleInstallRecipe? = null,
        val maven: MavenInstallRecipe? = null,
        val amper: AmperInstallRecipe? = null
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
        val plugins: List<GradlePlugin> = emptyList(),
        val disabled: Boolean = false,
    )

    @Serializable
    data class GradleRepository(
        val url: String? = null
    )

    @Serializable
    data class GradlePlugin(
        val id: String,
        val version: ArtifactVersion,
        val module: String? = null,
    )

    @Serializable
    data class MavenInstallRecipe(
        val repositories: MutableList<MavenRepository> = mutableListOf(),
        val plugins: List<MavenPlugin> = emptyList(),
        val disabled: Boolean = false,
    )

    @Serializable
    data class AmperInstallRecipe(
        val disabled: Boolean = false,
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
        val extra: String? = null,
        val module: String? = null,
    )

    @Serializable(with = CodeBlockSerializer::class)
    sealed class CodeSnippetSource {
        data class Text(val code: String) : CodeSnippetSource()
        data class File(
            val file: String,
            val module: String? = null,
            val test: Boolean = false,
        ) : CodeSnippetSource()
    }

    object CodeBlockSerializer : KSerializer<CodeSnippetSource> {
        private val fileReferenceRegex = Regex("(\\S+\\.\\S{1,5})\\s*(?:\\(([^)]+)\\))?")

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("CodeBlock", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: CodeSnippetSource) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): CodeSnippetSource {
            val stringValue = decoder.decodeString()
            return when (val fileReferenceMatch = fileReferenceRegex.matchEntire(stringValue)) {
                null -> CodeSnippetSource.Text(stringValue)
                else -> {
                    val (fileName, keywordsString) = fileReferenceMatch.destructured
                    val keywords = keywordsString.trim()
                        .split("\\s*,\\s*".toRegex())
                        .filterNot { it.isEmpty() }
                        .toSet()
                    CodeSnippetSource.File(
                        file = fileName,
                        module = (keywords - "test").firstOrNull(),
                        test = "test" in keywords
                    )
                }
            }
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
