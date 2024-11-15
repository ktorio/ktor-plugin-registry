/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.parseToYamlNode
import com.charleskorn.kaml.yamlMap
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream

fun Path.readYamlMap(): YamlMap? =
    takeIf { it.exists() }
        ?.inputStream()
        ?.use(Yaml.default::parseToYamlNode)
        ?.yamlMap
