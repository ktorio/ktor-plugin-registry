
Ktor allows you to use Thymeleaf templates as views within your application by installing the [Thymeleaf](https://ktor.io/docs/thymeleaf.html) plugin.

## Usage

Inside the `install` block, you can configure the `ClassLoaderTemplateResolver`. For example, the code snippet below enables Ktor to look up `*.html` templates in the `templates` package relative to the current classpath:
```kotlin
install(Thymeleaf) {
    setTemplateResolver(ClassLoaderTemplateResolver().apply {
        prefix = "templates/"
        suffix = ".html"
        characterEncoding = "utf-8"
    })
}
```

To learn more, see [Thymeleaf](https://ktor.io/docs/thymeleaf.html).
