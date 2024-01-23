Client "Server-sent Events" (SSE) plugin for consuming events sent using the 
[SSE specification](https://html.spec.whatwg.org/multipage/server-sent-events.html).

## Usage

```kotlin
val client = HttpClient {
    install(SSE)
}
client.sse {
    val event = incoming.receive()
}
```