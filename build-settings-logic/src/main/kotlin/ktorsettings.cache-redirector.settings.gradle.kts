/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import java.net.URI

private val ProviderFactory.isCIRun: Provider<Boolean>
    get() = gradleProperty("teamcity").map { true }
        .orElse(environmentVariable("TEAMCITY_VERSION").map { true })
        .orElse(false)

// This extra property is set by `cache-redirector.init.gradle.kts`,
// so adding that init-script means enabling cache redirector.
private val Settings.isCacheRedirectorEnabled: Boolean
    get() = extra.has("ktorbuild.cacheRedirectorEnabled") && extra["ktorbuild.cacheRedirectorEnabled"] == true

fun Project.overrideGradleDistributionUrl() {
    gradle.taskGraph.whenReady {
        tasks.named<Wrapper>("wrapper") {
            distributionUrl = distributionUrl.replace(
                "https://services.gradle.org/distributions",
                "https://cache-redirector.jetbrains.com/services.gradle.org/distributions",
            )
        }
    }
}

// Check repositories are overridden section
// Copied from the Kotlin project
// Source: https://github.com/JetBrains/kotlin/blob/v2.3.10/repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts
abstract class CheckRepositoriesTask : DefaultTask() {
    @get:Input
    val teamcityBuild = project.providers.isCIRun

    @get:Input
    val ivyNonCachedRepositories = project.providers.provider {
        project.repositories
            .filterIsInstance<IvyArtifactRepository>()
            .filter {
                @Suppress("SENSELESS_COMPARISON")
                it.url == null
            }
            .map { it.name }
    }

    @get:Input
    val nonCachedRepositories = project.providers.provider {
        project.repositories.findNonCachedRepositories()
    }

    @get:Input
    val nonCachedBuildscriptsRepositories = project.providers.provider {
        project.buildscript.repositories.findNonCachedRepositories()
    }

    @get:Internal
    val projectDisplayName = project.displayName

    @TaskAction
    fun checkRepositories() {
        val testName = "$name in $projectDisplayName"
        val isTeamcityBuild = teamcityBuild.get()
        if (isTeamcityBuild) {
            testStarted(testName)
        }

        ivyNonCachedRepositories.get().forEach { ivyRepoName ->
            logInvalidIvyRepo(testName, projectDisplayName, isTeamcityBuild, ivyRepoName)
        }

        nonCachedRepositories.get().forEach { repoUrl ->
            logNonCachedRepo(testName, projectDisplayName, repoUrl, isTeamcityBuild)
        }

        nonCachedBuildscriptsRepositories.get().forEach { repoUrl ->
            logNonCachedRepo(testName, projectDisplayName, repoUrl, isTeamcityBuild)
        }

        if (isTeamcityBuild) {
            testFinished(testName)
        }
    }

    private fun URI.isCachedOrLocal() = scheme == "file" ||
            host == "cache-redirector.jetbrains.com" ||
            host == "teamcity.jetbrains.com" ||
            host == "buildserver.labs.intellij.net"

    private fun RepositoryHandler.findNonCachedRepositories(): List<String> {
        val mavenNonCachedRepos = filterIsInstance<MavenArtifactRepository>()
            .filterNot { it.url.isCachedOrLocal() }
            .map { it.url.toString() }

        val ivyNonCachedRepos = filterIsInstance<IvyArtifactRepository>()
            .filterNot { it.url.isCachedOrLocal() }
            .map { it.url.toString() }

        return mavenNonCachedRepos + ivyNonCachedRepos
    }

    private fun escape(s: String): String {
        return s.replace("[|'\\[\\]]".toRegex(), "\\|$0").replace("\n".toRegex(), "|n").replace("\r".toRegex(), "|r")
    }

    private fun testStarted(testName: String) {
        println("##teamcity[testStarted name='%s']".format(escape(testName)))
    }

    private fun testFinished(testName: String) {
        println("##teamcity[testFinished name='%s']".format(escape(testName)))
    }

    private fun testFailed(name: String, message: String, details: String) {
        println("##teamcity[testFailed name='%s' message='%s' details='%s']"
            .format(escape(name), escape(message), escape(details)))
    }

    private fun logNonCachedRepo(
        testName: String,
        projectDisplayName: String,
        repoUrl: String,
        isTeamcityBuild: Boolean
    ) {
        val msg = "Repository $repoUrl in $projectDisplayName should be cached with cache-redirector"
        val details = "Using non cached repository may lead to download failures in CI builds." +
                " Check https://github.com/ktorio/ktor/blob/main/build-settings-logic/src/main/kotlin/ktorsettings.cache-redirector.settings.gradle.kts for details."

        if (isTeamcityBuild) {
            testFailed(testName, msg, details)
        }

        logger.warn("WARNING - $msg\n$details")
    }

    private fun logInvalidIvyRepo(
        testName: String,
        projectDisplayName: String,
        isTeamcityBuild: Boolean,
        ivyRepoName: String,
    ) {
        val msg = "Invalid ivy repo found in $projectDisplayName"
        val details = "Url must be not null for $ivyRepoName repository"

        if (isTeamcityBuild) {
            testFailed(testName, msg, details)
        }

        logger.warn("WARNING - $msg: $details")
    }
}

fun Project.addCheckRepositoriesTask() {
    tasks.register("checkRepositories", CheckRepositoriesTask::class.java)
}

fun Project.configureTestTasks() {
    tasks.withType<Test>().configureEach {
        systemProperty("ktorbuild.cacheRedirectorEnabled", true)
        systemProperty(
            "ktorbuild.cacheRedirectorInitScript",
            settingsDir.resolve("gradle/cache-redirector.init.gradle.kts").absolutePath,
        )
    }
}

// Main configuration
// Repositories are overridden by applying `--init-script gradle/cache-redirector.init.gradle.kts`.
// Here we apply additional project configurations.

if (isCacheRedirectorEnabled) {
    gradle.beforeProject {
        addCheckRepositoriesTask()
        configureTestTasks()
    }
}

// Override Gradle distribution URL to use cache redirector.
// Must run unconditionally because the :wrapper task only generates the URL in gradle-wrapper.properties.
gradle.rootProject {
    overrideGradleDistributionUrl()
}
