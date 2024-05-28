/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.encodeToStream
import kotlinx.serialization.Serializable
import org.gradle.api.artifacts.ResolvedArtifact
import java.io.File
import java.nio.file.Path
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
                    else -> version.toString()
                }
            }
        }
    }

/**
 * Prints out artifacts required for each Ktor release for the registry builder.
 */
fun outputReleaseArtifacts(outputFile: Path, configurations: Map<String, Set<ResolvedArtifact>>) {

    val artifactsByRelease =
        configurations.mapValues { (_, artifacts) ->
            artifacts.associate { resolvedArtifact ->
                Pair(
                    resolvedArtifact.referenceString(),
                    resolvedArtifact.file.path
                )
            }
        }

    outputFile.outputStream().use { output ->
        Yaml.default.encodeToStream(artifactsByRelease, output)
    }
}

fun ResolvedArtifact.referenceString() =
    "${moduleVersion.id.group}:$name:${moduleVersion.id.version}"