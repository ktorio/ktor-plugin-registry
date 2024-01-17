package io.ktor.plugins.registry

import java.nio.file.Paths

fun main() {
    val builder = RegistryBuilder()
    for (target in listOf("server", "client")) {
        builder.buildRegistry(
            pluginsRoot = Paths.get("plugins"),
            buildDir = Paths.get("build"),
            target = target
        )
    }
}
