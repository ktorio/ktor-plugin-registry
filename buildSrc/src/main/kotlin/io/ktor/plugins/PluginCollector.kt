package io.ktor.plugins

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import io.ktor.plugins.ArtifactReference.Companion.containsVersion
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

fun resolvePlugins(
    pluginDir: Path = Paths.get("plugins"),
    outputDir: Path = Paths.get("build/plugins"),
    configuration: Iterable<File>,
) {
    Files.createDirectories(outputDir)
    for (plugin in pluginFileReferences(pluginDir)) {
        outputDir.resolve("${plugin.id}.yaml").outputStream().use { out ->
            Yaml.default.encodeToStream(plugin.resolveJars(configuration), out)
        }
    }
}

fun pluginFileReferences(pluginDir: Path = Paths.get("plugins")): Sequence<PluginReference> = sequence {
    for (groupFolder in pluginDir.listDirectoryEntries()) {
        for (pluginFolder in groupFolder.listDirectoryEntries()) {
            val pluginFile = pluginFolder.resolve("plugin.yaml")
            if (!pluginFile.exists())
                continue
            val artifacts = pluginFile.inputStream().use {
                Yaml.default.decodeFromStream<Map<String, ArtifactReference>>(it)
            }
            assert(artifacts.isNotEmpty()) { "artifact name is required" }
            yield(
                PluginReference(
                    id = pluginFolder.name,
                    group = groupFolder.name,
                    artifacts = artifacts,
                )
            )
        }
    }
}

fun Iterable<File>.findArtifact(reference: ArtifactReference): File? = find { jarFile ->
    val resolved = ArtifactReference.parseJarName(jarFile)
    reference.name == resolved.name && reference.version.containsVersion(resolved.version)
}

fun PluginReference.resolveJars(configuration: Iterable<File>): PluginReference {
    for (artifact in artifacts.values)
        artifact.resolveJar(configuration)
    return this
}

fun ArtifactReference.resolveJar(configuration: Iterable<File>) {
    jarFile = configuration.find { jarFile ->
        val resolved = ArtifactReference.parseJarName(jarFile)
        name == resolved.name && version.containsVersion(resolved.version)
    }
}