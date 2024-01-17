package io.ktor.plugins.registry

object DocumentationExtractor {
    private val usageRegex = Regex("\n#+\\s*Usage\\s*?\n", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    fun parseDocumentationMarkdown(contents: String): DocumentationEntry =
        usageRegex.split(contents)
            .takeIf { it.size == 2 }
            ?.map(String::trim)
            ?.let { (description, usage) ->
                DocumentationEntry(description, usage)
            } ?: throw IllegalArgumentException("Missing usage section in documentation: \n$contents")
}

data class DocumentationEntry(
    val description: String,
    val usage: String,
)
