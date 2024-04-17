/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.outputStream

private const val DEFAULT_PROJECT_NAME = "io.ktor.test-project"
private const val DEFAULT_PROJECT_WEBSITE = "ktor.io"
private const val DEFAULT_BUILD_SYSTEM = "GRADLE_KTS"
private const val DEFAULT_ENGINE = "NETTY"
private const val DEFAULT_OUTPUT_DIR = "test-project"
private const val DEFAULT_KOTLIN_VERSION = "1.9.23" // TODO get from gradle

/**
 * Calls the project generator back-end to produce a new project in the provided
 * output folder.  Used to generate test projects for new plugins.
 */
class ProjectGeneratorClient(
    private val url: String,
    private val httpClient: HttpClient,
) {

    @OptIn(ExperimentalSerializationApi::class, ExperimentalPathApi::class)
    suspend fun generate(params: ProjectGeneratorRequestBuilder.() -> Unit) {
        ProjectGeneratorRequestBuilder().run {
            params()

            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Zip)
                accept(ContentType.Application.Json)

                setBody(Json.encodeToString(buildJsonObject {
                    putJsonObject("settings") {
                        put("project_name", projectName)
                        put("company_website", projectWebsite)
                        put("ktor_version", ktorVersion)
                        put("kotlin_version", kotlinVersion)
                        put("build_system", buildSystem)
                        put("engine", engine)
                    }
                    putJsonArray("features") {
                        features.forEach(::add)
                    }
                    putJsonArray("featureOverrides") {
                        addAll(featureOverrides)
                    }
                }))
            }

            check(response.status.isSuccess()) {
                "Request to project generator failed with status ${response.status}" +
                        "\n    Body: ${response.readBytes().decodeToString()}"
            }

            val outputDir = Paths.get(outputDir)
            if (outputDir.exists())
                outputDir.deleteRecursively()

            Files.createDirectories(outputDir)
            ZipInputStream(ByteArrayInputStream(response.readBytes())).use { zis ->
                for (entry in generateSequence { zis.nextEntry }) {
                    val outputFile = outputDir.resolve(entry.name)
                    when {
                        entry.isDirectory -> Files.createDirectories(outputFile)
                        else -> {
                            Files.createDirectories(outputFile.parent)
                            outputFile.outputStream().use { output ->
                                zis.copyTo(output)
                            }
                        }
                    }
                }
            }
        }

    }

}

class ProjectGeneratorRequestBuilder {
    var projectName: String = DEFAULT_PROJECT_NAME
    var projectWebsite: String = DEFAULT_PROJECT_WEBSITE
    var buildSystem: String = DEFAULT_BUILD_SYSTEM
    var engine: String = DEFAULT_ENGINE
    var outputDir: String = DEFAULT_OUTPUT_DIR
    var ktorVersion: String? = null
    var kotlinVersion: String = DEFAULT_KOTLIN_VERSION
    var features: List<String> = emptyList()
    var featureOverrides: List<JsonObject> = emptyList()
}
