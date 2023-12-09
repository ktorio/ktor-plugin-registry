package io.ktor.plugins

import java.nio.file.Paths
import kotlin.test.*

class PluginBuilderTest {

    @Test
    fun getsPlugins() {
        val pluginFolderReferences = pluginFileReferences(Paths.get("src/test/resources/plugins")).toList()
        for (plugin in pluginFolderReferences)
            println(plugin)
        assertFalse(pluginFolderReferences.isEmpty())
    }

}