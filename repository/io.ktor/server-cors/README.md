
If your server supposes to handle cross-origin requests, you need to install and configure the [CORS](https://ktor.io/docs/cors.html) Ktor plugin. This plugin allows you to configure allowed hosts, HTTP methods, headers set by the client, and so on.

## Usage

Typical CORS configuration might look as follows:
```kotlin
install(CORS) {
    allowHost("0.0.0.0:5000")
    allowHeader(HttpHeaders.ContentType)
}
```
To learn more, see [CORS](https://ktor.io/docs/cors.html).
