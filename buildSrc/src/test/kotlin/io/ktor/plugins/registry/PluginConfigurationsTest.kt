/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PluginConfigurationsTest {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PluginConfigurationsTest::class.java)
    }

    @Test
    fun `fails on invalid artifact reference`() {
        assertFailsWith<IllegalArgumentException> {
            collectPluginConfigs(logger, listOf("3.0.0"), rootPath = "src/test/resources") {
                it == "bad_semver"
            }
        }.also {
            assertEquals("Invalid artifact reference string \"what??\"", it.message)
        }
    }

    @Test
    fun `fails on duplicate plugin`() {
        assertFailsWith<IllegalArgumentException> {
            collectPluginConfigs(logger, listOf("3.0.0"), rootPath = "src/test/resources") {
                it == "dupe"
            }
        }.also {
            assertEquals(
                "Duplicate plugins found: " +
                        "src/test/resources/plugins/server/com.fail/dupe, " +
                        "src/test/resources/plugins/server/io.ktor/dupe",
                it.message
            )
        }
    }

    @Test
    fun `fails on missing prerequisite`() {
        assertFailsWith<IllegalArgumentException> {
            collectPluginConfigs(logger, listOf("3.0.0"), rootPath = "src/test/resources") {
                it == "missing_prerequisite"
            }
        }.also {
            assertEquals("Prerequisite plugin this_does_not_exist for missing_prerequisite not found", it.message)
        }
    }

    //**** handy tests for debugging ****//

    @Test
    fun collectsAllConfigs() {
        collectPluginConfigs(logger, listOf("3.0.0"), rootPath = "..").forEachIndexed { i, config ->
            println("$i ${config.name}: ${config.artifacts}")
        }
    }

    @Test
    fun latestByRange() {
        collectPluginConfigs(logger, listOf("3.0.0"), rootPath = "..").latestByPath().forEachIndexed { i, config ->
            println("$i ${config.name}${config.parent?.let { " -> $it" }.orEmpty()}: ${config.artifacts}")
        }
    }

}