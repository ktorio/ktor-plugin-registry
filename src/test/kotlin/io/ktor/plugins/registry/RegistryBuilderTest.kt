/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull
import kotlin.test.Test
import kotlin.test.*

class RegistryBuilderTest {

    private val registryBuilder = RegistryBuilder()
    private val testResources = Paths.get("src/test/resources")
    private val buildDir by lazy {
        Files.createTempDirectory("build").also { buildDir ->
            Files.copy(
                Paths.get("build/server-artifacts.yaml"),
                buildDir.resolve("server-artifacts.yaml"),
            )
            Files.copy(
                testResources.resolve("resolved/ktor_releases"),
                buildDir.resolve("ktor_releases"),
            )
        }
    }
    private val target = "server"

    @Test
    fun `happy path`() {
        buildRegistry {
            it == "csrf"
        }
    }

    @Test
    fun `fails on duplicate key`() {
        assertRegistryFailure("Duplicate plugin ID \"dupe\"") {
            buildRegistry {
                it == "dupe"
            }
        }
    }

    @Test
    fun `bad version range string`() {
        assertRegistryFailure("Failed to parse versions.ktor.yaml for plugin \"bad_semver\"") {
            buildRegistry {
                it == "bad_semver"
            }
        }
    }

    @Test
    fun `fails on missing install files`() {
        assertRegistryFailure("Missing install snippet install.kt") {
            buildRegistry {
                it == "no_install"
            }
        }
    }

    @Test
    fun `fails on missing markdown files`() {
        assertRegistryFailure("Missing documentation file documentation.md") {
            buildRegistry {
                it == "no_doc"
            }
        }
    }

    @Test
    fun `fails on missing group details`() {
        assertRegistryFailure("Missing group.ktor.yaml for plugin test") {
            buildRegistry {
                it == "test"
            }
        }
    }

    @Test
    fun `fails on missing fields`() {
        assertRegistryFailure("Property 'name' is required but it is missing.") {
            clonePlugin("csrf").substitute("name" to null).build()
        }
        assertRegistryFailure("Property 'name' requires a value") {
            clonePlugin("csrf").substitute("name" to "\"\"").build()
        }
        assertRegistryFailure("Property 'description' requires a value") {
            clonePlugin("csrf").substitute("description" to "\"\"").build()
        }
        assertRegistryFailure("Property 'license' requires a value") {
            clonePlugin("csrf").substitute("license" to "\"\"").build()
        }
    }

    @Test
    fun `fails on incorrect category`() {
        assertRegistryFailure(
            "Property 'category' must be one of " +
                "[Administration, Databases, HTTP, Monitoring, Routing, Security, Serialization, Sockets, Templating]"
        ) {
            clonePlugin("csrf")
                .substitute("category" to "Some wrong value")
                .build()
        }
    }

    @Test
    fun `fails on invalid vcs link`() {
        assertRegistryFailure("Invalid VCS link \"not a url\"") {
            clonePlugin("csrf")
                .substitute("vcsLink" to "not a url")
                .build()
        }
    }

    @Test
    fun `fails on install compilation error`() {
        assertRegistryFailure("""
            Could not read install function:
            ${resourceContents("/server/com.fail/bad_kt/2.3.7/install.kt")}
        """.trimIndent()) {
            buildRegistry {
                it == "bad_kt"
            }
        }
    }

    @Test
    fun `fails on unknown types`() {
        assertRegistryFailure("Unresolved reference: does (install.kt:1)") {
            buildRegistry {
                it == "missing_import"
            }
        }
    }

    private fun assertRegistryFailure(message: String, block: () -> Unit) {
        val ex = assertFailsWith<IllegalArgumentException>(message = "Expected failure", block = block)
        assertEquals(message, ex.message)
    }

    private fun buildRegistry(dir: Path = testResources, filter: (String) -> Boolean = { true }) {
        registryBuilder.buildRegistry(dir, buildDir, target, filter)
    }

    private fun clonePlugin(id: String): PluginTestContext =
        PluginTestContext(id)


    private fun resourceContents(resource: String): String =
        this::class.java.getResourceAsStream(resource)?.use {
            it.readAllBytes().toString(Charset.defaultCharset())
        } ?: throw IllegalArgumentException("No resource found for $resource")

    inner class PluginTestContext(private val id: String) {
        private val replacements = mutableMapOf<String, String?>()

        fun substitute(assignment: Pair<String, String?>): PluginTestContext {
            replacements += assignment
            return this
        }

        fun build() {
            val pluginDir = Files.walk(testResources).filter { it.name == id }.findFirst().getOrNull()
            require(pluginDir != null) { "Could not find plugin $id" }
            val groupDir = pluginDir.parent
            val groupYaml = groupDir.resolve("group.ktor.yaml")
            val tempDir = Files.createTempDirectory("cloned")
            val newGroupDir = tempDir.resolve("$target/${groupDir.name}")
            val newPluginDir = newGroupDir.resolve(pluginDir.name)
            val yamlPropertyRegex = { key: String ->
                Regex("""(?<=^|\n)$key:.*?(?=\n[a-z]+:)""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
            }

            // copy with manifest field replacement
            newGroupDir.createDirectories()
            Files.copy(groupYaml, newGroupDir.resolve("group.ktor.yaml"))
            Files.walk(pluginDir).forEach { source: Path ->
                val destination = newPluginDir.resolve(pluginDir.relativize(source))
                when {
                    source.isDirectory() -> destination.createDirectories()
                    source.name == "manifest.ktor.yaml" -> {
                        var yaml = source.readText()
                        for ((key, value) in replacements) {
                            yaml = when (value) {
                                null -> yamlPropertyRegex(key).replace(yaml, "")
                                else -> yamlPropertyRegex(key).replace(yaml, "$key: $value")
                            }
                        }
                        destination.writeText(yaml)
                    }
                    else -> Files.copy(source, destination)
                }
            }

            buildRegistry(dir = tempDir)
        }
    }
}
