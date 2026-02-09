
The `HttpRequestRetry` plugin allows you to configure the retry policy for failed requests in various ways: specify the number of retries, configure conditions for retrying a request, or specify delay logic.
By default, the Ktor client doesn't retry [requests](request.md) that failed due to network or server errors.
You can use the [HttpRequestRetry](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.plugins/-http-request-retry/index.html) plugin to configure the retry policy for failed requests in various ways: specify the number of retries, configure conditions for retrying a request, or modify a request before retrying.

## Usage

A [runnable example](https://github.com/ktorio/ktor-documentation/tree/%ktor_version%/codeSnippets/snippets/client-retry) below shows how to configure the basic retry policy:

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
//...
val client = HttpClient(CIO) {
    install(HttpRequestRetry)
}
### Configure retry conditions {id="conditions"}

There are also configuration settings that allow you to configure conditions for retrying a request or specify delay logic:

```kotlin
install(HttpRequestRetry) {
    maxRetries = 5
    retryIf { request, response ->
        !response.status.isSuccess()
    }
    retryOnExceptionIf { request, cause -> 
        cause is NetworkError 
    }
    delayMillis { retry -> 
        retry * 3000L 
    } // retries in 3, 6, 9, etc. seconds
}
```

### Modify a request before retrying {id="modify"}

If you need to modify a request before retrying, use `modifyRequest`:

```kotlin
install(HttpRequestRetry) {
    // Retry conditions
    modifyRequest { request ->
        request.headers.append("x-retry-count", retryCount.toString())
    }
}
```
```

* The `retryOnServerErrors` function enables retrying a request if a `5xx` response is received from a server and specifies the number of retries.
* `exponentialDelay` specifies an exponential delay between retries, which is calculated using the Exponential backoff algorithm.

You can learn more about supported configuration options from [HttpRequestRetry.Configuration](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.plugins/-http-request-retry/-configuration/index.html).
