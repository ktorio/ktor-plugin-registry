
This plugin enables serving static content provided by WebJars. It allows you to package your assets such as JavaScript libraries and CSS as part of your fat JAR.

## Usage

The configuration below enables the plugin to serve any WebJars assets on the `/assets` path:
```kotlin
install(Webjars) {
    path = "assets"
    zone = ZoneId.of("EST")
}
```

The `zone` argument configures the correct time zone to be used with the `Last-Modified` header to support caching (only if the `ConditionalHeaders` plugin is also installed). To learn more, see [WebJars](https://ktor.io/docs/webjars.html).
