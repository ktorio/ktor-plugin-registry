package io.ktor.plugins

import com.charleskorn.kaml.Yaml
import com.vdurmont.semver4j.Semver
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@OptIn(
    ExperimentalSerializationApi::class,
    ExperimentalPathApi::class
)
class RegistryBuilder(
    private val logger: KLogger = KotlinLogging.logger("RegistryBuilder"),
    private val yaml: Yaml = Yaml.default,
    private val json: Json = Json { prettyPrint = true }
) {
    fun buildRegistry(pluginsDir: Path, buildDir: Path) {
        val artifactsFile = buildDir.resolve("artifacts.yaml")
        val outputDir = buildDir.resolve("registry")
        val manifests = outputDir.resolve("manifests")
        if (!artifactsFile.exists())
            throw PluginsUnresolvedException()
        else {
            outputDir.apply {
                deleteRecursively()
                createDirectories()
                manifests.createDirectory()
            }
        }

        // Step 1: resolve the manifest for each plugin version
        for (plugin in pluginsDir.readPluginFiles()) {
            for ((version, _) in plugin.versions) {
                val precompiledManifest = pluginsDir.resolve("${plugin.group}/${plugin.id}/manifest$version.json")
                when {
                    precompiledManifest.exists() -> Files.copy(
                        precompiledManifest,
                        manifests.resolve(plugin.formatManifestName(version))
                    )
                    else -> {
                        // TODO Resolve manifest from library using reflection
                    }
                }
            }
        }

        // Step 2: generate mapping for all known ktor releases
        resolveKtorReleases().apply {
            resolvePluginVersions(pluginsDir)
            outputReleaseMappings(outputDir)
        }
    }

    private fun resolveKtorReleases() =
        Paths.get("ktor_releases").readLines().map(::KtorRelease)

    private fun List<KtorRelease>.resolvePluginVersions(pluginsDir: Path) {
        for (plugin in pluginsDir.readPluginFiles()) {
            try {
                val distributions = mapNotNull { release ->
                    release.pickVersion(plugin)?.let {
                        "${release.version}: $it"
                    }
                }
                logger.info { "Plugin ${plugin.id}\n\t${distributions.joinToString("\n\t")}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to process plugin ${plugin.id}!" }
            }
        }
    }

    private fun List<KtorRelease>.outputReleaseMappings(distDir: Path) {
        distDir.resolve("features.json").outputStream().use { output ->
            json.encodeToStream(associate { (ktorRelease, plugins) ->
                ktorRelease.toString() to plugins.map { it.manifest }
            }, output)
        }
    }

}

//logger.info { "Plugin ${plugin.id}" }
//for (artifact in plugin.artifacts) {
//    logger.info { "\tArtifact ${artifact.name}:${artifact.version} in ${artifact.jarFile?.name}" }
//    artifact.withResources(plugin) {
//        val scanner = Reflections(
//            ConfigurationBuilder.build()
//                .addUrls(ClasspathHelper.forPackage(plugin.group, classLoader))
//        )
//        scanner.getTypesAnnotatedWith(Deprecated::class.java).take(5).forEach { clazz ->
//            logger.info { "\t\tDeprecated: " + clazz.simpleName }
//        }
//    }
//}

data class KtorRelease(
    val version: Semver,
    val plugins: MutableList<PluginReference> = mutableListOf(),
) {
    constructor(versionString: String) : this(Semver(versionString, Semver.SemverType.NPM))

    /**
     * Selects the first plugin version that satisfies this release and includes it in the release selection.
     */
    fun pickVersion(plugin: PluginReference): String? {
        return plugin.versions.keys.firstOrNull(version::satisfies)?.also { foundVersion ->
            plugins.add(plugin.copy(versions = mapOf(foundVersion to plugin.versions[foundVersion]!!)))
        }
    }
}

//data class ArtifactResourcesContext(
//    val classLoader: URLClassLoader,
//    val reflections: Reflections,
//)

//fun ArtifactReference.withResources(plugin: PluginReference, op: ArtifactResourcesContext.() -> Unit) {
//    URLClassLoader(arrayOf(jarFile!!.toURL())).use { classLoader ->
//        ArtifactResourcesContext(
//            classLoader,
//            Reflections(
//                ConfigurationBuilder.build()
//                    .addUrls(ClasspathHelper.forPackage(plugin.group, classLoader))
//            )
//        ).apply(op)
//    }
//}

class PluginsUnresolvedException : IllegalArgumentException("Run resolvePlugins gradle task BEFORE executing")