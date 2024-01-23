Server-Sent Events (SSE) support plugin for sending events using the [SSE specification](https://html.spec.whatwg.org/multipage/server-sent-events.html).

This extends routing to include the `sse` function, which creates a special GET endpoint for streaming events.

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