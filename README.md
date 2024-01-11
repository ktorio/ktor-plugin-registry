# Ktor Plugin Registry

This project contains references to all Ktor plugins available in the Ktor project generator.

## Adding a plugin

To add a new plugin, follow these easy steps:

### 1. Publish your project to Maven

We read from repositories listed in [Repositories.kt](buildSrc/src/main/kotlin/io/ktor/plugins/registry/Repositories.kt). 

If you'd like to include another Maven repository for your plugin, include an update to
Repositories.kt in your pull request in step 3.  It should have public read access.

### 2. Clone this repository and add files under `/plugins`

Use the following structure for the files:
```
  /plugins
    /<server|client>
      /<group>                        -- e.g. "io.ktor"
        /group.ktor.yaml              -- organization details
        /<plugin-id>                  -- must be unique
           /versions.ktor.yaml        -- see templates/versions.ktor.yaml
           /<version>                 -- ktor version range w/ special chars stripped
             /manifest.ktor.yaml      -- use template templates/manifest.ktor.yaml
             /install.kt              -- contains install function
             /documentation.md        -- contains documentation
```

You can include any number of install files for populating new projects.  More information under 
[templates/manifest.ktor.yaml](templates/manifest.ktor.yaml)

Run `./gradlew buildRegistry` before submitting to test the new files.

### 3. Create a pull request

Once merged, your plugin will be available in the ktor project generator.