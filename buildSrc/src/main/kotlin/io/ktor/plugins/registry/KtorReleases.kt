package io.ktor.plugins.registry

import org.gradle.kotlin.dsl.provideDelegate
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

const val KTOR_MAVEN_REPO = "https://repo1.maven.org/maven2/io/ktor/ktor-server/maven-metadata.xml"
const val LOCAL_LIST = "build/ktor_releases"

/**
 * Retrieve all Ktor versions from maven, presented by client/server targets.
 * Cached in local text file build/ktor_releases.
 */
fun fetchKtorTargets(): List<KtorTarget> {
    val ktorVersions = readKtorVersionsFromFile()
        ?: fetchKtorVersionsFromMaven().also(::writeToFile)
    return listOf("client", "server").map { name ->
        KtorTarget(name, ktorVersions.map { version ->
            KtorRelease("$name-$version", version)
        })
    }
}

private fun readKtorVersionsFromFile(): List<String>? = try {
    Paths.get(LOCAL_LIST).takeIf { it.exists() }?.readLines()
} catch (e: Exception) {
    null
}

private fun writeToFile(versionsList: List<String>) =
    Paths.get(LOCAL_LIST).also {
        if (!it.parent.exists())
            it.parent.createDirectories()
    }.writeText(versionsList.joinToString("\n"))

private fun fetchKtorVersionsFromMaven(): List<String> {
    val allVersions = buildList {
        URL(KTOR_MAVEN_REPO).openStream().use {
            val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(it)

            val versioning = doc.getElementsByTagName("versioning").item(0)
            val versions = versioning.childNodes

            for (i in 0 until (versions.length)) {
                if (versions.item(i).nodeName == "versions") {
                    val versionList = versions.item(i).childNodes
                    for (j in 0 until versionList.length) {
                        if (versionList.item(j).nodeName == "version") {
                            add(versionList.item(j).textContent)
                        }
                    }
                }
            }
        }
    }
    return allVersions.filterIndexed { i, version ->
        // only return latest of each minor version
        val minorVersion = version.minorVersion
        minorVersion !in obsoleteReleases
                && (i + 1 >= allVersions.size || allVersions[i + 1].minorVersion != minorVersion)
    }
}

private val obsoleteReleases = setOf("1.0", "1.1", "1.2", "1.3")
private val minorVersionRegex = Regex("""(\d+\.\d+)\..*""")

private val String.minorVersion get() =
    minorVersionRegex.matchEntire(this)?.groups?.get(1)?.value ?: ""

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
        pluginsDir.readPluginFiles().allArtifactsForVersion(version)
}

/**
 * Holds the version number of a Ktor release.  The "config" field is the version with a target prefix.
 */
data class KtorRelease(val config: String, val version: String)