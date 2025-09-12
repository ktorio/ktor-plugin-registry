/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class CodeAnalysis(private val classpathJars: List<Path> = emptyList()) {

    fun parseInstallSnippet(
        contents: String,
        meta: SourceCodeMeta,
    ): CodeRef =
        pluginAnalyzer()
            .parseInstallSnippet(contents, meta)

    private fun pluginAnalyzer() = PluginCodeAnalyzer(classpathJars)
}

class PluginCodeAnalyzer(
    classpathJars: List<Path> = emptyList(),
    private val pluginVersionFolder: Path? = null
) {

    private var environment: KotlinCoreEnvironment
    private var psiFileFactory: PsiFileFactory
    private val analyzer = TopDownAnalyzerFacadeForJVM

    init {
        val verbose = false
        val stderrMessages = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, verbose)
        val configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, stderrMessages)
            put(CommonConfigurationKeys.MODULE_NAME, "PluginRegistry")
            put(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS, true)
            put(JVMConfigurationKeys.JDK_HOME, File(System.getenv("JAVA_HOME")))

            pluginVersionFolder?.let {
                addKotlinSourceRoot(pluginVersionFolder.absolutePathString())
            }
            addJvmClasspathRoots(classpathJars.map(Path::toFile))
        }
        environment = KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        psiFileFactory = PsiFileFactory.getInstance(environment.project)
    }

    fun parseInstallSnippet(
        contents: String,
        meta: SourceCodeMeta,
    ): CodeRef =
        when(meta.site.extractionMethod) {
            CodeExtractionMethod.FUNCTION_BODY -> {
                with(compileKotlinSource(contents, meta.file)) {
                    functionBody()?.let { codeBlock ->
                        CodeRef.InjectedKotlin(codeBlock, meta.site, importListAsStrings())
                    } ?: throw IllegalArgumentException("Could not read install function:\n$contents")
                }
            }
            CodeExtractionMethod.CLASS_BODY -> {
                with(compileKotlinSource(contents, meta.file)) {
                    classBody()?.let { codeBlock ->
                        CodeRef.InjectedKotlin(codeBlock, meta.site, importListAsStrings())
                    } ?: throw IllegalArgumentException("Could not read install class:\n$contents")
                }
            }
            CodeExtractionMethod.CODE_CONTENTS -> {
                val ktFile = compileKotlinSource(contents, meta.file)
                val codeContents = ktFile.endOfImports()?.let { endOfImports ->
                    contents.substring(endOfImports)
                } ?: contents
                CodeRef.InjectedKotlin(codeContents.trim(), meta.site, ktFile.importListAsStrings())
            }
            CodeExtractionMethod.VERBATIM -> CodeRef.SourceFile(contents, meta.site)
            CodeExtractionMethod.FILE -> CodeRef.SourceFile(contents, meta.site, meta.file, meta.module, meta.test)
        }

    private fun compileKotlinSource(contents: String, filename: String? = null): KtFile =
        psiFileFactory.createFileFromText(
            filename ?: "Install.kt",
            KotlinFileType.INSTANCE,
            contents
        ) as KtFile

    fun findErrors(): List<CompilationError> {
        val trace = CliBindingTrace(environment.project)
        val sourceFiles = environment.getSourceFiles()
        analyzer.analyzeFilesWithJavaIntegration(
            environment.project,
            sourceFiles,
            trace,
            environment.configuration,
            environment::createPackagePartProvider
        )
        return trace.bindingContext.diagnostics.filter {
            it.severity == Severity.ERROR
        }.map { compileError ->
            val startOffset = compileError.textRanges.first().startOffset
            val (line, column) = compileError.psiFile.viewProvider.document?.let { doc ->
                val line = doc.getLineNumber(startOffset)
                val column = startOffset - doc.getLineStartOffset(line)
                (line + 1) to (column + 1)
            } ?: (null to null)

            CompilationError(
                compileError.psiFile.name,
                line,
                column,
                DefaultErrorMessages.render(compileError)
            )
        }
    }

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



sealed class CodeRef(
    override val site: CodeInjectionSite,
    val code: String,
    override val module: String? = null,
    override val test: Boolean = false,
): SourceCodeMeta {
    val importsOrEmpty get() = (this as? InjectedKotlin)?.imports ?: emptyList()

    class InjectedKotlin(
        code: String,
        site: CodeInjectionSite,
        val imports: List<String>,
    ) : CodeRef(site, code) {
        override val file: String? get() = null
    }

    class SourceFile(
        code: String,
        site: CodeInjectionSite,
        override val file: String? = null,
        module: String? = null,
        test: Boolean = false,
    ) : CodeRef(site, code, module, test)
}

fun CodeRef.isMainInjectionSite() =
    this is CodeRef.InjectedKotlin && site != CodeInjectionSite.TEST_FUNCTION

data class CompilationError(
    val file: String,
    val lineNumber: Int?,
    val column: Int?,
    val message: String,
)
