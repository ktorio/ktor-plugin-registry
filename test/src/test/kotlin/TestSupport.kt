/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.registry

import kotlinx.coroutines.runBlocking
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.kastle.LocalPackRepository
import org.jetbrains.kastle.MutablePackRepository
import org.jetbrains.kastle.ProjectGenerator
import org.jetbrains.kastle.VersionsCatalog
import org.jetbrains.kastle.io.FileSystemPackRepository.Companion.export
import org.jetbrains.kastle.io.isDirectory
import org.jetbrains.kastle.io.readToml
import org.jetbrains.kastle.io.resolve
import org.jetbrains.kastle.logging.ConsoleLogger
import org.jetbrains.kastle.logging.LogLevel
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.absolutePathString

data class TestEnvironment(
    val outputDir: Path,
    val repository: MutablePackRepository,
    val generator: ProjectGenerator,
)

fun setupTestEnvironment(outputDirName: String): TestEnvironment {
    val fs = SystemFileSystem
    val outputDir = Path("../test-output/$outputDirName").also {
        fs.deleteRecursively(it)
        fs.createDirectories(it)
    }
    val ktorVersions = Path("../ktor-version-catalog.toml")
        .readToml<VersionsCatalog>() ?: VersionsCatalog()
    val repository = runBlocking {
        LocalPackRepository(Path("../repository"))
            .export(outputDir.resolve("repository"))
            .also { it.catalogs(it.catalogs() + ktorVersions.copy(name = "ktorLibs")) }
    }
    val generator = ProjectGenerator(repository, log = ConsoleLogger(level = LogLevel.INFO))
    return TestEnvironment(outputDir, repository, generator)
}

fun runWrapper(
    executable: java.nio.file.Path,
    workingDir: java.nio.file.Path,
    vararg args: String,
): String {
    Files.setPosixFilePermissions(executable, PosixFilePermissions.fromString("rwxr-xr-x"))
    val process = ProcessBuilder(listOf(executable.absolutePathString(), *args))
        .directory(workingDir.toFile())
        .redirectErrorStream(true)
        .start()
    return process.inputStream.bufferedReader().use { it.readText() }
}

private fun FileSystem.deleteRecursively(path: Path, visited: Set<Path> = mutableSetOf()) {
    if (isDirectory(path)) {
        val contents = list(path)
        for (entry in contents - visited)
            deleteRecursively(entry, visited + path)
    }
    runCatching {
        delete(path, mustExist = false)
    }
}
