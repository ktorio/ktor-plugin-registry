/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import com.charleskorn.kaml.YamlScalar
import io.ktor.plugins.registry.PluginGroup
import java.nio.file.Path
import kotlin.io.path.name

fun Path.readPluginGroup(): PluginGroup? =
    readYamlMap()?.let { yaml ->
        val (name, url, email, logo) = listOf("name", "url", "email", "logo").map {
            yaml.get<YamlScalar>(it)?.content
        }
        PluginGroup(parent.name, name, url, email, logo)
    }
