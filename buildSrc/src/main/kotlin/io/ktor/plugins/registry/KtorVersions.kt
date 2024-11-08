/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import org.slf4j.Logger
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.net.URL
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

const val LOCAL_LIST = "build/ktor_releases"

/**
 * Referenced from build script - gets all Ktor releases, filtered to what we want in the generator.
 */
fun getKtorReleases(log: Logger, latestCount: Int = 2) =
    readKtorVersionsFromFile(log)
        ?: fetchKtorVersionsFromMaven(latestCount, log).also(::writeToFile)

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