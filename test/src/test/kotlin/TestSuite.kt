package io.ktor.registry

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.kastle.*
import org.jetbrains.kastle.io.FileSystemPackRepository.Companion.export
import org.jetbrains.kastle.io.deleteRecursively
import org.jetbrains.kastle.io.export
import org.jetbrains.kastle.io.readToml
import org.jetbrains.kastle.io.resolve
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.listDirectoryEntries

class TestSuite : FeatureSpec({
    val fs = SystemFileSystem
    val outputDir = Path("../test-output").also {
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
    val generator = ProjectGenerator(repository)
    val gradle = PackId("org.gradle", "gradle")
    val maven = PackId("org.apache", "maven")
    val amper = PackId("org.jetbrains", "amper")
    // less amper because it's slow
    val buildSystems = listOf(gradle, amper, maven, gradle, maven)
    val engines = listOf(
        "server-netty",
        "server-cio",
        "server-jetty",
        "server-tomcat",
    ).map {
        PackId.parse("io.ktor/$it")
    }

    feature("Ktor repository") {
        val packs = mutableListOf<PackDescriptor>()

        scenario("Loads successfully") {
            repository.readAll().collect(packs::add)
            packs.sortBy { it.name }
        }

        var i = 0
        for (pack in packs) {
            // ignore client packs
            if ("server" !in pack.tags) continue
            // ignore core packs
            if ("core" in pack.tags) continue

            val isMultiModule = pack.sources.modules is ProjectModules.Multi
            var modules = pack.sources.modules.modules
            val isMultiPlatform = modules.any { it.platforms.size > 1 }
            val amperCannotProcess: (SourceModule) -> Boolean = { module ->
                if (module.amper.isNotEmpty()) true
                else module.gradle.plugins.isNotEmpty()
                    || module.dependencies.values.flatten().any { it is FunctionDependency }
            }
            val buildSystem = buildSystems[i % buildSystems.size].let { bs ->
                // defaults to gradle when not compatible
                when(bs) {
                    maven if (isMultiModule || isMultiPlatform) -> gradle
                    amper if (modules.any(amperCannotProcess)) -> gradle
                    else -> bs
                }
            }

            feature("${pack.name} (${pack.group?.id ?: "unknown group"})") {
                val projectDir = outputDir.resolve(pack.id.toString()).also {
                    fs.createDirectories(it)
                }
                val generated = Job()

                scenario("generates") {
                    try {
                        generator.generate(
                            ProjectDescriptor(
                                name = "test-${pack.id.id}",
                                group = "io.ktor",
                                properties = emptyMap(),
                                packs = listOf(
                                    buildSystem,
                                    engines[i % engines.size],
                                    pack.id,
                                ),
                            )
                        ).export(projectDir)
                    } catch (e: Throwable) {
                        generated.completeExceptionally(e)
                        throw e
                    }
                    generated.complete()
                }

                scenario("builds (${buildSystem.id})") {
                    generated.join()
                    val projectPath = Paths.get(projectDir.toString())
                    require(projectPath.listDirectoryEntries().isNotEmpty()) { "Generate failed" }
                    when(buildSystem) {
                        gradle -> {
                            Files.setPosixFilePermissions(projectPath.resolve("gradlew"), PosixFilePermissions.fromString("rwxr-xr-x"))
                            val process = ProcessBuilder("./gradlew", "test", "--stacktrace")
                                .directory(projectPath.toFile())
                                .redirectErrorStream(true)
                                .start()
                            val output = process.inputStream.bufferedReader().use {
                                it.readText()
                            }
                            output shouldContain "BUILD SUCCESSFUL"
                        }
                        amper -> {
                            Files.setPosixFilePermissions(projectPath.resolve("amper"), PosixFilePermissions.fromString("rwxr-xr-x"))
                            val process = ProcessBuilder("./amper", "test")
                                .directory(projectPath.toFile())
                                .redirectErrorStream(true)
                                .start()
                            val output = process.inputStream.bufferedReader().use {
                                it.readText()
                            }
                            output shouldContain "0 tests failed"
                        }
                        maven -> {
                            Files.setPosixFilePermissions(projectPath.resolve("mvnw"), PosixFilePermissions.fromString("rwxr-xr-x"))
                            val process = ProcessBuilder("./mvnw", "test")
                                .directory(projectPath.toFile())
                                .redirectErrorStream(true)
                                .start()
                            val output = process.inputStream.bufferedReader().use {
                                it.readText()
                            }
                            output shouldContain "BUILD SUCCESS"
                        }
                    }
                }
            }

            i++
        }
    }
})
