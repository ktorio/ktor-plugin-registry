Server-Sent Events (SSE) support plugin. It is required to be installed first before binding any sse endpoints.

To learn more, see [specification](https://html.spec.whatwg.org/multipage/server-sent-events.html).

## Usage

Under `Application.module()`:

```kotlin
install(SSE)
```

Under routing:

```kotlin
install(Routing) {
    // creates GET endpoint /hello with single
    // event streamed with content "world"
    sse("/hello") {
        send(ServerSentEvent("world"))
    }
}
```