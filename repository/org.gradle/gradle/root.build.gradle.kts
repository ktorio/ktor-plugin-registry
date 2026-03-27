val versionCatalogEnabled: Boolean by _properties

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    if (_project.modules.any { it.platform == "jvm" }) {
        alias(libs.plugins.kotlin.jvm) apply false
    }
    for (plugin in _project.gradle.plugins) {
        alias(_unsafe("${plugin.name}")) apply false
    }
}

subprojects {
    group = _project.group
    version = "1.0.0-SNAPSHOT"
}
