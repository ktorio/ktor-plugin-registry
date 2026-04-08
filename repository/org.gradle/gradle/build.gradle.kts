val isExecutable: Boolean by _properties

plugins {
    when(_module.platform) {
        "jvm" -> {
            alias(libs.plugins.kotlin.jvm)
        }
        else -> {
            alias(libs.plugins.kotlin.multiplatform)
        }
    }
    for (item in _module.gradle.plugins) {
        alias(_unsafe("${item}"))
    }
}

if (_project.modules.size == 1) {
    group = _project.group
    version = "1.0.0-SNAPSHOT"
}

_slots("buildRoot")

if (_module.platform != "jvm") {
    kotlin {
        _slots("kotlinRoot")
        for (platform in _module.platforms) {
            when(platform) {
                "jvm" -> {
                    jvm()
                }
                "android" -> {}
                "ios" -> {
                    if (_slots.contains("iosOverride")) {
                        _slot("iosOverride")
                    } else {
                        iosArm64()
                        iosSimulatorArm64()
                    }
                }
                "js" -> {
                    js {
                        browser()
                        if (isExecutable) {
                            binaries.executable()
                        }
                    }
                }
                "wasmJs" -> {
                    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
                    wasmJs {
                        browser()
                        if (isExecutable) {
                            binaries.executable()
                        }
                    }
                }
                "web" -> {
                    js {
                        browser()
                        if (isExecutable) {
                            binaries.executable()
                        }
                    }
                    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
                    wasmJs {
                        browser()
                        if (isExecutable) {
                            binaries.executable()
                        }
                    }
                }
            }
        }

        sourceSets {
            for (e in _module.dependencies.entries) {
                if (e.value.isNotEmpty()) {
                    _unsafe("${e.key}Main").dependencies {
                        for (dependency in e.value) {
                            when (dependency.type) {
                                "maven" -> {
                                    if (dependency.exported) {
                                        api("${dependency.group}:${dependency.artifact}:${dependency.version}")
                                    } else {
                                        implementation("${dependency.group}:${dependency.artifact}:${dependency.version}")
                                    }
                                }
                                "project" -> {
                                    if (dependency.exported) {
                                        api(project(dependency.gradlePath))
                                    } else {
                                        implementation(project(dependency.gradlePath))
                                    }
                                }
                                "catalog" -> {
                                    if (dependency.exported) {
                                        api(_unsafe("${dependency.key}"))
                                    } else {
                                        implementation(_unsafe("${dependency.key}"))
                                    }
                                }
                                "function" -> {
                                    if (dependency.exported) {
                                        api(_unsafe("${dependency.functionName}(${dependency.args.joinToString()})"))
                                    } else {
                                        implementation(_unsafe("${dependency.functionName}(${dependency.args.joinToString()})"))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (e in _module.testDependencies.entries) {
                if (e.value.isNotEmpty() || e.key == "common") {
                    _unsafe("${e.key}Test").dependencies {
                        if (e.key == "common") {
                            kotlin("test")
                        }
                        for (dependency in e.value) {
                            when (dependency.type) {
                                "maven" -> {
                                    implementation("${dependency.group}:${dependency.artifact}:${dependency.version}")
                                }
                                "project" -> {
                                    implementation(project(dependency.gradlePath))
                                }
                                "catalog" -> {
                                    implementation(_unsafe("${dependency.key}"))
                                }
                                "function" -> {
                                    implementation(_unsafe("${dependency.functionName}(${dependency.args.joinToString()})"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} else {
    if (_slots.contains("kotlinRoot")) {
        kotlin {
            _slots("kotlinRoot")
        }
    }
    dependencies {
        for (dependency in _module.dependencies.values.flatten()) {
            when (dependency.type) {
                "maven" -> {
                    if (dependency.exported) {
                        api("${dependency.group}:${dependency.artifact}:${dependency.version}")
                    } else {
                        implementation("${dependency.group}:${dependency.artifact}:${dependency.version}")
                    }
                }
                "project" -> {
                    if (dependency.exported) {
                        api(project(dependency.gradlePath))
                    } else {
                        implementation(project(dependency.gradlePath))
                    }
                }
                "catalog" -> {
                    if (dependency.exported) {
                        api(_unsafe("${dependency.key}"))
                    } else {
                        implementation(_unsafe("${dependency.key}"))
                    }
                }
            }
        }

        testImplementation(kotlin("test"))
        for (dependency in _module.testDependencies.values.flatten()) {
            when (dependency.type) {
                "maven" -> {
                    testImplementation("${dependency.group}:${dependency.artifact}:${dependency.version}")
                }
                "project" -> {
                    testImplementation(project(dependency.gradlePath))
                }
                "catalog" -> {
                    testImplementation(_unsafe("${dependency.key}"))
                }
            }
        }
    }
}
