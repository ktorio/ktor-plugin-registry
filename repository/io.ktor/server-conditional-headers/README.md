
The [ConditionalHeaders](https://ktor.io/docs/conditional-headers.html) plugin avoids sending the body of content if it has not changed since the last request. This is achieved by using the following headers:
* The `Last-Modified` response header contains a resource modification time. For example, if the client request contains the `If-Modified-Since` value, Ktor will send a full response only if a resource has been modified after the given date.
* The `Etag` response header is an identifier for a specific resource version. For instance, if the client request contains the `If-None-Match` value, Ktor won't send a full response in case this value matches the `Etag`.

## Usage

The code snippet below shows how to add a `Etag` and `Last-Modified` headers for CSS:
```kotlin
install(ConditionalHeaders) {
    val file = File("src/main/kotlin/com/example/Application.kt")
    version { outgoingContent ->
        when (outgoingContent.contentType?.withoutParameters()) {
            ContentType.Text.CSS -> listOf(
                EntityTagVersion(file.lastModified().hashCode().toString()),
                LastModifiedVersion(Date(file.lastModified()))
            )
            else -> emptyList()
        }
    }
}
```

To learn more, see [Conditional headers](https://ktor.io/docs/conditional-headers.html).
