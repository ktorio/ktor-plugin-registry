package io.ktor.plugins.registry

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.yamlMap
import com.charleskorn.kaml.yamlScalar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlTest {

    @Test
    fun parses() {
        val node = Yaml.default.parseToYamlNode("""
            "[1.4,2.0)": foo
        """.trimIndent())
        node.yamlMap.entries.entries.forEach { (key, value) ->
            assertTrue(key.yamlScalar is YamlScalar)
        }

        assertEquals("{'test': 'foo'}", node.contentToString())
    }

}