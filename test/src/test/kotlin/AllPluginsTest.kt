package io.ktor.registry

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.kastle.*
import org.jetbrains.kastle.io.export
import org.jetbrains.kastle.io.resolve
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.listDirectoryEntries

// Run through every plugin and try building and running tests
val AllPlugins by testSuite(
    "All plugins",
    testConfig = TestConfig
        .invocation(TestConfig.Invocation.Concurrent)
) {
    val environment = setupTestEnvironment("all-plugins")
    val fs = SystemFileSystem
    val gradle = PackId("org.gradle", "gradle")
    val maven = PackId("org.apache", "maven")
    val toolchain = PackId("org.jetbrains", "kotlin-toolchain")
    // Amper can be disabled via the `enableAmper` system property (defaults to disabled in CI).
    val toolchainEnabled = System.getProperty("enableAmper")?.toBoolean() == true
    // more gradle, less amper / maven, for performance
    val buildSystems = listOf(gradle, toolchain, maven, gradle, gradle)
    val executables = ConcurrentHashMap<PackId, java.nio.file.Path>()

    val engines = listOf(
        "server-netty",
        "server-cio",
        "server-jetty",
        "server-tomcat",
    ).map {
        PackId.parse("io.ktor/$it")
    }

    val configOptions = listOf(
        "HOCON",
        "YAML",
        "none"
    )

    val allPacks = runBlocking {
        environment.repository.readAll().toList()
    }.sortedBy { it.name }

    val testCases = buildList {
        allPacks.forEachIndexed { i, pack ->
            // ignore client packs
            if ("server" !in pack.tags) return@forEachIndexed
            // ignore core packs
            if ("core" in pack.tags) return@forEachIndexed

            add(
                KtorPackTestCase(
                    pack = pack,
                    buildSystem = buildSystems[i % buildSystems.size],
                    serverEngine = engines[i % engines.size],
                    configFormat = configOptions[i % configOptions.size],
                )
            )
        }
    }

    // Reuse the same wrappers so that Gradle can at least use the same daemon.
    fun runBuildWrapper(
        buildSystemId: PackId,
        projectPath: java.nio.file.Path,
        fileName: String,
        target: String,
        expectedOutput: String,
        vararg extraArgs: String,
    ) {
        val executable = executables.computeIfAbsent(buildSystemId) {
            projectPath.resolve(fileName)
        }
        val output = runWrapper(executable, projectPath, target, *extraArgs)
        output shouldContain expectedOutput
    }

    for (testCase in testCases) {
        val pack = testCase.pack
        val engine = testCase.serverEngine
        val buildSystem = testCase.buildSystem.let { bs ->
            // defaults to gradle when not compatible
            when (bs) {
                maven if (testCase.isMultiModule() || testCase.isMultiPlatform()) -> gradle
                toolchain if (!toolchainEnabled || !testCase.isCompatibleWithKotlinToolchain()) -> gradle
                else -> bs
            }
        }
        val configFormat = testCase.configFormat

        testSuite(testCase.featureName) {
            val projectDir = environment.outputDir.resolve(pack.id.toString()).also {
                fs.createDirectories(it)
            }
            val generated = Job()

            /**
             * There are some optional fields in KASTLE that are non-optional in the Ktor generator.
             */
            test("validate") {
                pack.group?.name.shouldNotBeNull()
                pack.group?.url.shouldNotBeNull()
                val isClient = "client" in pack.tags
                val isServer = "server" in pack.tags
                (isClient xor isServer) shouldBe true
            }

            /**
             * Ensures the project can be generated from this pack.
             */
            test("generates") {
                try {
                    environment.generator.generate(
                        ProjectDescriptor(
                            name = "test-${pack.id.id}",
                            group = "io.ktor",
                            properties = mapOf(
                                VariableId.parse("io.ktor/server-core/configFormat") to configFormat
                            ),
                            packs = buildList {
                                add(buildSystem)
                                add(engine)
                                add(pack.id)
                                if (configFormat == "YAML")
                                    add(PackId.parse("io.ktor/server-config-yaml"))
                            },
                        )
                    ).export(projectDir)
                } catch (e: Throwable) {
                    generated.completeExceptionally(e)
                    throw e
                }
                generated.complete()
            }

            // TODO skip Kafka plugin for now; it will fail on Maven due to KMP issue
            //      waiting for fix in KASTLE
            if (testCase.featureName.startsWith("Kafka"))
                return@testSuite

            /**
             * We can build and run tests on the generated project.
             */
            test("tests pass (${buildSystem.id}, $configFormat)") {
                generated.join()
                val projectPath = Paths.get(projectDir.toString())
                require(projectPath.listDirectoryEntries().isNotEmpty()) { "Generate failed" }
                when (buildSystem) {
                    gradle -> runBuildWrapper(
                        buildSystem,
                        projectPath,
                        "gradlew",
                        "test",
                        "BUILD SUCCESSFUL",
                        "--no-configuration-cache"
                    )

                    toolchain -> runBuildWrapper(buildSystem, projectPath, "kotlin", "test", "0 tests failed")
                    maven -> runBuildWrapper(buildSystem, projectPath, "mvnw", "test", "BUILD SUCCESS")
                }
            }
        }
    }
}

data class KtorPackTestCase(
    val pack: PackDescriptor,
    val buildSystem: PackId,
    val serverEngine: PackId,
    val configFormat: String,
) {
    val modules: List<SourceModule> get() = pack.sources.modules.modules
    val featureName: String get() = "${pack.name} (${pack.group?.id?.substringAfterLast('.') ?: "unknown group"})"

    fun isMultiModule(): Boolean = pack.sources.modules is ProjectModules.Multi
    fun isMultiPlatform(): Boolean = modules.any { it.platforms.size > 1 }
    fun isCompatibleWithKotlinToolchain(): Boolean = modules.all { module ->
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
