/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import kotlinx.serialization.Serializable

@Serializable
data class ResolvedArtifact(
    val version: String,
    val path: String,
)

fun Map<String, ResolvedArtifact>.find(artifact: ArtifactReference, group: PluginGroup) =
    get(artifact.groupAndName(group)) ?: get(artifact.groupAndName(group) + "-jvm")

fun ArtifactReference.resolve(resolvedArtifact: ResolvedArtifact?): ArtifactReference =
    resolvedArtifact?.let {
        resolve(ArtifactVersion.parse(it.version))
    } ?: this