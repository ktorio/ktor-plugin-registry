/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import io.ktor.client.*
import io.ktor.client.call.body
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
private const val DEFAULT_CONFIGURATION_OPTION = "HOCON"

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

            require(kotlinVersion != null) { "Kotlin version must be specified" }
            require(ktorVersion != null) { "Ktor version must be specified" }

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
                        putJsonObject("build_system_args") {
                            put("version_catalog", true)
                        }
                        put("engine", engine)
                    }
                    put("configurationOption", configurationOption)

                    putJsonArray("features") {
                        features.forEach(::add)
                    }
                    putJsonArray("featureOverrides") {
                        addAll(featureOverrides)
                    }
                    put("addWrapper", addWrapper)
                }))
            }

            require(response.status.isSuccess()) {
                "Request to project generator failed with status ${response.status}" +
                        "\n    Body: ${response.bodyAsText()}"
            }

            val outputDir = Paths.get(outputDir)
            if (outputDir.exists())
                outputDir.deleteRecursively()

            Files.createDirectories(outputDir)
            ZipInputStream(ByteArrayInputStream(response.body())).use { zis ->
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
    var kotlinVersion: String? = null
    var features: List<String> = emptyList()
    var featureOverrides: List<JsonObject> = emptyList()
    var configurationOption: String = DEFAULT_CONFIGURATION_OPTION
    var addWrapper: Boolean = true
}
