package io.ktor.plugins.registry

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class CodeSnippetExtractor {

    private val environment by lazy {
        val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, true)
        val configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        }
        KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    fun parseInstallSnippet(site: CodeInjectionSite, contents: String, filename: String? = null): InstallSnippet =
        when(site.extractionMethod) {
            CodeExtractionMethod.FUNCTION_BODY -> {
                val ktFile = compileKotlinSource(contents, filename)
                ktFile.functionBody()?.let { codeBlock ->
                    InstallSnippet.Kotlin(
                        imports = ktFile.importListAsStrings(),
                        code = codeBlock
                    )
                } ?: throw IllegalArgumentException("Could not read install function:\n$contents")
            }
            CodeExtractionMethod.CLASS_BODY -> {
                val ktFile = compileKotlinSource(contents, filename)
                ktFile.classBody()?.let { codeBlock ->
                    InstallSnippet.Kotlin(
                        imports = ktFile.importListAsStrings(),
                        code = codeBlock
                    )
                } ?: throw IllegalArgumentException("Could not read install function:\n$contents")
            }
            CodeExtractionMethod.CODE_CONTENTS -> {
                val ktFile = compileKotlinSource(contents, filename)
                val codeContents = ktFile.endOfImports()?.let { endOfImports ->
                    contents.substring(endOfImports)
                } ?: contents
                InstallSnippet.Kotlin(
                    imports = ktFile.importListAsStrings(),
                    code = codeContents.trim()
                )
            }
            CodeExtractionMethod.VERBATIM -> InstallSnippet.RawContent(contents)
            CodeExtractionMethod.FILE -> InstallSnippet.RawContent(contents, filename)
        }

    private fun compileKotlinSource(contents: String, filename: String? = null) =
        PsiFileFactory.getInstance(environment.project).createFileFromText(filename ?: "Install.kt", KotlinFileType.INSTANCE, contents) as KtFile

}

private fun KtFile.importListAsStrings() =
    importList?.imports?.map { it.text.substring("import ".length) } ?: emptyList()

private fun KtFile.functionBody() =
    declarations.filterIsInstance<KtNamedFunction>()
        .singleOrNull()
        ?.bodyExpression?.text?.trimBraces()?.trimIndent()

private fun KtFile.classBody() =
    declarations.filterIsInstance<KtClass>()
        .singleOrNull()
        ?.body?.text?.trimBraces()?.trimIndent()?.trim('\n')

private fun KtFile.endOfImports(): Int? =
    importDirectives.maxOfOrNull { it.textRange.endOffset }

// Function contents usually will include braces
private fun String.trimBraces() =
    if (startsWith('{') && endsWith('}'))
        substring(1, length - 1)
    else this

sealed interface InstallSnippet {
    val code: String

    data class Kotlin(
        val imports: List<String>,
        override val code: String,
    ) : InstallSnippet

    data class RawContent(
        override val code: String,
        val filename: String? = null
    ) : InstallSnippet
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

// Intended destination of a code snippet in the generated project
enum class CodeInjectionSite(val extractionMethod: CodeExtractionMethod) {
    // In category's install file
    DEFAULT(CodeExtractionMethod.FUNCTION_BODY),

    // In Application.module() { ... } extension:
    INSIDE_APP(CodeExtractionMethod.FUNCTION_BODY),

    // After Application.module() { ... } extension:
    OUTSIDE_APP(CodeExtractionMethod.CODE_CONTENTS),

    // In a file, separate from Application.kt:
    IN_ROUTING(CodeExtractionMethod.FUNCTION_BODY),

    // Serialization config inside install(ContentNegotiation) {...} block:
    SERIALIZATION_CONFIG(CodeExtractionMethod.FUNCTION_BODY),

    // CallLogging config inside install(CallLogging) {...} block:
    CALL_LOGGING_CONFIG(CodeExtractionMethod.FUNCTION_BODY),

    // In ApplicationTest.kt as a separate function:
    TEST_FUNCTION(CodeExtractionMethod.CLASS_BODY),

    // In application resources folder as a separate resource file
    RESOURCES(CodeExtractionMethod.FILE),

    // In separate file near the code
    SOURCE_FILE_KT(CodeExtractionMethod.FILE),

    // In application.conf file
    APPLICATION_CONF(CodeExtractionMethod.VERBATIM),

    // In application.yaml file
    APPLICATION_YAML(CodeExtractionMethod.VERBATIM),
}