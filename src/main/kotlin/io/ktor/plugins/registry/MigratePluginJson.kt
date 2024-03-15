/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import java.nio.file.Paths

/**
 * A script for migrating from the export format JSON to the more readable yaml + external files.
 */
fun main(args: Array<String>) {
    require(args.isNotEmpty()) {
        "Please supply a path to the JSON file as an argument"
    }
    migratePluginJson(Paths.get(args[0]))
}
