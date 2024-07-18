# Ktor Plugin Registry

This project contains references to all [Ktor](https://github.com/ktorio/ktor/) plugins available in the web-based 
[Ktor project generator](https://start.ktor.io) and the [IDEA plugin](https://plugins.jetbrains.com/plugin/16008-ktor).

## Adding a plugin

To add a new plugin, follow these easy steps:

1. **Publish your project to Maven.**
   - Use the gradle [maven publish plugin](https://docs.gradle.org/current/userguide/publishing_maven.html) to publish 
     to [Maven Central](https://central.sonatype.org/) or some other public Maven repository.
   - If you'd like to reference another repository, this can be referenced from the plugin manifest in Step 3.
     <br /><br />

2. **Fork and clone this repository.**<br /><br />

3. **Run `./gradlew --console=plain -q createPlugin`**
    - This will prompt you with a couple questions about the new plugin.
    - After it is completed, you should have some new files in this structure:
    ```
    plugins
    └── server
        └── <group>
            ├── group.ktor.yaml
            └── <plugin-id>
                ├── versions.ktor.yaml
                └── 2.0
                    ├── manifest.ktor.yaml
                    ├── install,kt
                    └── documentation.md
    ```
   - You can include any number of install files for populating new projects.  More information under 
     [templates/manifest.ktor.yaml](templates/manifest.ktor.yaml).  The existing plugin files under the 
     [plugins](plugins) folder can also be useful reference for introducing new plugins.
   - If your plugin artifacts aren't published on Maven Central you may need to add an entry to the `allRepositories()`
     function in the [Repositories.kt](buildSrc/src/main/kotlin/io/ktor/plugins/registry/Repositories.kt) file.
   <br /><br />
   
4. **Run `./gradlew buildRegistry`** to test the new files.<br /><br />

5. **Run `./gradlew buildTestProject`** to generate a sample project.
   - You can now experiment with a project generated with your plugin.
   - Iterate on updating the new files, building the registry, and generating the test project until you're happy with the results.

6. **Create a pull request** with the new changes.
    - Once merged, your plugin will be available in the ktor project generator.