# Ktor Plugin Registry

This project contains references to all Ktor plugins available in the Ktor project generator.

## Adding a plugin

To add a new plugin, follow these easy steps:

1. In your project, add a `manifest.ktor.yaml` file to your main resources.
    - The file must be under a directory like `<group>/<plugin-id>`
    - Start with the [sample template](samples/org/group/sample/manifest.ktor.yaml)
2. Publish your project to Maven.
    - We read from repositories listed in [Repositories.kt](buildSrc/src/main/kotlin/io/ktor/plugins/registry/Repositories.kt). 
    - If you'd like to include another Maven repository for your plugin, include an update to Repositories.kt in your 
      pull request in step 3.
3. Create a pull request on this repository with a new file addition in the form `plugins/<server|client>/<group>/<plugin-id>/versions.ktor.yaml`. 
    - See the [plugins](plugins) directory for examples.
    - For organization information, also include a `group.ktor.yaml` file under the `plugins/<server|client>/<group>` directory.

The plugin file contents are a mapping of Ktor version ranges to artifact versions.

For example:
```yaml
^2.0.0: ktor-server-auth-jwt:2.+
^1.0.0: ktor-auth-jwt:1.+
```

The version ranges use the same syntax as [node-semver](https://github.com/npm/node-semver) for defining ranges.  For 
most cases, you can simply use a range like `^2.0.0` annotation for any release under a major version, or `>=1` if you
want to include all Ktor versions.  If a plugin breaks after a release, we'll update the supported versions and notify
the author from the contact information listed in the plugin manifest.


## Running and testing

To validate all plugin manifests, run `./gradlew buildRegistry`.  This will generate the model for the plugin registry
by resolving and verifying all plugins for all relevant Ktor release versions.