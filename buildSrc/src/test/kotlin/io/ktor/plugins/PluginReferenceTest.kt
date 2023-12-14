package io.ktor.plugins

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PluginReferenceTest {

    @Test
    fun artifactReferences() {
        val artifactReference = ArtifactReference.parseReferenceString("org.jetbrains:kotlin-css-jvm:1.0.0-pre.129-kotlin-1.4.20")
        assertTrue(artifactReference.accepts(File("some/dir/kotlin-css-jvm-1.0-SNAPSHOT.jar")))
    }

}