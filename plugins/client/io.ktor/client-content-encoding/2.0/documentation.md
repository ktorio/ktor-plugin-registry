
The ContentEncoding plugin allows you to enable specified compression algorithms (such as 'gzip' and 'deflate') and configure their settings.

The Ktor client provides the [ContentEncoding](https://api.ktor.io/ktor-client/ktor-client-plugins/ktor-client-encoding/io.ktor.client.plugins.compression/-content-encoding/index.html) plugin that allows you to enable specified compression algorithms (such as `gzip` and `deflate`) and configure their settings. This plugin serves two primary purposes:
* Sets the `Accept-Encoding` header with the specified quality value.
* Decodes content received from a server to obtain the original payload.

## Usage

The [example](https://github.com/ktorio/ktor-documentation/tree/%ktor_version%/codeSnippets/snippets/client-content-encoding) below shows how to enable the `deflate` and `gzip` encoders on the client with the specified quality values:

```kotlin
val client = HttpClient(CIO) {
    install(ContentEncoding) {
        deflate(1.0F)
        gzip(0.9F)
    }
}```

