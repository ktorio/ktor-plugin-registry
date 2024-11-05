/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.Test

class PluginConfigurationsTest {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PluginConfigurationsTest::class.java)
    }

    @Test
    fun collectsAllConfigs() {
        collectPluginConfigs(logger, rootPath = "..").forEachIndexed { i, config ->
            println("$i ${config.name}: ${config.artifacts}")
        }
    }

    @Test
    fun latestByRange() {
        collectPluginConfigs(logger, rootPath = "..").latestByPath().forEachIndexed { i, config ->
            println("$i ${config.name}${config.parent?.let { " -> $it" }.orEmpty()}: ${config.artifacts}")
        }
    }

}