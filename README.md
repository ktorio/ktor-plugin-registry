# Ktor Plugin Registry

This project contains references to all [Ktor](https://github.com/ktorio/ktor/) plugins available in the web-based 
[Ktor project generator](https://start.ktor.io) and the [IDEA plugin](https://plugins.jetbrains.com/plugin/16008-ktor).

Modules listed here are compatible with the [Kotlin Application Source Templating and Layout Engine (KASTLE)](https://github.com/ktorio/kastle), which powers the project generator back end.

## Adding a plugin

To add a new plugin, follow these easy steps:

1. **Publish your project to Maven.**
   - Use the Gradle [maven publish plugin](https://docs.gradle.org/current/userguide/publishing_maven.html) to publish 
     to [Maven Central](https://central.sonatype.org/) or some other public Maven repository.
   - If you'd like to reference another repository, this can be referenced from the plugin manifest in Step 3.
   <br /><br />

2. **Fork and clone this repository.**
    - For best experience, import the project into IntelliJ IDEA.  This should automatically configure the JSON schema for easier editing.
    <br /><br />

3. **Run `./new.sh`**
    - This will prompt you with a couple questions about the new plugin.
    - After it is completed, you should have some new files in this structure:
    ```
    repository
    └── <group>
        ├── group.ksl.yaml
        └── <plugin-id>
            ├── pack.ksl.yaml
            └── <server / client> # module path
                ├── module.ksl.yaml
                └── src
                    └── <Plugin>.kt
    ```
   - You can include any number of source files for populating new projects.
   - Information for the manifest files can be found in the [KASTLE documentation](https://github.com/ktorio/kastle/blob/main/docs/manifests.md).
   - Filtering in the project generator is done using the tags in the pack manifest, so it's important to have "server" or "client" in the tags, along with whichever category it belongs to.
   <br /><br />
   
4. **Run `./gradlew kslRunProject`** to build a new project with the plugin.
    - The project configuration is provided by project.ksl.yaml, which can be modified as you see fit.
    - You can also use `./gradlew kslRunServer` to run the KASTLE server locally and explore the other plugins and properties.
   <br /><br />

5. **Create a pull request** with the new changes.
    - Once merged, your plugin will be available in the Ktor project generator.


### Examples

Here are a few examples to illustrate different techniques that you can apply for your extensions:

| plugin                                              | features                                             |
|-----------------------------------------------------|------------------------------------------------------|
| [Koin](repository/io.insert-koin/server-koin)       | Basic layout with an extra source file               |
| [Exposed](repository/org.jetbrains/server-exposed)  | Several injections and dependencies                  |
| [Kafka](repository/io.github.flaxoos/server-kafka)  | Includes configuration files and a custom repository |
| [kotlinx-rpc](repository/org.jetbrains/kotlinx-rpc) | Multi-module layout with core, client, and server    |                                              
