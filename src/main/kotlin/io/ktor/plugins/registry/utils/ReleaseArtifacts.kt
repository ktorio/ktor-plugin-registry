/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import io.ktor.plugins.registry.*
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths

@JvmInline
value class ReleaseArtifactsMapping(private val artifactsByRelease: Map<String, Map<String, String>>) {
    operator fun get(release: KtorRelease): ReleaseArtifacts {
        val artifacts = artifactsByRelease[release.versionString]
        check(artifacts != null) { "No artifacts found for ${release.versionString}!" }
        return ReleaseArtifacts(artifacts)
    }
}

data class ReleaseArtifacts(
    val references: List<ArtifactReference>,
    val jars: List<Path>,
) {
    constructor(releaseArtifacts: Map<String, String>) : this(
        references = releaseArtifacts.keys.map(ArtifactReference.Companion::parse),
        jars = releaseArtifacts.values.map(Paths::get)
    )

    fun resolveActualVersion(artifact: ArtifactReference): ArtifactReference {
        val versionRange = artifact.version.asRange() ?: return artifact
        val artifactNameRegex = Regex(Regex.escape(artifact.name) + "(?:-jvm)?", RegexOption.IGNORE_CASE)
        val actualArtifact = references.find {
            it.group == artifact.group && artifactNameRegex.matches(it.name)
        }
        check(actualArtifact != null) {
            "Could not find actual version for $artifact"
        }
        val actualVersion = actualArtifact.version
        check(actualVersion is VersionNumber) {
            "Unexpected version during resolution $actualVersion"
        }
        check(versionRange.contains(actualVersion)) {
            "Resolved version $actualVersion is not in ${artifact.version} for $artifact"
        }

        return artifact.copy(
            name = actualArtifact.name,
            version = versionRange.resolve(actualVersion)
        )
    }

    fun newClassloader(): URLClassLoader =
        URLClassLoader(jars.map { it.toUri().toURL() }.toTypedArray())
}
