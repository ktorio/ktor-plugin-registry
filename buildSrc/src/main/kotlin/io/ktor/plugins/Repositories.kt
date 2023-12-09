package io.ktor.plugins

import org.gradle.api.artifacts.dsl.RepositoryHandler

fun RepositoryHandler.allRepositories() {
    mavenCentral()
}