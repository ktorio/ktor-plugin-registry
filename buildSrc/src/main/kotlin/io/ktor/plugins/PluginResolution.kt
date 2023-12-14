package io.ktor.plugins

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.encodeToStream
import kotlinx.serialization.Serializable
import org.gradle.api.artifacts.ResolvedArtifact
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.outputStream

/**
 * Get all artifact dependency strings for the given Ktor version. Used in gradle classpath resolution.
 */
fun Sequence<PluginReference>.allArtifactsForVersion(ktorRelease: String): Sequence<String> =
    flatMap { plugin ->
        plugin.allArtifactsForVersion(ktorRelease).map { artifact ->
            with(artifact) {
                "$group:$name:" + when(version) {
                    is MatchKtor -> ktorRelease
                    is VersionNumber -> version.number
                }
            }
        }
    }

/**
 * Prints out artifacts required for each Ktor release for the registry builder.
 */
fun outputReleaseArtifacts(outputDir: Path, configurations: Map<String, Set<ResolvedArtifact>>) {
    if (!outputDir.exists())
        Files.createDirectories(outputDir)

    val artifactsByRelease: Map<String, Map<String, @Serializable(with = FilePathSerializer::class) File>> =
        configurations.mapValues { (_, artifacts) ->
            artifacts.associate { resolvedArtifact ->
                Pair(
                    resolvedArtifact.referenceString(),
                    resolvedArtifact.file
                )
            }
        }
    outputDir.resolve("artifacts.yaml").outputStream().use { output ->
        Yaml.default.encodeToStream(artifactsByRelease, output)
    }
}

fun ResolvedArtifact.referenceString() =
    "${moduleVersion.id.group}:$name:${moduleVersion.id.version}"

//
//fun PluginReference.resolveJars(configurations: Map<String, Configuration>) =
//    configurations.entries.associate { (ktorRelease, dependencies) ->
//        ktorRelease to allArtifactsForVersion(ktorRelease).associateWith { artifact ->
//            artifact.resolveJar(ktorRelease, dependencies)
//                ?: throw IllegalStateException("Cannot resolve artifact $artifact for release $ktorRelease")
//        }
//    }
//
//fun ArtifactReference.resolveJar(ktorRelease: String, dependencies: Configuration): File? =
//    when(version) {
//        is MatchKtor -> dependencies.find(this.withVersion(ktorRelease)::accepts)
//        is VersionNumber -> dependencies.find(this::accepts)
//    }