/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Represents a build configuration for a given plugin.
 *
 * @property path       Where the sources are located for the module
 * @property id         The plugin ID (i.e., auth-basic)
 * @property type       Either "server" or "client"
 * @property release    Ktor version string
 * @property module     Either "server", "client", "shared", etc.
 * @property range      The version range matched for this release
 * @property artifacts  The required artifacts for building this plugin
 * @property parent     The parent module (i.e., server -> core)
 */
@Serializable
data class PluginConfiguration(
    val path: String,
    val id: String,
    val type: String,
    val release: String,
    val module: String,
    val range: String,
    val artifacts: Artifacts,
    val parent: String?
) {
    val name: String get() = "$id.$module.$release"
    val paths: PluginConfigurationPaths get() =
        PluginConfigurationPaths.get(path, module)
    val groupId: String get() = paths.group.fileName.toString()

    val groupFile: Path get() = paths.group.resolve("group.ktor.yaml")
    val manifestFile: Path get() = paths.resolved.resolve("manifest.ktor.yaml")

    override fun toString(): String = name
}

/**
 * Follows a directory structure like:
 *   plugins/server/io.ktor/plugin/3.0/module
 *
 * Where the "module" segment is optional for single-module plugins.
 */
interface PluginConfigurationPaths {
    val group: Path
    val plugin: Path
    val resolved: Path
    val moduleSources: Path

    companion object {
        fun get(path: String, module: String) : PluginConfigurationPaths =
            when(path.substringAfterLast('/')) {
                module -> MultiModulePluginConfigurationPaths(Paths.get(path))
                else -> SingleModulePluginConfigurationPaths(Paths.get(path))
            }
    }
}

private open class MultiModulePluginConfigurationPaths(override val moduleSources: Path) : PluginConfigurationPaths {
    override val group: Path get() = plugin.parent
    override val plugin: Path get() = resolved.parent
    override val resolved: Path get() = moduleSources.parent
}

private class SingleModulePluginConfigurationPaths(moduleSources: Path): MultiModulePluginConfigurationPaths(moduleSources) {
    override val resolved: Path get() = moduleSources
}