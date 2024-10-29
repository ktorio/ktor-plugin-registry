Server-Sent Events (SSE) plugin for the Ktor client, using the [SSE specification](https://html.spec.whatwg.org/multipage/server-sent-events.html).

For more information, please refer to the [documentation](https://ktor.io/docs/3.0.0-beta-1/sse-client.html).

## Usage

```kotlin
val client = HttpClient {
    install(SSE)
}
client.sse {
    val event = incoming.receive()
}
```