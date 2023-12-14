package io.ktor.plugins

import java.nio.file.Paths

// TODO
//   remove and report invalid artifacts
//   populate all metadata using manifests imported from jars
fun main() {
    RegistryBuilder().buildRegistry(
        pluginsDir = Paths.get("plugins"),
        buildDir = Paths.get("build"),
    )
}