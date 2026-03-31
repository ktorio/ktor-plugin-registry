package io.ktor.registry

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Job
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.kastle.*
import org.jetbrains.kastle.io.deleteRecursively
import org.jetbrains.kastle.io.export
import org.jetbrains.kastle.io.resolve
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

class TestSuite : FeatureSpec({
    val repository = LocalPackRepository(Path("../repository"))
    val fs = SystemFileSystem
    val outputDir = Path("../test-output").also {
        fs.deleteRecursively(it)
        fs.createDirectories(it)
    }
    val generator = ProjectGenerator(repository)
    val gradle = PackId("org.gradle", "gradle")
    val maven = PackId("org.apache", "maven")
    val amper = PackId("org.jetbrains", "amper")
    // gradle is repeated because it's faster and more important
    val buildSystems = listOf(gradle, maven, gradle, amper, gradle)
    val engines = listOf(
        "server-netty",
        "server-cio",
        "server-jetty",
        "server-tomcat",
    ).map {
        PackId.parse("io.ktor/$it")
    }

    feature("Ktor repository") {
        val packs = mutableListOf<PackMetadata>()

        scenario("Loads successfully") {
            repository.getAll().collect(packs::add)
        }

        var i = 0
        for (pack in packs) {
            // ignore client packs
            if ("server" !in pack.tags) continue
            // ignore core packs
            if ("core" in pack.tags) continue

            val isMultiModule = pack.modules.size > 1
            val buildSystem = buildSystems[i % buildSystems.size].let { bs ->
                // defaults to gradle when not compatible
                when(bs) {
                    maven if (isMultiModule) -> gradle
                    amper if (pack.modules.any { it.gradle.plugins.isNotEmpty() }) -> gradle
                    else -> bs
                }
            }

            feature(pack.name) {
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
                    when(buildSystem) {
                        gradle -> {
                            Files.setPosixFilePermissions(projectPath.resolve("gradlew"), PosixFilePermissions.fromString("rwxr-xr-x"))
                            val process = ProcessBuilder("./gradlew", "test")
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
                            output shouldContain "testJvm" // TODO add test to server-core
                        }
                    }
                }
            }

            i++
        }
    }
})
