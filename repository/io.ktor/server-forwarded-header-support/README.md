
The [ForwardedHeaders](https://ktor.io/docs/forward-headers.html) plugin allows you to handle reverse proxy headers to get information about the original request when it's behind a proxy.
* `ForwardedHeaders` handles the standard `Forwarded` header.
* `XForwardedHeaders` handles `X-Forwarded-Host`/`X-Forwarded-Server`, `X-Forwarded-For`, `X-Forwarded-By`, `X-Forwarded-Proto`/`X-Forwarded-Protocol`, and `X-Forwarded-SSL`/ `Front-End-Https`.

## Usage

You can install `ForwardedHeaders` as follows:
```kotlin
install(ForwardedHeaders)
```
To learn how to get information about the original request, see [ForwardedHeaders](https://ktor.io/docs/forward-headers.html).
