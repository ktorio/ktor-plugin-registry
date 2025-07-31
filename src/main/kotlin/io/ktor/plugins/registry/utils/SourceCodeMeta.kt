/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

// General details for source code injection
interface SourceCodeMeta {
    val site: CodeInjectionSite
    val file: String? get() = null
    val module: String? get() = null
    val test: Boolean get() = false
}

// Intended destination of a code snippet in the generated project
enum class CodeInjectionSite(
    val extractionMethod: CodeExtractionMethod,
    val defaultFileLocation: String? = null,
    val module: String? = null,
) {
    // In category's install file
    DEFAULT(CodeExtractionMethod.FUNCTION_BODY, "install.kt"),

    // In Application.module() { ... } extension:
    INSIDE_APP(CodeExtractionMethod.FUNCTION_BODY, "inside_app.kt"),

    // After Application.module() { ... } extension:
    OUTSIDE_APP(CodeExtractionMethod.CODE_CONTENTS, "outside_app.kt"),

    // In a file, separate from Application.kt:
    IN_ROUTING(CodeExtractionMethod.FUNCTION_BODY, "routing.kt"),

    // Serialization config inside install(ContentNegotiation) {...} block:
    SERIALIZATION_CONFIG(CodeExtractionMethod.FUNCTION_BODY, "content_negotiation.kt"),

    // CallLogging config inside install(CallLogging) {...} block:
    CALL_LOGGING_CONFIG(CodeExtractionMethod.FUNCTION_BODY, "call_logging.kt"),

    // In ApplicationTest.kt as a separate function:
    TEST_FUNCTION(CodeExtractionMethod.CLASS_BODY, "test.kt"),

    // In application resources folder as a separate resource file
    RESOURCES(CodeExtractionMethod.FILE),

    // In separate file near the code
    SOURCE_FILE_KT(CodeExtractionMethod.FILE),

    // In application.conf file
    APPLICATION_CONF(CodeExtractionMethod.VERBATIM, "application.conf"),

    // In application.yaml file
    APPLICATION_YAML(CodeExtractionMethod.VERBATIM, "application.yaml"),

    // In client module, when specified from a server plugin
    CLIENT(CodeExtractionMethod.FUNCTION_BODY, module = "client"),

    // In the main function of the web module, executed in the browser
    WEB(CodeExtractionMethod.FUNCTION_BODY, module = "web"),

    // Outside the main function of the web module
    OUTSIDE_WEB(CodeExtractionMethod.CODE_CONTENTS, module = "web"),

    // In gradle.settings.kts
    GRADLE_SETTINGS(CodeExtractionMethod.VERBATIM),

    // In gradle.build.kts
    GRADLE_BUILD(CodeExtractionMethod.VERBATIM);

    val lowercaseName: String get() = name.lowercase()
}

// What bits of the code to use for injection
enum class CodeExtractionMethod {
    // pulls the body out of the function declaration found in this file
    FUNCTION_BODY,
    // uses the contents of the class declaration to be injected into another class
    CLASS_BODY,
    // extracts all top-level declarations for the code, excluding imports
    CODE_CONTENTS,
    // 1:1 copy of file contents
    VERBATIM,
    // 1:1 copy of file contents, preserving file name
    FILE
}

fun CodeInjectionSite.asMeta(
    file: String? = null,
    module: String? = null,
    test: Boolean = false,
) = object : SourceCodeMeta {
    override val site = this@asMeta
    override val file = file
    override val module = module
    override val test = test
}
