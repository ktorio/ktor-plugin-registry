
The [Compression](https://ktor.io/docs/compression.html) plugin allows you to compress outgoing content. You can use different compression algorithms, including `gzip` and `deflate`, specify the required conditions for compressing data (such as a content type or response size), or even compress data based on specific request parameters.

## Usage

You can configure compression in multiple ways: enable only specific encoders, specify their priorities, compress only specific content types, and so on. For example, To enable only specific encoders, call the corresponding extension functions:
```kotlin
install(Compression) {
    gzip()
    deflate()
}
```
The code snippet below shows how to compress all text subtypes and JavaScript code using `gzip`:
```kotlin
install(Compression) {
    gzip {
        matchContentType(
            ContentType.Text.Any,
            ContentType.Application.JavaScript
        )
    }
}
```
You can learn more from the [Compression](https://ktor.io/docs/compression.html) help topic.
