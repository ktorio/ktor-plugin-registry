# How to contribute

Thank you for your interest in contributing to the Ktor ecosystem!

For introducing a new plugin to the registry, please follow the 
[instructions presented in README.md](README.md#adding-a-plugin).  For more information about contributing to this
repository, continue reading!

Independently of how you'd like to contribute, please make sure you read and comply with the [Code of Conduct](CODE_OF_CONDUCT.md).

## Project structure

#### /buildSrc

This includes the initial resolution of all Ktor release build configurations that are required for building out 
the plugin registry.  A few of the kotlin sources are shared with the main classpath for collecting the plugin 
references under the [plugins](plugins) folder.

#### /plugins

This is where all the Ktor plugin configuration files are located.

These files are organized by: 
```
plugins / {client|server} / {group-id} / {plugin-id} / {version-id}
```

Please refer to the [README](README.md) for contributing to the plugin files.

You'll notice there are `manifest.json` files included in many of the official Ktor plugins.  These files were migrated
from the previous iteration of the plugin registry and are used for populating the registry.  It is recommended to 
ignore these for the purposes of contributing new plugins and instead use the more user-friendly YAML format.

#### /templates

Reference examples are located here for working with the plugin configuration files.  There are also JSON schema files 
located under [templates/schema](templates/schema) which are used for IDE support in editing the YAML files.

#### /src

The main source path includes all the code for building out the plugin registry for exporting for project generation.

## Building

For building the project, simply run `./gradlew build`.  This builds the project and runs the tests to ensure the 
plugin artifacts can be resolved and the registry builder is functioning normally.  Note, this does not build the 
registry for export to the project generator (see [Building the registry](CONTRIBUTING.md#building-the-registry)).

#### Resolving plugin artifacts

When this project is built, it will also resolve all the artifacts required by the various Ktor plugins found 
under [plugins](plugins) while iterating through all the relevant Ktor releases found in maven 
(see [KtorReleases.kt](buildSrc/src/main/kotlin/io/ktor/plugins/registry/KtorReleases.kt)).  During this step, 
the build will output `<client/server>-artifacts.yaml` and `ktor_releases` files which are later used for building 
the registry.  To run this step in isolation, run `./gradlew resolvePlugins`.

#### Building the registry

The primary goal of this project is for compiling and exporting its plugin metadata for use in Ktor project generation.

To build the plugin registry, execute the gradle target `./gradlew buildRegistry`.  This target is used for both 
validation and for exporting to consumer applications (i.e. [start.ktor.io](https://start.ktor.io)).  This task will 
verify that all required fields are present and that the imported Kotlin snippets compile using the plugin artifacts.

#### TeamCity

Our CI builds are performed on the [Ktor TeamCity instance](https://ktor.teamcity.com/project/Ktor_ProjectKtorGenerator?mode=builds).

[Test plugin registry](https://ktor.teamcity.com/buildConfiguration/Ktor_KtorPluginRegistryVerify) ensures branches are in a good state before merging, and 
[Publish plugin registry](https://ktor.teamcity.com/buildConfiguration/Ktor_KtorPluginRegistry) delivers the artifacts 
to our [Space files repository](https://jetbrains.team/p/ktor/packages/files/files/plugin-registry).

### Pull Requests

Contributions are made using Github [pull requests](https://help.github.com/en/articles/about-pull-requests):

1. Fork the Ktor repository and work on your fork.
2. Try to [build the registry](CONTRIBUTING.md#building-the-registry) locally before submitting the pull request.  If
   you have some difficulty with the build you can submit the PR and we can help debug the problem.
3. [Create](https://github.com/ktorio/ktor-plugin-registry/compare) a new PR with a request to merge to the **main** branch.
4. Provide as much context as possible regarding the change, including links to issues or YouTrack tickets.

## Issues and Feedback

Please use [YouTrack](https://youtrack.jetbrains.com/issues/KTOR) to submit issues, whether these are
bug reports or feature requests.  For other support, please refer to the channels presented under
[Ktor support](https://ktor.io/support/).