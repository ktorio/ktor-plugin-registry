pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") {
            name = "SpaceKotlinJsWrappers"
        }
        maven("https://packages.confluent.io/maven/")
        maven("https://packages.jetbrains.team/maven/p/ktor-htmx-hackathon-2024/htmx")
        maven("https://packages.jetbrains.team/maven/p/ktor-htmx-hackathon-2024/kotlinx-html")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "ktor-plugin-registry"
