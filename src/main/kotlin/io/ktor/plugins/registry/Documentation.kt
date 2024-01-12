package io.ktor.plugins.registry

object DocumentationExtractor {
    private val usageRegex = Regex("\n#+\\s*Usage\\s*?\n", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    fun parseDocumentationMarkdown(contents: String): DocumentationEntry =
        try {
            usageRegex.split(contents).map(String::trim).let { (description, usage) ->
                DocumentationEntry(description, usage)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Missing usage section in documentation: \n$contents")
        }
}

data class DocumentationEntry(
    val description: String,
    val usage: String,
)