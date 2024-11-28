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
import java.nio.file.Paths

/**
 * Represents a reference to a plugin, including its ID, group, versions, and client flag.
 *
 * @property id The ID of the plugin.
 * @property group The publishing group or vendor of the plugin.
 * @property versions A map of version ranges to artifacts for the plugin.
 * @property client A flag indicating whether the plugin is a client plugin.
 */
data class PluginReference(
    val id: String,
    val group: PluginGroup,
    val versions: Map<String, Artifacts>,
    val client: Boolean,
) {
    override fun toString() = "${group.id}:$id"
}

/**
 * The credited vendor of the plugin, for displaying links, logo, etc. in the generator.
 *
 * @property id The ID of the plugin group.
 * @property name The name of the plugin group, generally title-case.
 * @property url The URL associated with the plugin group.
 * @property email The contact email of the plugin group.
 * @property logo The logo URL of the plugin group.
 */
data class PluginGroup(
    val id: String,
    val name: String?,
    val url: String?,
    val email: String?,
    val logo: String?,
)

typealias Artifacts = List<ArtifactReference>

val PluginReference.artifacts: Artifacts get() = versions.values.flatten()

@Serializable(ArtifactReferenceSerializer::class)
data class ArtifactReference(
    val group: String? = null,
    val name: String,
    val version: ArtifactVersion,
    val module: String? = null,
) {
    companion object {
        private val referenceStringRegex = Regex("""(?:(.+?):)?(.+?):(.+)""")

        fun parse(
            text: String,
            defaultGroup: String? = null,
            versionVariables: Map<String, String> = emptyMap(),
            module: String? = null,
        ): ArtifactReference =
            referenceStringRegex.matchEntire(text)?.destructured?.let { (group, name, version) ->
                ArtifactReference(
                    group = group.takeIf(String::isNotEmpty) ?: defaultGroup,
                    name = name,
                    version = ArtifactVersion.parse(version, versionVariables),
                    module = module,
                )
            } ?: throw IllegalArgumentException("Invalid artifact reference string \"$text\"")
    }

    override fun toString() =
        buildString {
            if (group != null) {
                append(group).append(':')
            }
            append(name).append(':').append(version)
        }

    fun groupAndName(defaultGroup: PluginGroup) =
        (group ?: defaultGroup.id) + ':' + name

    fun resolve(actualVersion: ArtifactVersion?) =
        (actualVersion?.asNumber())?.let { copy(version = version.resolve(it)) } ?: this
}

@Serializable(ArtifactVersionSerializer::class)
sealed interface ArtifactVersion {
    companion object {
        private val variableRegex = Regex("\\$(?<name>[\\w-_]+)(?:@(?<version>.+))?")

        fun parse(text: String, versionVariableScope: Map<String, String> = emptyMap()): ArtifactVersion = when {
            text == KtorVersion.KEY -> KtorVersion()
            text == KotlinVersion.KEY -> KotlinVersion()
            text.startsWith('$') -> readVersionVariable(text, versionVariableScope)
            text.endsWith(".+") -> VersionRange(prefixVersionToMavenRange(text))
            text.contains(Regex("[,\\[\\]()]")) -> VersionRange(text)
            else -> VersionNumber(text)
        }

        /**
         * Reads and resolves a version variable in the form "$variable@version" where the "@version" is optional.
         * This allows us to export resolved version variables without looking them up repeatedly.
         */
        private fun readVersionVariable(
            text: String,
            versionVariableScope: Map<String, String>
        ): VersionVariable = variableRegex.matchEntire(text)?.destructured?.let { (name, suppliedVersion) ->
            val resolvedVersionString = suppliedVersion.takeIf { it.isNotEmpty() }
                ?: versionVariableScope[name]
                ?: throw IllegalArgumentException("Unresolved version variable: $name")
            val resolvedVersion = parse(resolvedVersionString, versionVariableScope)
            VersionVariable(name, resolvedVersion)
        } ?: throw IllegalArgumentException("Invalid version variable \"$text\"")
    }
    fun asRange(): VersionRange? = null
    fun asNumber(): VersionNumber? = null
    fun toExportString(): String?
    fun resolve(version: VersionNumber): ArtifactVersion
    fun contains(other: ArtifactVersion): Boolean

    val resolvedString: String get() =
        asNumber()?.toString() ?: toString()
    val safeName: String get() =
        Regex("[^a-zA-Z0-9\\-,.]").replace(toString(), "").trim(',')
}

/**
 * Special version string that ensures a plugin is the same as the ktor version.
 */
data class KtorVersion(val resolved: VersionNumber? = null) : ArtifactVersion {
    companion object {
        const val KEY = "=="
    }
    override fun contains(other: ArtifactVersion) = true
    override fun asNumber(): VersionNumber? = resolved
    override fun resolve(version: VersionNumber): ArtifactVersion = KtorVersion(version)
    override fun toExportString(): String = "\$ktor_version"
    override fun toString() = KEY
}

/**
 * Special version that is assigned from project creation from the selected Kotlin version.
 */
data class KotlinVersion(val resolved: VersionNumber? = null) : ArtifactVersion {
    companion object {
        const val KEY = "\$kotlin_version"
    }
    override fun contains(other: ArtifactVersion) = true
    override fun asNumber(): VersionNumber? = resolved
    override fun resolve(version: VersionNumber): ArtifactVersion = KotlinVersion(version)
    override fun toExportString(): String = "\$kotlin_version"
    override fun toString() = KEY
}

/**
 * Standard semantic version number references (i.e., 1.0.0)
 */
data class VersionNumber(
    val number: String,
    val mavenVersion: org.apache.maven.artifact.versioning.DefaultArtifactVersion = org.apache.maven.artifact.versioning.DefaultArtifactVersion(
        number
    )
) : ArtifactVersion, org.apache.maven.artifact.versioning.ArtifactVersion by mavenVersion {
    override fun contains(other: ArtifactVersion): Boolean = this == other
    override fun asNumber(): VersionNumber = this
    override fun resolve(version: VersionNumber): ArtifactVersion = version
    override fun toString(): String = number
    override fun toExportString(): String = number
}

data class VersionRange(
    private val range: org.apache.maven.artifact.versioning.VersionRange,
    private val resolved: VersionNumber? = null,
) : ArtifactVersion {
    constructor(text: String): this(org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec(text))
    override fun contains(other: ArtifactVersion): Boolean = other is VersionNumber && range.containsVersion(other.mavenVersion)
    override fun resolve(version: VersionNumber): ArtifactVersion = copy(resolved = version)
    override fun asRange(): VersionRange = this
    override fun asNumber(): VersionNumber? = resolved
    override fun toString(): String = range.toString()
    override fun toExportString(): String? = asNumber()?.toString()
}

/**
 * Variable name for version represented in catalog (i.e., $ktor_version)
 */
data class VersionVariable(val name: String, val version: ArtifactVersion): ArtifactVersion by version {
    override fun resolve(version: VersionNumber): ArtifactVersion =
        VersionVariable(name, version)

    override fun toString() = "\$$name@$version"
    override fun toExportString(): String? = normalizedName
}

val VersionVariable.normalizedName: String
    get() {
        val variableName = name.replace(Regex("(?<=[a-z])[A-Z]"), "_$0").replace('-', '_').lowercase()
        return '$' + if (variableName.endsWith("_version")) variableName else variableName + "_version"
    }

/**
 * This will replace prefix versions ending with .+ with the corresponding Maven version range.
 * For example, 2.+ -> [2,3)
 */
fun prefixVersionToMavenRange(text: String): String {
    require(text.endsWith(".+")) { "Expected \"$text\" to end with .+" }
    val prefix = text.substringBeforeLast(".+")
    val nextPrefix = prefix.split('.').mapIndexed { index, part ->
        if (index < prefix.count { it == '.' }) part else (part.toInt() + 1).toString()
    }.joinToString(".")
    return "[$prefix,$nextPrefix)"
}

class ArtifactReferenceSerializer: KSerializer<ArtifactReference> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            "ArtifactReference",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: ArtifactReference) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ArtifactReference =
        ArtifactReference.parse(decoder.decodeString())
}

class ArtifactVersionSerializer: KSerializer<ArtifactVersion> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            "ArtifactVersion",
            PrimitiveKind.STRING
        )
    private val versionProperties: Map<String, String> by lazy {
        Paths.get("plugins").readVersionProperties()
    }

    override fun serialize(encoder: Encoder, value: ArtifactVersion) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ArtifactVersion =
        ArtifactVersion.parse(decoder.decodeString(), versionProperties)
}