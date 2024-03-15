
Ktor allows you to use Pebble templates as views within your application by installing the [Pebble](https://ktor.io/docs/pebble.html) plugin.

## Usage

To load templates, you need to configure how to load templates using `PebbleEngine.Builder`. For example, the code snippet below enables Ktor to look up templates in the `templates` package relative to the current classpath:
```kotlin
install(Pebble) {
    loader(ClasspathLoader().apply {
        prefix = "templates"
    })
}
```

To learn more, see [Pebble](https://ktor.io/docs/pebble.html).
