/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.registry

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.kastle.PackId
import org.jetbrains.kastle.ProjectDescriptor
import org.jetbrains.kastle.ProjectGenerator
import org.jetbrains.kastle.VariableId
import org.jetbrains.kastle.VersionsCatalog
import org.jetbrains.kastle.io.FileSystemPackRepository.Companion.export
import org.jetbrains.kastle.io.export
import org.jetbrains.kastle.io.readToml
import org.jetbrains.kastle.io.resolve
import org.jetbrains.kastle.LocalPackRepository
import org.jetbrains.kastle.logging.ConsoleLogger
import org.jetbrains.kastle.logging.LogLevel
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

/**
 * This validates that there are no false claims in the Ktor tutorials when referencing
 * generated projects.
 *
 * For each "Get started with Ktor Server" tutorial, the test:
 *  1. Asks the same [ProjectGenerator] used by [TestSuite] to generate a project with the
 *     plugin set the tutorial tells the reader to select on https://start.ktor.io/.
 *  2. Asserts the file paths and code fragments the tutorial claims will be present.
 *
 * The tutorials referenced are (sources live in
 * https://github.com/ktorio/ktor-documentation/tree/main/topics):
 *   - server-create-a-new-project.topic           ("Create, open, and run a new Ktor project")
 *   - server-application-structure.md             ("Default project structure")
 *   - server-requests-and-responses.topic         ("Handle requests and generate responses")
 *   - server-create-restful-apis.topic            ("Create a RESTful API")
 *   - server-create-website.topic                 ("Create a website with Thymeleaf")
 *   - server-create-websocket-application.topic   ("Create a WebSocket application")
 *   - server-integrate-database.topic             ("Integrate a database with Exposed")
 *
 * NOTE: This suite is **expected to surface failures** wherever the project generator's
 * actual output has drifted from what the tutorials describe. Each failure pinpoints a
 * documentation update that is required.
 */
@OptIn(ExperimentalKotest::class)
class TutorialTests : FeatureSpec({

    // --- Shared setup, mirroring TestSuite ---------------------------------------------------

    val fs = SystemFileSystem
    val outputDir = Path("../test-output/tutorials").also {
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

    // The Ktor Project Generator defaults: Gradle Kotlin + Netty + YAML config.
    val gradle = PackId("org.gradle", "gradle")
    val netty = PackId.parse("io.ktor/server-netty")
    val configYaml = PackId.parse("io.ktor/server-config-yaml")

    // Plugin pack ids referenced from each tutorial's "Add the following plugins" step.
    // (Routing is covered by io.ktor/server-core; there is no separate "Routing" pack.)
    val staticContent       = PackId.parse("io.ktor/server-static-content")
    val contentNegotiation  = PackId.parse("io.ktor/server-content-negotiation")
    val kotlinxSerialization = PackId.parse("io.ktor/server-kotlinx-serialization")
    val statusPages         = PackId.parse("io.ktor/server-status-pages")
    val websockets          = PackId.parse("io.ktor/server-websockets")
    val thymeleaf           = PackId.parse("io.ktor/server-thymeleaf")
    val exposed             = PackId.parse("org.jetbrains/server-exposed")
    val postgres            = PackId.parse("org.jetbrains/server-postgres")

    /**
     * Generates a project with the supplied plugins (using the same defaults the tutorials
     * pick on https://start.ktor.io/) and returns the on-disk root path.
     */
    suspend fun generateTutorialProject(name: String, plugins: List<PackId>): java.nio.file.Path {
        val projectDir = outputDir.resolve(name).also { fs.createDirectories(it) }
        generator.generate(
            ProjectDescriptor(
                name = name,
                group = "com.example",
                properties = mapOf(
                    VariableId.parse("io.ktor/server-core/configFormat") to "YAML",
                ),
                packs = buildList {
                    add(gradle)
                    add(netty)
                    add(configYaml)
                    addAll(plugins)
                },
            )
        ).export(projectDir)
        return Paths.get(projectDir.toString())
    }

    // --- Helpers -----------------------------------------------------------------------------

    fun java.nio.file.Path.read(relative: String): String {
        val p = resolve(relative)
        check(p.exists()) { "Expected file '$relative' to exist under $this, but it was not generated." }
        return p.readText()
    }

    fun java.nio.file.Path.shouldHaveFile(relative: String) {
        val p = resolve(relative)
        check(p.exists() && !p.isDirectory()) {
            "Tutorial implies the generator should produce '$relative', but it does not exist."
        }
    }

    fun java.nio.file.Path.shouldHaveDir(relative: String) {
        val p = resolve(relative)
        check(p.exists() && p.isDirectory()) {
            "Tutorial implies the generator should produce directory '$relative', but it does not exist."
        }
    }

    // -----------------------------------------------------------------------------------------
    // Tutorial 1: server-create-a-new-project.topic
    //   "Two files are created by default, named Application.kt and Routing.kt"
    //   settings.gradle.kts contains rootProject.name = "<artifact>"
    //   src/main/resources/application.yaml has ktor.deployment.port: 8080 and a single
    //       module pointing to com.example.ApplicationKt.module
    //   src/main/kotlin/Routing.kt's configureRouting() returns "Hello World!" on GET /
    //   src/test/kotlin exists for tests
    // -----------------------------------------------------------------------------------------
    feature("Tutorial: Create, open, and run a new Ktor project") {

        lateinit var project: java.nio.file.Path

        scenario("generate project with default plugin set (no extras)") {
            project = generateTutorialProject("ktor-sample", emptyList())
            project.toFile().listFiles().orEmpty().toList().shouldNotBeEmpty()
        }

        scenario("Default project structure: settings.gradle.kts, build.gradle.kts and gradlew exist") {
            project.shouldHaveFile("settings.gradle.kts")
            project.shouldHaveFile("build.gradle.kts")
            project.shouldHaveFile("gradlew")
            project.shouldHaveDir("src/main/kotlin")
            project.shouldHaveDir("src/main/resources")
            project.shouldHaveDir("src/test/kotlin")
        }

        scenario("settings.gradle.kts contains rootProject.name = \"ktor-sample\"") {
            project.read("settings.gradle.kts") shouldContain "rootProject.name = \"ktor-sample\""
        }

        scenario("Application.kt and Routing.kt are both created by default") {
            project.shouldHaveFile("src/main/kotlin/Application.kt")
            project.shouldHaveFile("src/main/kotlin/Routing.kt")
        }

        scenario("Routing.kt contains the documented Hello World GET /") {
            val routing = project.read("src/main/kotlin/Routing.kt")
            routing shouldContain "fun Application.configureRouting()"
            routing shouldContain "get(\"/\")"
            routing shouldContain "Hello World!"
        }

        scenario("Application.kt declares a single Application.module() entry point") {
            val app = project.read("src/main/kotlin/Application.kt")
            app shouldContain "fun Application.module()"
            app shouldContain "configureRouting()"
        }

        scenario("application.yaml exists with ktor.deployment.port 8080 and a single 'module' entry") {
            val yaml = project.read("src/main/resources/application.yaml")
            yaml shouldContain "port: 8080"
            // The tutorial snippet has exactly one module that points at the generated package's
            // ApplicationKt.module function. See codeSnippets/snippets/tutorial-server-get-started/
            //   src/main/resources/application.yaml
            yaml shouldContain "com.example.ApplicationKt.module"
        }

        scenario("Generated package is com.example") {
            project.read("src/main/kotlin/Application.kt") shouldContain "package com.example"
            project.read("src/main/kotlin/Routing.kt")     shouldContain "package com.example"
        }
    }

    // -----------------------------------------------------------------------------------------
    // Tutorial 2: server-requests-and-responses.topic
    //   No extra plugins selected; uses the default project as in Tutorial 1.
    //   "The module written for you by the Project Generator is loaded, which in turn invokes
    //   the routing function" -> Application.module() must call configureRouting().
    // -----------------------------------------------------------------------------------------
    feature("Tutorial: Handle requests and generate responses (Task Manager)") {

        scenario("project ships with Application.module() that delegates to configureRouting") {
            val project = generateTutorialProject("ktor-task-app", emptyList())
            val app = project.read("src/main/kotlin/Application.kt")
            app shouldContain "fun Application.module()"
            app shouldContain "configureRouting()"
        }
    }

    // -----------------------------------------------------------------------------------------
    // Tutorial 3: server-create-restful-apis.topic
    //   Plugins: Routing, Content Negotiation, Kotlinx.serialization, Static Content
    //   "If you look inside the generated code in the project you will find a file called
    //   Serialization.kt inside src/main/kotlin"
    //   "open the index.html page inside src/main/resources/static"
    //   Routing.kt contains staticResources("static", "static")  (per the doc snippet)
    // -----------------------------------------------------------------------------------------
    feature("Tutorial: Create a RESTful API") {

        lateinit var project: java.nio.file.Path

        scenario("generate project with Content Negotiation, Kotlinx serialization and Static Content") {
            project = generateTutorialProject(
                name = "ktor-rest-task-app",
                plugins = listOf(contentNegotiation, kotlinxSerialization, staticContent),
            )
            project.toFile().listFiles().orEmpty().toList().shouldNotBeEmpty()
        }

        scenario("Serialization.kt is generated in src/main/kotlin") {
            project.shouldHaveFile("src/main/kotlin/Serialization.kt")
        }

        scenario("Serialization.kt installs ContentNegotiation with kotlinx.serialization JSON") {
            val ser = project.read("src/main/kotlin/Serialization.kt")
            ser shouldContain "ContentNegotiation"
            ser shouldContain "json"
        }

        scenario("static/index.html sample page is generated under resources") {
            project.shouldHaveFile("src/main/resources/static/index.html")
        }

        scenario("Routing.kt registers staticResources at /static") {
            val routing = project.read("src/main/kotlin/Routing.kt")
            routing shouldContain "staticResources("
        }
    }

    // -----------------------------------------------------------------------------------------
    // Tutorial 4: server-create-website.topic
    //   Plugins: Routing, Static Content, Thymeleaf
    //   "Open the Templating.kt file in src/main/kotlin"
    //   "configureTemplating()" installs Thymeleaf with prefix "templates/thymeleaf/" etc.
    //   "Because you included the Static Content plugin, the following code will be present in
    //    the Routing.kt file:  staticResources(\"/static\", \"static\")"
    // -----------------------------------------------------------------------------------------
    feature("Tutorial: Create a website (Thymeleaf)") {

        lateinit var project: java.nio.file.Path

        scenario("generate project with Static Content and Thymeleaf plugins") {
            project = generateTutorialProject(
                name = "ktor-task-web-app",
                plugins = listOf(staticContent, thymeleaf),
            )
            project.toFile().listFiles().orEmpty().toList().shouldNotBeEmpty()
        }

        scenario("Templating.kt is generated in src/main/kotlin") {
            // Tutorial: "Open the Templating.kt file in src/main/kotlin"
            project.shouldHaveFile("src/main/kotlin/Templating.kt")
        }

        scenario("Templating.kt declares Application.configureTemplating() with the documented Thymeleaf setup") {
            val templating = project.read("src/main/kotlin/Templating.kt")
            templating shouldContain "fun Application.configureTemplating()"
            templating shouldContain "install(Thymeleaf)"
            templating shouldContain "prefix = \"templates/thymeleaf/\""
            templating shouldContain "suffix = \".html\""
            templating shouldContain "characterEncoding = \"utf-8\""
        }

        scenario("Routing.kt contains the documented staticResources(\"/static\", \"static\") line") {
            val routing = project.read("src/main/kotlin/Routing.kt")
            routing shouldContain "staticResources(\"/static\", \"static\")"
        }
    }

    // -----------------------------------------------------------------------------------------
    // Tutorial 5: server-create-websocket-application.topic
    //   Plugins: Routing, Content Negotiation, Kotlinx.serialization, WebSockets, Static Content
    //   "Because you included the WebSockets plugin, a Sockets.kt file has been generated within
    //    src/main/kotlin."
    //   "Open the Sockets.kt file and replace the existing Application.configureSockets() function"
    // -----------------------------------------------------------------------------------------
    feature("Tutorial: Create a WebSocket application") {

        lateinit var project: java.nio.file.Path

        scenario("generate project with WebSockets, Content Negotiation, Kotlinx serialization and Static Content") {
            project = generateTutorialProject(
                name = "ktor-websockets-task-app",
                plugins = listOf(contentNegotiation, kotlinxSerialization, websockets, staticContent),
            )
            project.toFile().listFiles().orEmpty().toList().shouldNotBeEmpty()
        }

        scenario("Sockets.kt is generated in src/main/kotlin") {
            project.shouldHaveFile("src/main/kotlin/Sockets.kt")
        }

        scenario("Sockets.kt declares Application.configureSockets() that installs WebSockets") {
            val sockets = project.read("src/main/kotlin/Sockets.kt")
            sockets shouldContain "fun Application.configureSockets()"
            sockets shouldContain "install(WebSockets)"
        }
    }

    // -----------------------------------------------------------------------------------------
    // Tutorial 6: server-integrate-database.topic
    //   Plugins: Routing, Content Negotiation, Kotlinx.serialization, Static Content,
    //            Status Pages, Exposed, Postgres
    //   "Navigate to src/main/kotlin and delete the files CitySchema.kt and UsersSchema.kt"
    //   "Open the Databases.kt file and remove the content of the configureDatabases() function."
    //   "Open the gradle/libs.versions.toml file and ensure the latest Exposed version is
    //    specified" -> implies an `exposed` version key already exists.
    // -----------------------------------------------------------------------------------------
    feature("Tutorial: Integrate a database (Exposed + Postgres)") {

        lateinit var project: java.nio.file.Path

        scenario("generate project with Status Pages, Exposed and Postgres plugins") {
            project = generateTutorialProject(
                name = "ktor-exposed-task-app",
                plugins = listOf(
                    contentNegotiation,
                    kotlinxSerialization,
                    staticContent,
                    statusPages,
                    exposed,
                    postgres,
                ),
            )
            project.toFile().listFiles().orEmpty().toList().shouldNotBeEmpty()
        }

        scenario("Databases.kt is generated with an Application.configureDatabases() function") {
            project.shouldHaveFile("src/main/kotlin/Databases.kt")
            val db = project.read("src/main/kotlin/Databases.kt")
            db shouldContain "fun Application.configureDatabases()"
        }

        scenario("CitySchema.kt and UsersSchema.kt sample files are generated") {
            // Tutorial step: "delete the files CitySchema.kt and UsersSchema.kt"
            project.shouldHaveFile("src/main/kotlin/CitySchema.kt")
            project.shouldHaveFile("src/main/kotlin/UsersSchema.kt")
        }

        scenario("gradle/libs.versions.toml exists and declares an 'exposed' version") {
            project.shouldHaveFile("gradle/libs.versions.toml")
            val toml = project.read("gradle/libs.versions.toml")
            toml shouldContain "exposed"
        }

        scenario("StatusPages plugin dependency is wired into build.gradle.kts") {
            val build = project.read("build.gradle.kts")
            build shouldContain "status-pages"
        }
    }

    // -----------------------------------------------------------------------------------------
    // Cross-tutorial check: server-application-structure.md "Default project structure" diagram
    //   Claims `application.conf` lives under src/main/resources by default. This contradicts
    //   server-create-a-new-project.topic which uses application.yaml. Recorded as its own
    //   scenario so the discrepancy surfaces in the test report.
    // -----------------------------------------------------------------------------------------
    feature("Tutorial: Application structure default layout") {

        scenario("default project structure diagram lists application.conf in resources") {
            val project = generateTutorialProject("ktor-structure", emptyList())
            // server-application-structure.md shows `application.conf`. The new-project tutorial
            // shows `application.yaml`. Pick the one matching the picked Configuration option:
            // because Tutorial 1 asserts YAML, this one asserts the conf file the structure doc
            // shows.
            project.shouldHaveFile("src/main/resources/application.conf")
        }
    }
})
