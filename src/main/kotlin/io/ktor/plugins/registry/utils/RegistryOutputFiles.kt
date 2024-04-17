/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.readLines

@OptIn(ExperimentalSerializationApi::class)
class RegistryOutputFiles(private val buildDir: Path, val target: String) {

    constructor(buildDir: String, target: String = "server"): this(Paths.get(buildDir), target)

    private val registryDir: Path = buildDir.resolve("registry/server")
    private val manifestsDir: Path = registryDir.resolve("manifests")
    private val featuresByVersionMap: Map<String, List<String>> by lazy {
        registryDir.resolve("features.json").inputStream().use { input ->
            Json.decodeFromStream(input)
        }
    }
    val ktorReleases: List<String> by lazy {
        buildDir.resolve("ktor_releases").readLines().filter { it.isNotBlank() }
    }

    init {
        check(buildDir.exists()) { "Build directory does not exist: $buildDir; try running buildRegistry" }
        check(registryDir.exists()) { "Registry directory does not exist: $registryDir; try running buildRegistry" }
    }

    fun featuresForVersion(ktorRelease: String) =
        featuresByVersionMap[ktorRelease] ?: throw MissingKtorVersionException(ktorRelease)

    fun readManifest(ktorRelease: String, pluginId: String): JsonObject =
        featuresForVersion(ktorRelease).filter {
            it.startsWith("$pluginId-")
        }.minByOrNull { it.length }
            ?.let { manifestFileName ->
                manifestsDir.resolve(manifestFileName).inputStream().use { input ->
                    Json.decodeFromStream<JsonObject>(input)
                }
            } ?: throw MissingManifestException(ktorRelease, pluginId)

}

class MissingKtorVersionException(version: String) : IllegalStateException(
        "$version not found in built registry files; it may be out-of-date - try running buildRegistry again"
)

class MissingManifestException(version: String, pluginId: String) : IllegalStateException(
    "$pluginId plugin at ktor version $version not found in built registry files"
)
