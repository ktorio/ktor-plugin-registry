
The HttpCache allows you to save previously fetched resources in a cache.

## Usage

To install `HttpCache`, pass it to the `install` function inside a [client configuration block](create-client.md#configure-client):
```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
//...
val client = HttpClient(CIO) {
    install(HttpCache)
}
```

This is enough to enable the client to save previously fetched resources in a cache. For example, if you make two consequent requests to a resource with the configured `Cache-Control` header, the client executes only the first request and skips the second one since data is already saved in a cache.

```kotlin
val client = HttpClient(CIO) {
        install(HttpCache)
        install(Logging) { level = LogLevel.INFO }
    }

    client.get("http://localhost:8080/customer/1")
    client.get("http://localhost:8080/customer/1")
}```

