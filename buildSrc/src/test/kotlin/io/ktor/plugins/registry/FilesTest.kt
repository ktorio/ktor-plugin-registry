/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.yamlScalar
import org.gradle.internal.impldep.junit.framework.TestCase.assertTrue
import kotlin.io.path.exists
import kotlin.test.Test

class FilesTest {

    @Test
    fun listFolders() {
        val result = folders("../plugins/*/io.ktor/*/2.0").map { it.toString() }
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("../plugins/server/io.ktor/resources/2.0"))
    }

    @Test
    fun migrateVersionProps() {
        folders("../plugins/*/*/*").mapNotNull {
            it.resolve(VERSIONS_FILE).takeIf { it.exists() }
        }.flatMap { versionsFile ->
            versionsFile.readYamlMap()?.let { yamlMap ->
                yamlMap.entries.asSequence()
                    .filter { (key) -> key.content.matches(Regex("[\\w-]+")) }
                    .map { (key, value) -> "${key.content}=${value.yamlScalar.content}" }
            }.orEmpty()
        }.distinctBy {
            it.substringBefore('=')
        }.forEach { entry ->
            println(entry)
        }
    }

}