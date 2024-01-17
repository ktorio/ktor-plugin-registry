/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginResolutionTest {

    @Test
    fun resolvePlugins() {
        val releases = listOf(
            "1.6.8",
            "2.3.7",
            "3.0.0-beta-1",
            "3.0.0-beta-2"
        ).map {
            KtorRelease("$it-config", it)
        }
        val target = KtorTarget("server", releases, pluginsDir = Paths.get("src/test/resources/plugins/server"))
        target.assertVersionYieldsArtifacts(
            "1.6.8",
            "io.ktor:ktor-auth:1.6.8"
        )
        target.assertVersionYieldsArtifacts(
            "2.3.7",
            "org.jetbrains.exposed:exposed-core:0.41.1",
            "org.jetbrains.exposed:exposed-jdbc:0.41.1",
            "com.h2database:h2:2.1.214",
            "io.ktor:ktor-server-auth-jvm:2.3.7",
        )
        target.assertVersionYieldsArtifacts(
            "3.0.0-beta-1",
            "org.jetbrains.exposed:exposed-core:0.41.1",
            "org.jetbrains.exposed:exposed-jdbc:0.41.1",
            "com.h2database:h2:2.1.214",
            "io.ktor:ktor-server-auth-jvm:3.0.0-beta-1",
        )
        target.assertVersionYieldsArtifacts(
            "3.0.0-beta-2",
            "org.jetbrains.exposed:exposed-core:0.41.1",
            "org.jetbrains.exposed:exposed-jdbc:0.41.1",
            "com.h2database:h2:2.1.214",
            "io.ktor:ktor-server-auth-jvm:3.0.0-beta-2",
            "io.ktor:ktor-server-csrf-jvm:3.0.0-beta-2"
        )

    }

    private fun KtorTarget.assertVersionYieldsArtifacts(version: String, vararg artifacts: String) {
        assertEquals(listOf(*artifacts), allArtifactsForVersion(version).toList())
    }

}