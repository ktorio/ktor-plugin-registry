/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.io.File

/**
 * Represents a reference to a plugin, including its ID, group, versions, and client flag.
 *
 * @property id The ID of the plugin.
 * @property group The publishing group or vendor of the plugin.
 * @property versions A map of version ranges to artifacts for the plugin.
 * @property client A flag indicating whether the plugin is a client plugin.
 */
@Serializable
data class PluginReference(
    val id: String,
    val group: PluginGroup,
    val versions: Map<String, Artifacts>,
    val client: Boolean,
)

/**
 * The credited vendor of the plugin, for displaying links, logo, etc. in the generator.
 *
 * @property id The ID of the plugin group.
 * @property name The name of the plugin group, generally title-case.
 * @property url The URL associated with the plugin group.
 * @property email The contact email of the plugin group.
 * @property logo The logo URL of the plugin group.
 */
@Serializable
data class PluginGroup(
    val id: String,
    val name: String?,
    val url: String?,
    val email: String?,
    val logo: String?,
)

typealias Artifacts = List<ArtifactReference>

val PluginReference.artifacts: Artifacts get() = versions.values.flatten()

fun PluginReference.allArtifactsForVersion(ktorVersion: String): Artifacts =
    ArtifactVersion.parse(ktorVersion).let { releaseVersion ->
        versions.entries.firstNotNullOfOrNull { (versionRange, artifact) ->
            artifact.takeIf {
                ArtifactVersion.parse(versionRange).contains(releaseVersion)
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
        fun parse(text: String): ArtifactVersion = when {
            text == "==" -> MatchKtor
            text.contains(Regex("[+,\\[\\]()]")) -> VersionRange(text)
            else -> VersionNumber(text)
        }
    }
    fun contains(other: ArtifactVersion): Boolean
}

/**
 * Special version string that ensures a plugin is the same as the ktor version.
 */
object MatchKtor : ArtifactVersion {
    override fun contains(other: ArtifactVersion) = true
    override fun toString() = "=="
}

/**
 * Standard semantic version number references (i.e. 1.0.0)
 */
data class VersionNumber(
    val number: String,
    val mavenVersion: DefaultArtifactVersion = DefaultArtifactVersion(number)
) : ArtifactVersion, org.apache.maven.artifact.versioning.ArtifactVersion by mavenVersion {
    override fun contains(other: ArtifactVersion): Boolean = this == other
    override fun toString(): String = number
}

data class VersionRange(private val range: org.apache.maven.artifact.versioning.VersionRange) : ArtifactVersion {
    constructor(text: String): this(org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec(text))
    override fun contains(other: ArtifactVersion): Boolean = other is VersionNumber && range.containsVersion(other.mavenVersion)
    override fun toString(): String = range.toString()
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