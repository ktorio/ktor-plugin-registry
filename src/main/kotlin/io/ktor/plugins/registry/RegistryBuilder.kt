/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import io.ktor.plugins.registry.utils.*
import io.ktor.plugins.registry.utils.Files.ifExists
import io.ktor.plugins.registry.utils.Files.listImages
import io.ktor.plugins.registry.utils.Files.listSubDirectories
import io.ktor.plugins.registry.utils.Terminal.ktorScriptHeader
import io.ktor.utils.io.core.use
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@OptIn(
    ExperimentalSerializationApi::class,
    ExperimentalPathApi::class
)
class RegistryBuilder(
    private val logger: Logger = LoggerFactory.getLogger("RegistryBuilder"),
    private val yaml: Yaml = Yaml.default,
    private val json: Json = Json { prettyPrint = true }
) {

    /**
     * Outputs registry files for use in the project generator, using artifacts from the `resolvePlugins` task.
     */
    fun buildRegistries(
        pluginsRoot: Path,
        buildDir: Path,
        assetsDir: Path,
        filterPluginIds: (String) -> Boolean = { true },
    ) {
        logger.info(ktorScriptHeader())

        logger.info("Reading resolved plugin configurations...")
        val ktorReleasesFile = buildDir.resolve("ktor_releases")
        val pluginConfigsFile = buildDir.resolve("plugins/configurations.yaml")
        require(ktorReleasesFile.exists()) { "Release list file $ktorReleasesFile does not exist" }
        require(pluginConfigsFile.exists()) { "Plugin resolutions file $pluginConfigsFile does not exist" }

        val allPluginConfigs = yaml.decodeFromStream<List<PluginConfiguration>>(pluginConfigsFile.inputStream())
            .asSequence()
            .filter { filterPluginIds(it.id) }
            .map { it.copy(path = pluginsRoot.resolve(it.path).toString()) }
            .toList()
        val versionProperties = pluginsRoot.readVersionProperties()
        logger.info("Reading groups...")
        val groups = allPluginConfigs.asSequence().distinctBy { it.groupId }.associate {
            it.groupId to it.groupFile.readPluginGroup()
        }

        for ((type, pluginsForType) in allPluginConfigs.groupBy { it.type }) {
            val pluginsDir = pluginsRoot.resolve(type)
            if (!pluginsDir.exists()) {
                logger.warn("Plugins directory ${pluginsDir.absolute()} does not exist; continuing")
                continue
            }
            val outputDir = buildDir.resolve("registry").resolve(type)
            val manifestsDir = outputDir.resolve("manifests")
            logger.info("Starting registry processing for the '$type' type...")
            logger.info("Cleaning output dir $outputDir...")
            outputDir.apply {
                deleteRecursively()
                createDirectories()
                manifestsDir.createDirectory()
            }
            logger.info("Moving assets...")
            processAssets(pluginsDir, assetsDir)

            logger.info("Writing manifest mappings...")
            outputDir.resolve("features.json").outputStream().use { out ->
                json.encodeToStream(
                    pluginsForType.distinctBy {
                        it.pluginAndRelease
                    }.groupBy({ it.release }) {
                        it.manifestOutputFile
                    },
                    out
                )
            }

            for ((pluginAndRelease, pluginConfigs) in pluginsForType.groupBy { it.pluginAndRelease }) {
                logger.debug("Exporting $pluginAndRelease...")

                // merge module config artifacts
                val plugin = pluginConfigs.firstOrNull { type == it.module.name }
                    ?.copy(
                        artifacts = pluginConfigs.flatMap { moduleConfig ->
                            moduleConfig.artifacts.map { artifact ->
                                artifact.copy(module = moduleConfig.module)
                            }
                        },
                    )
                require(plugin != null) { "Expected default module $type for plugin $plugin" }
                val manifestOutput = manifestsDir.resolve(plugin.manifestOutputFile)
                val classPathFile = buildDir.resolve("plugins/classpaths/${plugin.id}.${plugin.release}.yaml")
                val artifacts = classPathFile.ifExists()?.inputStream()?.use {
                    yaml.decodeFromStream<Map<String, ResolvedArtifact>>(it)
                }.orEmpty()
                val group = groups[plugin.groupId]
                val modules = pluginConfigs.map { it.module }
                require(group != null) { "Missing group.ktor.yaml for plugin ${plugin.id}" }

                try {
                    plugin.resolveManifest(
                        group,
                        modules,
                        artifacts,
                        assetsDir,
                        versionProperties,
                    ).apply {
                        validate()
                        export(manifestOutput, json)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to process manifest for plugin ${plugin.groupId}/${plugin.id}")
                    throw e
                }
            }

            logger.info(buildString {
                append("Registry built for $type including plugins: ")
                append(pluginsForType.map { it.id }.sorted().joinToString())
            })
        }
    }
}

val PluginConfiguration.manifestOutputFile: String get() = "$id-$range.json"
val PluginConfiguration.pluginAndRelease get() = "$id@$release"
val PluginGroup.outputLogo get() = logo?.let { id + '.' + it.substringAfterLast('.') }

private fun processAssets(pluginsDir: Path, assetsDir: Path) {
    if (!assetsDir.exists())
        assetsDir.createDirectories()

    for (groupFolder in pluginsDir.listSubDirectories()) {
        for (pluginFolder in groupFolder.listSubDirectories()) {
            val pluginLogoFile = pluginFolder.listImages().firstOrNull() ?: continue
            val dest = assetsDir.resolve(
                pluginFolder.name + '.' + pluginLogoFile.name.substringAfterLast('.')
            )
            pluginLogoFile.copyTo(dest, overwrite = true)
        }
        val group = groupFolder.resolve(GROUP_FILE).readPluginGroup() ?: continue
        val logo = group.logo ?: continue
        val logoFile = groupFolder.resolve(logo).takeIf { it.exists() } ?: continue
        logoFile.copyTo(assetsDir.resolve(group.outputLogo!!), overwrite = true)
    }
}

private fun PluginConfiguration.resolveManifest(
    group: PluginGroup,
    modules: Collection<ProjectModule>,
    resolvedArtifacts: Map<String, ResolvedArtifact>,
    assetsDir: Path,
    versionProperties: Map<String, String>,
): PluginManifestData {
    val model: PluginManifestData.ImportManifest =
        manifestFile.inputStream().use(Yaml.default::decodeFromStream)
    val sourcesDir = paths.resolved
    val readSourceFile: (String) -> InputStream? = { file ->
        sourcesDir.resolve(file).ifExists()?.inputStream()
    }
    val codeAnalysis = CodeAnalysis(resolvedArtifacts.values.map { Paths.get(it.path) })
    val codeInjections = model.installation.map { (site, installBlock) ->
        codeAnalysis.readCodeSnippet(sourcesDir, site, installBlock, readSourceFile)
    }
    val sourceFiles = model.sources.filterIsInstance<PluginManifestData.CodeSnippetSource.File>().map { file ->
        codeAnalysis.readSourceFile(sourcesDir, file, findCodeInput = readSourceFile)
    }
    val resourceFiles = model.resources.filterIsInstance<PluginManifestData.CodeSnippetSource.File>().map { template ->
        codeAnalysis.readSourceFile(sourcesDir, template, CodeInjectionSite.RESOURCES, readSourceFile)
    }
    val documentationEntry = readDocumentation(model.documentation) { file ->
        paths.resolved.resolve(file).ifExists()?.inputStream()
    }
    val pluginConfig = copy(
        artifacts = artifacts.map { artifact ->
            artifact.resolve(resolvedArtifacts.find(artifact, group))
        }
    )

    return PluginManifestData(
        plugin = pluginConfig,
        group = group,
        model = model,
        modules = modules,
        codeRefs = codeInjections + sourceFiles + resourceFiles,
        documentationEntry = documentationEntry,
        logo = assetsDir.listImages(id).firstOrNull(),
        versionProperties = versionProperties,
    )
}

private fun CodeAnalysis.readCodeSnippet(
    path: Path,
    site: String,
    codeSnippet: PluginManifestData.CodeSnippetSource,
    findCodeInput: (String) -> InputStream?
): CodeRef {
    val injectionSite = CodeInjectionSite.valueOf(site.uppercase())
    val (code: String, filename: String?) = when (codeSnippet) {
        is PluginManifestData.CodeSnippetSource.Text -> codeSnippet.code to null
        is PluginManifestData.CodeSnippetSource.File -> {
            // check for file under module dir first
            val siteModule = injectionSite.module ?: "server"
            val codeInput = findCodeInput("$siteModule/${codeSnippet.file}")
                ?: findCodeInput(codeSnippet.file)
            require(codeInput != null) { "Code source could not be found at $path/${codeSnippet.file}" }
            val code = codeInput.use {
                it.readAllBytes().toString(Charset.defaultCharset())
            }
            code to codeSnippet.file
        }
    }
    return parseInstallSnippet(
        contents = code,
        meta = object : SourceCodeMeta {
            override val site = injectionSite
            override val file = filename
        }
    )
}

private fun CodeAnalysis.readSourceFile(
    pluginPath: Path,
    template: PluginManifestData.CodeSnippetSource.File,
    site: CodeInjectionSite = CodeInjectionSite.SOURCE_FILE_KT,
    findCodeInput: (String) -> InputStream?,
): CodeRef {
    val file = template.module?.let { "${template.module}/${template.file}" } ?: template.file
    val code = findCodeInput(file)?.use { it.readAllBytes().toString(Charset.defaultCharset()) }
        ?: throw IllegalArgumentException("Missing source file $pluginPath/$file")

    return parseInstallSnippet(
        contents = code,
        meta = template.atSite(site)
    )
}

private fun PluginManifestData.CodeSnippetSource.File.atSite(site: CodeInjectionSite): SourceCodeMeta =
    object: SourceCodeMeta {
        override val site = site
        override val file = this@atSite.file
        override val module = this@atSite.module
        override val test = this@atSite.test
    }

private fun readDocumentation(
    codeSnippet: PluginManifestData.CodeSnippetSource,
    findCodeInput: (String) -> InputStream?
) = DocumentationExtractor.parseDocumentationMarkdown(when (codeSnippet) {
    is PluginManifestData.CodeSnippetSource.Text -> codeSnippet.code
    is PluginManifestData.CodeSnippetSource.File -> {
        findCodeInput(codeSnippet.file)?.use { it.readAllBytes().toString(Charset.defaultCharset()) }
            ?: throw IllegalArgumentException("Missing documentation file ${codeSnippet.file}")
    }
})
