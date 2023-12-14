package io.ktor.plugins

import com.vdurmont.semver4j.Semver
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
    val versions: Map<String, Artifacts>,
)

typealias Artifacts = List<ArtifactReference>
typealias ResolvedJarMap = Map<String, Map<ArtifactReference, @Serializable(with = FilePathSerializer::class) File>>

val PluginReference.artifacts: List<ArtifactReference> get() = versions.values.flatten()
val PluginReference.manifest: String get() = formatManifestName(versions.keys.single())
fun PluginReference.formatManifestName(version: String) = "$id${version}.json"
fun PluginReference.allArtifactsForVersion(ktorVersion: String): Artifacts =
    versions.entries.firstNotNullOfOrNull { (ktorVersionRange, artifact) ->
        artifact.takeIf {
            Semver(ktorVersion, Semver.SemverType.NPM).satisfies(ktorVersionRange)
        }
    }.orEmpty()
fun ResolvedJarMap.merge(other: ResolvedJarMap) =
    other.mapValues { (key, value) -> value + this[key].orEmpty() }

@Serializable(with = ArtifactReferenceStringSerializer::class)
data class ArtifactReference(
    val group: String? = null,
    val name: String,
    val version: ArtifactVersion,
) {
    companion object {
        private val jarNameRegex = Regex("""(.*?)?-(\d+(?:\.\d+)+.*)\.jar""")
        private val referenceStringRegex = Regex("""(?:(.+?):)?(.+?):(.+)""")

        fun parseJarName(jarFile: File): ArtifactReference =
            jarNameRegex.matchEntire(jarFile.name)?.destructured?.let { (name, version) ->
                ArtifactReference(group = null, name, VersionNumber(version))
            } ?: throw IllegalArgumentException("Unexpected jar file name $jarFile")

        fun parseReferenceString(text: String, defaultGroup: String? = null): ArtifactReference =
            referenceStringRegex.matchEntire(text)?.destructured?.let { (group, name, version) ->
                ArtifactReference(group.takeIf(String::isNotEmpty) ?: defaultGroup, name, ArtifactVersion.parse(version))
            } ?: throw IllegalArgumentException("Invalid reference string $text")
    }

    fun withVersion(version: String): ArtifactReference =
        copy(version = VersionNumber(version))

    fun accepts(jarFile: File): Boolean =
        accepts(parseJarName(jarFile))

    fun accepts(other: ArtifactReference): Boolean {
        // TODO version fix
        return name.removeSuffix("-jvm") == other.name.removeSuffix("-jvm")
               // && version.containsVersion(other.version.toString())
    }

    override fun toString() = buildString {
        if (group != null)
            append(group).append(':')
        append(name).append(':').append(version)
    }
}

sealed interface ArtifactVersion {
    companion object {
        fun parse(text: String): ArtifactVersion = when(text) {
            "==" -> MatchKtor
            else -> VersionNumber(text.fixPreRelease())
        }

        // Pre-releases resolve to SNAPSHOT jars,
        // so we try to be more lenient here.
        private fun String.fixPreRelease(): String =
            replace(Regex("""(\d+)\.\d+\.\d-pre.*$"""), "$1.+")
    }

    fun containsVersion(version: String): Boolean
}
object MatchKtor : ArtifactVersion {
    override fun containsVersion(version: String) = false
    override fun toString() = "=="
}
data class VersionNumber(val number: String) : ArtifactVersion {
    override fun containsVersion(version: String): Boolean {
        return Semver(version, Semver.SemverType.IVY).satisfies(this.number)
    }
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