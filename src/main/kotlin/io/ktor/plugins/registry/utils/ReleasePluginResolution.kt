/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import io.ktor.plugins.registry.*
import io.ktor.plugins.registry.utils.CodeAnalysis.Companion.formatErrors
import io.ktor.plugins.registry.utils.FileUtils.listImages
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.InputStream
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.file.Path
import javax.xml.transform.Source
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.toPath

class ReleasePluginResolution private constructor(
    internal val release: KtorRelease,
    internal val releaseArtifacts: ReleaseArtifacts,
    private val codeAnalysis: CodeAnalysis,
    private val classLoader: URLClassLoader,
    private val pluginsDir: Path,
): Closeable by classLoader {

    companion object {
        fun withResolution(
            release: KtorRelease,
            releaseArtifacts: ReleaseArtifacts,
            pluginsDir: Path,
            resolution: ReleasePluginResolution.() -> Unit
        ) = ReleasePluginResolution(
            release = release,
            releaseArtifacts = releaseArtifacts,
            codeAnalysis = CodeAnalysis(releaseArtifacts.jars),
            classLoader = releaseArtifacts.newClassloader(),
            pluginsDir = pluginsDir
        ).resolution()
    }

    private val logger = LoggerFactory.getLogger("PluginResolutionContext")

    /**
     * Preference goes:
     * - prebuilt JSON
     * - local YAML
     * - YAML from artifacts
     */
    fun resolveManifest(plugin: PluginReference, assetsDir: Path): ResolvedPluginManifest? =
        resolvePrebuiltJson(plugin)
            ?: resolveYamlFile(plugin, assetsDir)
            ?: resolveYamlFromClasspath(plugin, assetsDir)

    private fun resolvePrebuiltJson(plugin: PluginReference): ResolvedPluginManifest? {
        return plugin.versionPath.resolve("manifest.json").ifExists()?.let { path ->
            logger.info("${plugin.identifier} resolved from JSON")
            PrebuiltJsonManifest(path)
        }
    }

    private fun resolveYamlFile(plugin: PluginReference, assetsDir: Path) =
        plugin.versionPath.resolve("manifest.ktor.yaml").ifExists()?.let { yamlPath ->
            val model: YamlManifest.ImportManifest =
                yamlPath.inputStream().use(Yaml.default::decodeFromStream)

            logger.info("${plugin.identifier} resolved from YAML")
            codeAnalysis.findErrors(plugin.versionPath).ifNotEmpty {
                logger.warn("Ignoring compilation errors #yolo: ${formatErrors(plugin.versionPath)}")
            }
            // TODO fix erroneous compilation errors since Ktor 3.0
            // codeAnalysis.findErrorsAndThrow(plugin.versionPath, plugin)

            YamlManifest(
                plugin = plugin,
                model = model,
                codeRefs = readAllCodeReferences(model, plugin.versionPath) {
                    plugin.readFileFromVersionPath(it)
                },
                documentationEntry = readDocumentation(model.documentation) {
                    plugin.readFileFromVersionPath(it)
                },
                logo = assetsDir.listImages(plugin.id).firstOrNull()
            )
        }

    private fun resolveYamlFromClasspath(plugin: PluginReference, assetsDir: Path): YamlManifest? {
        val yamlUrl = classLoader.getResource(plugin.manifestResourceFile) ?: return null
        val model: YamlManifest.ImportManifest = yamlUrl.openStream()
            ?.use(Yaml.default::decodeFromStream)
            ?: return null
        val resolvedManifestFolder = yamlUrl.toURI().toPath().parent

        logger.info("${plugin.identifier} resolved from classpath")
        codeAnalysis.findErrorsAndThrow(resolvedManifestFolder, plugin)

        return YamlManifest(
            plugin = plugin,
            model = model,
            codeRefs = readAllCodeReferences(model, plugin.versionPath) {
                plugin.readFileFromClasspath(it)
            },
            documentationEntry = readDocumentation(model.documentation) {
                plugin.readFileFromClasspath(it)
            },
            logo = assetsDir.listImages(plugin.id).firstOrNull()
        )
    }

    private fun readAllCodeReferences(
        model: YamlManifest.ImportManifest,
        versionPath: Path,
        findCodeInput: (String) -> InputStream?,
    ): List<CodeRef> {
        val codeInjections = model.installation.map { (site, installBlock) ->
            codeAnalysis.readCodeSnippet(versionPath, site, installBlock, findCodeInput)
        }
        val sourceFiles = model.sources.filterIsInstance<YamlManifest.CodeSnippetSource.File>().map { file ->
            codeAnalysis.readSourceFile(versionPath, file, findCodeInput = findCodeInput)
        }
        val resourceFiles = model.resources.filterIsInstance<YamlManifest.CodeSnippetSource.File>().map { template ->
            codeAnalysis.readSourceFile(versionPath, template, CodeInjectionSite.RESOURCES, findCodeInput)
        }

        return codeInjections + sourceFiles + resourceFiles
    }

    private val PluginReference.versionPath: Path
        get() =
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

    private fun readDocumentation(
        codeSnippet: YamlManifest.CodeSnippetSource,
        findCodeInput: (String) -> InputStream?
    ) = DocumentationExtractor.parseDocumentationMarkdown(when (codeSnippet) {
        is YamlManifest.CodeSnippetSource.Text -> codeSnippet.code
        is YamlManifest.CodeSnippetSource.File -> {
            findCodeInput(codeSnippet.file)?.use { it.readAllBytes().toString(Charset.defaultCharset()) }
                ?: throw IllegalArgumentException("Missing documentation file ${codeSnippet.file}")
        }
    })
}

private fun CodeAnalysis.readCodeSnippet(
    path: Path,
    site: String,
    codeSnippet: YamlManifest.CodeSnippetSource,
    findCodeInput: (String) -> InputStream?
): CodeRef {
    val injectionSite = CodeInjectionSite.valueOf(site.uppercase())
    val (code: String, filename: String?) = when (codeSnippet) {
        is YamlManifest.CodeSnippetSource.Text -> codeSnippet.code to null
        is YamlManifest.CodeSnippetSource.File -> {
            val code = findCodeInput(codeSnippet.file)?.use { it.readAllBytes().toString(Charset.defaultCharset()) }
                ?: throw IllegalArgumentException("Missing install snippet ${codeSnippet.file}")
            code to codeSnippet.file
        }
    }
    return parseInstallSnippet(
        sourceRoot = path,
        contents = code,
        meta = object : SourceCodeMeta {
            override val site = injectionSite
            override val file = filename
        }
    )
}

private fun CodeAnalysis.readSourceFile(
    path: Path,
    template: YamlManifest.CodeSnippetSource.File,
    site: CodeInjectionSite = CodeInjectionSite.SOURCE_FILE_KT,
    findCodeInput: (String) -> InputStream?,
): CodeRef {
    val code = findCodeInput(template.file)?.use { it.readAllBytes().toString(Charset.defaultCharset()) }
        ?: throw IllegalArgumentException("Missing source file ${template.file}")

    return parseInstallSnippet(
        sourceRoot = path,
        contents = code,
        meta = template.atSite(site)
    )
}

private fun YamlManifest.CodeSnippetSource.File.atSite(site: CodeInjectionSite): SourceCodeMeta =
    object: SourceCodeMeta {
        override val site = site
        override val file = this@atSite.file
        override val module = this@atSite.module
        override val test = this@atSite.test
    }
