package io.ktor.plugins

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.vdurmont.semver4j.Semver
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readLines

// TODO
//   remove and report invalid artifacts
//   populate all metadata using manifests inside jars

fun main() {
    val pluginsDir = Paths.get("build/plugins")
    if (!pluginsDir.exists()) {
        throw IllegalStateException("""
            Plugin dependencies not resolved yet!
            Run resolvePlugins gradle task BEFORE executing.
        """)
    }

    val ktorReleases: Map<Semver, MutableSet<PluginReference>> = Paths.get("ktor_releases").readLines().associate { release ->
        Semver(release, Semver.SemverType.NPM) to mutableSetOf()
    }

    for (pluginFile in pluginsDir.listDirectoryEntries()) {
        val plugin = pluginFile.inputStream().use { input ->
            Yaml.default.decodeFromStream<PluginReference>(input)
        }
        println("Plugin ${plugin.id}")
        for ((_, artifact) in plugin.artifacts) {
            println("\tArtifact ${artifact.name}:${artifact.version} in ${artifact.jarFile?.name}")
            artifact.classLoader().use { classLoader ->
                val scanner = Reflections(
                    ConfigurationBuilder.build()
                        .addUrls(ClasspathHelper.forPackage(plugin.group, classLoader))
                )
                scanner.getTypesAnnotatedWith(Deprecated::class.java).take(5).forEach { clazz ->
                    println("\t\tDeprecated: " + clazz.simpleName)
                }
            }
        }
        for ((ktorRelease, plugins) in ktorReleases) {
            plugin.copy(
                artifacts = plugin.artifacts.filter { (range, _) ->
                    ktorRelease.satisfies(range)
                }
            ).takeIf { it.artifacts.isNotEmpty() }?.let {
                plugins.add(it)
            }
        }
    }

    for ((ktorRelease, plugins) in ktorReleases) {
        println("\n${ktorRelease.originalValue}")
        if (plugins.isEmpty())
            println("\t(none)")
        for (plugin in plugins)
            println("\t${plugin.id} { ${plugin.artifacts.values.joinToString { "${it.name}:${it.actualVersion()}" }} }")
    }
}

fun ArtifactReference.classLoader() = URLClassLoader(arrayOf(jarFile!!.toURL()))