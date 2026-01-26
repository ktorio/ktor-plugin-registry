
The [CachingHeaders](https://ktor.io/docs/caching.html) plugin adds the capability to configure the `Cache-Control` and `Expires` headers used for HTTP caching. You can introduce different caching strategies for specific content types, such as images, CSS and JavaScript files, and so on.

## Usage

To configure the `CachingHeaders` plugin, you need to define the `options` function to provide specified caching options for a given content type. The code snippet below shows how to add the `Cache-Control` header with the `max-age` option for CSS:
```kotlin
install(CachingHeaders) {
    options { call, outgoingContent ->
        when (outgoingContent.contentType?.withoutParameters()) {
            ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600))
            else -> null
        }
    }
}
```

You can learn more from the [Caching headers](https://ktor.io/docs/caching.html) help topic.
