
Ktor allows you to use FreeMarker templates as views within your application by installing the [Freemarker](https://ktor.io/docs/freemarker.html) plugin.

## Usage

To load templates, you need to assign the desired `TemplateLoader` type to the `templateLoader` property. For example, the code snippet below enables Ktor to look up templates in the `templates` package relative to the current classpath:
```kotlin
install(FreeMarker) {
    templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
}
```
To learn more, see [Freemarker](https://ktor.io/docs/freemarker.html).
