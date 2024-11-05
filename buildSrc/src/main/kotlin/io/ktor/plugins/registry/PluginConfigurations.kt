/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.yamlScalar
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.associate
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.readLines
import kotlin.io.path.writeText

const val KTOR_MAVEN_REPO = "https://repo1.maven.org/maven2/io/ktor/ktor-server/maven-metadata.xml"
const val LOCAL_LIST = "build/ktor_releases"
val DEPRECATED_VERSIONS = setOf(1)
val moduleParents: Map<String, String> = mapOf(
    "client" to "core",
    "server" to "core",
)

/**
 * Retrieve all Ktor versions from maven, presented by client/server targets.
 * Cached in local text file build/ktor_releases.
 */
fun fetchKtorTargets(log: Logger, latestCount: Int = 2): List<KtorTarget> {
    val ktorVersions = readKtorVersionsFromFile(log)
        ?: fetchKtorVersionsFromMaven(latestCount, log).also(::writeToFile)
    return listOf("client", "server").map { name ->
        KtorTarget(name, ktorVersions.map { version ->
            KtorRelease("$name-$version", version)
        })
    }
}

fun collectPluginConfigs(
    log: Logger,
    latestCount: Int = 2,
    rootPath: String = ".",
): List<PluginConfiguration> {
    val ktorReleaseStrings = readKtorVersionsFromFile(log)
        ?: fetchKtorVersionsFromMaven(latestCount, log).also(::writeToFile)
    val ktorReleases = ktorReleaseStrings.map { ArtifactVersion.parse(it) }
    val versionProperties: Map<String, String> = Paths.get("$rootPath/plugins/gradle.properties").let { versionsFile ->
        Properties().apply {
            load(versionsFile.inputStream())
        }.entries.associate { (key, value) ->
            key.toString() to value.toString()
        }
    }
    return pluginConfigCombinations(rootPath, ktorReleases).flatMap { (type, release, pluginDir, modules) ->
        readPluginConfigs(
            type,
            modules,
            release,
            pluginDir,
            versionProperties
        )
    }.sortedBy { it.parent }.toList()
}

private fun pluginConfigCombinations(rootPath: String, releases: List<ArtifactVersion>): Sequence<PluginConfigurationStub> =
    sequence {
        for (type in listOf("server", "client"))
            for (pluginDir in folders("$rootPath/plugins/$type/*/*"))
                for (release in releases)
                    yield(PluginConfigurationStub(type, release, pluginDir, (pluginDir.moduleReferences() + type).distinct()))
    }

data class PluginConfigurationStub(
    val type: String,
    val release: ArtifactVersion,
    val pluginDir: Path,
    val modules: List<String>,
)

private fun readPluginConfigs(
    type: String,
    modules: List<String>,
    release: ArtifactVersion,
    pluginDir: Path,
    versionProperties: Map<String, String>
): Sequence<PluginConfiguration> {
    val pluginId = pluginDir.fileName.toString()
    val groupId = pluginDir.parent.fileName.toString()
    val artifactsMap = pluginDir.resolve(VERSIONS_FILE).readYamlMap()
        ?.readArtifacts(groupId, pluginId, versionProperties)
        .orEmpty()
    val (versionRange, artifacts) = artifactsMap.entries.lastOrNull {
        it.key.contains(release)
    } ?: return emptySequence()
    val pluginSourceDir = pluginDir
        .resolve(versionRange.toString().stripSpecialChars())
    val manifest = pluginSourceDir
        .resolve(MANIFEST_FILE).readYamlMap()
    val prerequisites = manifest?.get<YamlList>("prerequisites")?.items?.map {
        it.yamlScalar.content
    }.orEmpty()

    // recursively find all prerequisite artifacts
    val prerequisiteArtifactsByModule = prerequisites.flatMap { pluginId ->
        readPluginConfigs(
            type,
            modules,
            release,
            pluginDir.parent.resolve(pluginId),
            versionProperties
        )
    }.associate { it.module to it.artifacts }

    return modules.asSequence().map { module ->
        val prerequisiteArtifacts = prerequisiteArtifactsByModule[module] ?: emptyList()

        // resolve ktor version to the supplied release
        val allArtifacts = (artifacts + prerequisiteArtifacts).map {
            if (it.version is MatchKtor) it.copy(version = release) else it
        }

        val kotlinSourcePath =
            if (modules.size > 1)
                pluginSourceDir.resolve(module)
            else pluginSourceDir

        val parent = moduleParents[module]?.takeIf { it in modules }?.let { parent ->
            "$pluginId.${parent}.$release"
        }

        PluginConfiguration(
            kotlinSourcePath.toString(),
            pluginId,
            type,
            release.toString(),
            module,
            allArtifacts,
            parent,
        )
    }
}

private fun YamlMap.readArtifacts(
    groupId: String,
    pluginId: String,
    versionVariables: Map<String, String>
): Map<ArtifactVersion, Artifacts> =
    entries.entries.filterNot { (key) ->
        key.content.matches(Regex("[\\w-]+"))
    }.associate { (versionRangeKey, artifacts) ->
        val artifactReferences = readArtifacts(artifacts, groupId, versionVariables)
        val versionRange = try {
            ArtifactVersion.parse(versionRangeKey.content)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid version range $versionRangeKey in plugin $pluginId", e)
        }

        versionRange to artifactReferences
    }

private fun readKtorVersionsFromFile(log: Logger): List<String>? = try {
    Paths.get(LOCAL_LIST).takeIf { it.exists() }?.readLines()
} catch (e: Exception) {
    log.info("Local release list not found at $LOCAL_LIST, will fetch from maven")
    null
}

private fun writeToFile(versionsList: List<String>) =
    Paths.get(LOCAL_LIST).also {
        if (!it.parent.exists())
            it.parent.createDirectories()
    }.writeText(versionsList.joinToString("\n"))

internal fun fetchKtorVersionsFromMaven(
    latestCount: Int,
    log: Logger,
    source: Document = fetchAndParseXml(),
): List<String> {
    val groupedVersions = source.readVersionNumbers(log).groupByVersions()

    return groupedVersions
        .filterKeys { it !in DEPRECATED_VERSIONS }
        .filterByLatest(latestCount)
        .map(VersionNumber::number)
}

private fun fetchAndParseXml(location: String = KTOR_MAVEN_REPO): Document {
    val doc = URL(location).openStream().use { input ->
        val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
        val db = dbf.newDocumentBuilder()
        db.parse(input)
    }
    return doc
}

private fun Document.readVersionNumbers(log: Logger): Sequence<VersionNumber> = sequence {
    val versioning = getElementsByTagName("versioning").item(0)

    versioning.childNodes("versions").forEach { versionsList ->
        versionsList.childNodes("version").forEach { version ->
            try {
                yield(VersionNumber(version.textContent))
            } catch (e: Exception) {
                log.warn("Couldn't read version from maven metadata $version")
            }
        }
    }
}

private fun Node.childNodes(nodeName: String): Sequence<Node> = with(childNodes) {
    sequence {
        for (i in 0 until length)
            if (item(i).nodeName == nodeName)
                yield(item(i))
    }
}

private fun Sequence<VersionNumber>.groupByVersions(): GroupedVersions =
    groupBy { it.majorVersion }
        .mapValues { entry ->
            entry.value.groupBy { it.minorVersion }
        }

/**
 * Retains the latest count of minor and patch releases.
 */
private fun GroupedVersions.filterByLatest(latestCount: Int): MutableList<VersionNumber> {
    val filteredVersions = mutableListOf<VersionNumber>()
    values.asSequence().map { minorVersions ->
        minorVersions.values.toList().takeLast(latestCount)
    }.forEach { minorVersions ->
        minorVersions.forEach { versions ->
            val versionsRefined = if (versions.any { it.qualifier.isNullOrEmpty() })
                versions.filter { it.qualifier.isNullOrEmpty() }
            else versions

            filteredVersions.addAll(versionsRefined.takeLast(latestCount))
        }
    }
    return filteredVersions
}

typealias GroupedVersions = Map<Int, Map<Int, List<VersionNumber>>>

/**
 * Represents a build configuration for a given plugin.
 *
 * @property type       Either "server" or "client"
 * @property release    Ktor version string
 * @property pluginDir  Where the plugin is located
 * @property module     Either "server", "client", "shared", etc.
 */
@Serializable
data class PluginConfiguration(
    val path: String,
    val id: String,
    val type: String,
    val release: String,
    val module: String,
    val artifacts: Artifacts,
    val parent: String?
) {
    val pluginDir: Path get() = Paths.get(path).parent
    val name: String get() = "$id.$module.$release"
    val isDefaultModule get() = module == type

    override fun toString(): String = name
}

/**
 * Either server or client; holds release references which are used for gradle configs / plugin dependency management.
 */
data class KtorTarget(
    val name: String,
    val releases: List<KtorRelease>,
    val pluginsDir: Path = Paths.get("plugins/$name")
) {
    val releaseConfigs: List<String> get() = releases.map(KtorRelease::config)

    fun allArtifactsForVersion(version: String): Sequence<String> =
        pluginsDir.readPluginFiles(name == "client").allArtifactsForVersion(version)
}

/**
 * Holds the version number of a Ktor release.  The "config" field is the version with a target prefix.
 */
data class KtorRelease(val config: String, val version: String)

/**
 * Gets the latest configs by path
 */
fun List<PluginConfiguration>.latestByPath(): List<PluginConfiguration> =
    groupBy { it.path }.map { (_, configs) -> configs.maxBy { it.release } }