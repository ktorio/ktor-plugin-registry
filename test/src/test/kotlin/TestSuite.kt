package io.ktor.registry

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.engine.concurrency.TestExecutionMode
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.kastle.*
import org.jetbrains.kastle.io.FileSystemPackRepository.Companion.export
import org.jetbrains.kastle.io.deleteRecursively
import org.jetbrains.kastle.io.export
import org.jetbrains.kastle.io.readToml
import org.jetbrains.kastle.io.resolve
import org.jetbrains.kastle.logging.ConsoleLogger
import org.jetbrains.kastle.logging.LogLevel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries

@OptIn(ExperimentalKotest::class)
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
    val generator = ProjectGenerator(repository, log = ConsoleLogger(level = LogLevel.INFO))
    val gradle = PackId("org.gradle", "gradle")
    val maven = PackId("org.apache", "maven")
    val amper = PackId("org.jetbrains", "amper")
    // more gradle, less amper / maven, for performance
    val buildSystems = listOf(gradle, amper, maven, gradle, gradle)
    val executables = mutableMapOf<PackId, java.nio.file.Path>()

    val engines = listOf(
        "server-netty",
        "server-cio",
        "server-jetty",
        "server-tomcat",
    ).map {
        PackId.parse("io.ktor/$it")
    }

    val testCases = ConcurrentLinkedDeque<KtorPackTestCase>().also { list ->
        runBlocking {
            repository.readAll().collectIndexed { i, pack ->
                // ignore client packs
                if ("server" !in pack.tags) return@collectIndexed
                // ignore core packs
                if ("core" in pack.tags) return@collectIndexed

                list.add(
                    KtorPackTestCase(
                        pack = pack,
                        buildSystem = buildSystems[i % buildSystems.size],
                        serverEngine = engines[i % engines.size],
                    )
                )
            }
        }
    }.sortedBy {
        it.pack.name
    }

    // Reuse the same wrappers so that Gradle can at least use the same daemon.
    fun runWrapper(
        buildSystemId: PackId,
        projectPath: java.nio.file.Path,
        fileName: String,
        target: String,
        expectedOutput: String
    ) {
        val wrapperExecutable = executables.getOrPut(buildSystemId) {
            projectPath.resolve(fileName).also { executable ->
                Files.setPosixFilePermissions(
                    executable,
                    PosixFilePermissions.fromString("rwxr-xr-x")
                )
            }
        }
        val process = ProcessBuilder(listOf(wrapperExecutable.absolutePathString(), target))
            .directory(projectPath.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        output shouldContain expectedOutput
    }

    for (testCase in testCases) {
        val pack = testCase.pack
        val engine = testCase.serverEngine
        val buildSystem = testCase.buildSystem.let { bs ->
            // defaults to gradle when not compatible
            when (bs) {
                maven if (testCase.isMultiModule() || testCase.isMultiPlatform()) -> gradle
                amper if (!testCase.isCompatibleWithAmper()) -> gradle
                else -> bs
            }
        }

        feature(testCase.featureName) {
            val projectDir = outputDir.resolve(pack.id.toString()).also {
                fs.createDirectories(it)
            }
            val generated = Job()

            /**
             * There are some optional fields in KASTLE that are non-optional in the Ktor generator.
             */
            scenario("validate") {
                pack.group?.name.shouldNotBeNull()
                pack.group?.url.shouldNotBeNull()
            }

            /**
             * Ensures the project can be generated from this pack.
             */
            scenario("generates") {
                try {
                    generator.generate(
                        ProjectDescriptor(
                            name = "test-${pack.id.id}",
                            group = "io.ktor",
                            properties = emptyMap(),
                            packs = listOf(
                                buildSystem,
                                engine,
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

            /**
             * We can build and run tests on the generated project.
             */
            scenario("builds (${buildSystem.id})").config(blockingTest = true) {
                generated.join()
                val projectPath = Paths.get(projectDir.toString())
                require(projectPath.listDirectoryEntries().isNotEmpty()) { "Generate failed" }
                when (buildSystem) {
                    gradle -> runWrapper(buildSystem, projectPath, "gradlew", "test", "BUILD SUCCESSFUL")
                    amper -> runWrapper(buildSystem, projectPath, "amper", "test", "0 tests failed")
                    maven -> runWrapper(buildSystem, projectPath, "mvnw", "test", "BUILD SUCCESS")
                }
            }
        }
    }
})

data class KtorPackTestCase(
    val pack: PackDescriptor,
    val buildSystem: PackId,
    val serverEngine: PackId,
) {
    val modules: List<SourceModule> get() = pack.sources.modules.modules
    val featureName: String get() = "${pack.name} (${pack.group?.id?.substringAfterLast('.') ?: "unknown group"})"

    fun isMultiModule(): Boolean = pack.sources.modules is ProjectModules.Multi
    fun isMultiPlatform(): Boolean = modules.any { it.platforms.size > 1 }
    fun isCompatibleWithAmper(): Boolean = modules.all { module ->
        if (module.amper.isNotEmpty()) true
        else module.gradle.plugins.isEmpty()
                && module.dependencies.values.flatten()
                    .none { it is FunctionDependency || (it as? CatalogReference)?.let(::isMissingFromAmper) == true }
    }
}

fun isMissingFromAmper(library: CatalogReference) =
    library.keyInCatalog in setOf(
        "server.routingOpenapi",
        "server.di",
    )
