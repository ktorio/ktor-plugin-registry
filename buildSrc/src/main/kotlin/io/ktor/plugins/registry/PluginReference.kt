package io.ktor.plugins.registry

import io.ktor.plugins.registry.SemverUtils.asMavenRange
import io.ktor.plugins.registry.SemverUtils.asMavenVersion
import io.ktor.plugins.registry.SemverUtils.semverString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import java.io.File

@Serializable
data class PluginReference(
    val id: String,
    val group: PluginGroup,
    val versions: Map<String, Artifacts>,
)

@Serializable
data class PluginGroup(
    val id: String,
    val name: String?,
    val url: String?,
)

typealias Artifacts = List<ArtifactReference>

val PluginReference.artifacts: Artifacts get() = versions.values.flatten()

fun PluginReference.allArtifactsForVersion(ktorVersion: String): Artifacts =
    ktorVersion.asMavenVersion().let { releaseVersion ->
        versions.entries.firstNotNullOfOrNull { (versionRange, artifact) ->
            artifact.takeIf {
                versionRange.asMavenRange().containsVersion(releaseVersion)
            }
        }.orEmpty()
    }

@Serializable(with = ArtifactReferenceStringSerializer::class)
data class ArtifactReference(
    val group: String? = null,
    val name: String,
    val version: ArtifactVersion,
) {
    companion object {
        private val referenceStringRegex = Regex("""(?:(.+?):)?(.+?):(.+)""")

        fun parseReferenceString(text: String, defaultGroup: String? = null): ArtifactReference =
            referenceStringRegex.matchEntire(text)?.destructured?.let { (group, name, version) ->
                ArtifactReference(
                    group.takeIf(String::isNotEmpty) ?: defaultGroup,
                    name,
                    ArtifactVersion.parse(version)
                )
            } ?: throw IllegalArgumentException("Invalid reference string $text")
    }

    override fun toString() = buildString {
        if (group != null)
            append(group).append(':')
        append(name).append(':').append(version)
    }
}

sealed interface ArtifactVersion {
    companion object {
        fun parse(text: String): ArtifactVersion = when (text) {
            "==" -> MatchKtor
            else -> VersionNumber(text.semverString())
        }
    }

    // fun containsVersion(version: String): Boolean
}

/**
 * Special version string that ensures a plugin is the same as the ktor version.
 */
object MatchKtor : ArtifactVersion {
    override fun toString() = "=="
}

/**
 * Standard semantic version number references (i.e. 1.0.0)
 */
data class VersionNumber(val number: String) : ArtifactVersion {
    override fun toString(): String = number
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

object FilePathSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FilePath", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(decoder: Decoder): File =
        File(decoder.decodeString())
}

object SemverUtils {

    fun validateRange(range: String) {
        VersionRange.createFromVersionSpec(range)
    }

    fun String.asMavenRange() =
        VersionRange.createFromVersionSpec(this)

    fun String.asMavenVersion() =
        DefaultArtifactVersion(this)

    fun String.semverString() =
        fixBetaNotation()

    // 1.0.0-beta-1 is technically incorrect, it should be 1.0.0-beta.1
    private fun String.fixBetaNotation() =
        replace(Regex("beta-(\\d+)"), "beta.$1")

    // Pre-releases resolve to SNAPSHOT jars,
    // so we try to be more lenient here.
    // TODO we don't parse jar filenames anymore
    private fun String.fixPreRelease(): String =
        replace(Regex("""(\d+)\.\d+\.\d-pre.*$"""), "$1.+")
}