
The [HttpsRedirect](https://ktor.io/docs/https-redirect.html) plugin makes all affected HTTP calls perform a redirect to their HTTPS counterpart before processing the call. By default, the redirection is a `301 Moved Permanently`, but it can be configured to be a `302 Found` redirect.

## Usage

Typical `HttpsRedirect` configuration might look as follows:
```kotlin
install(HttpsRedirect) {
    sslPort = 443
    permanentRedirect = true
}
```
To learn more, see [HttpsRedirect](https://ktor.io/docs/https-redirect.html).
