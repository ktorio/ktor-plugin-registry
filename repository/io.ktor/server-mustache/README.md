
Ktor allows you to use Mustache templates as views within your application by installing the [Mustache](https://ktor.io/docs/mustache.html) plugin.

## Usage

To load templates, you need to assign the `MustacheFactory` to the `mustacheFactory` property. For example, the code snippet below enables Ktor to look up templates in the `templates` package relative to the current classpath:
```kotlin
install(Mustache) {
    mustacheFactory = DefaultMustacheFactory("templates")
}
```
To learn more, see [Mustache](https://ktor.io/docs/mustache.html).
