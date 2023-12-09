package io.ktor.plugins

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File

@Serializable
data class PluginReference(
    val id: String,
    val group: String,
    val artifacts: Map<String, ArtifactReference>,
)

@Serializable(with = ArtifactReferenceStringSerializer::class)
data class ArtifactReference(
    val name: String,
    val version: String,
    var jarFile: File? = null,
) {
    companion object {
        private val jarNameRegex = Regex("""(.*?)(?:-jvm)?-(\d+(?:\.\d+)*.*)\.jar""")
        private val referenceStringRegex = Regex("""(.+?):(.+?)(?:\[(.+)])?""")

        fun parseJarName(jarFile: File): ArtifactReference =
            jarNameRegex.matchEntire(jarFile.name)?.destructured?.let { (name, version) ->
                ArtifactReference(name, version)
            } ?: throw IllegalArgumentException("Unexpected jar file name $jarFile")

        fun parseReferenceString(text: String): ArtifactReference =
            referenceStringRegex.matchEntire(text)?.destructured?.let { (name, version, jar) ->
                ArtifactReference(name, version, File(jar))
            } ?: throw IllegalArgumentException("Invalid reference string $text")

        fun String.containsVersion(version: String): Boolean {
            val regexPattern = replace(".", "\\.").replace("+", ".+")
            return version.matches(Regex(regexPattern))
        }
    }

    fun actualVersion(): String? = jarFile?.let { jar -> parseJarName(jar).version }

    override fun toString() = buildString {
        append(name).append(':').append(version)
        jarFile?.let { jar ->
            append('[').append(jar).append(']')
        }
    }
}

object ArtifactReferenceStringSerializer : KSerializer<ArtifactReference> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ArtifactReference", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ArtifactReference) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ArtifactReference =
        ArtifactReference.parseReferenceString(decoder.decodeString())
}