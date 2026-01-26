rootProject.name = _project.name

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        for (repository in _project.gradle.repositories) {
            if (repository.gradleFunction != null) {
                _unsafe("${repository.gradleFunction}()")
            } else {
                maven(repository.url)
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        for (repository in _project.gradle.repositories) {
            if (repository.gradleFunction != null) {
                _unsafe("${repository.gradleFunction}()")
            } else {
                maven(repository.url)
            }
        }
    }
    if (_slots.contains("versionCatalogs")) {
        versionCatalogs {
            _slots("versionCatalogs")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    _slots("gradleSettingsPlugins")
}
_slots("gradleSettings")

for (module in _project.modules) {
    if (module.path.isNotEmpty()) {
        include(":${module.path.replace('/', ':')}")
    }
}