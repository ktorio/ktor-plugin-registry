Server-Sent Events (SSE) support plugin for sending events using the [SSE specification](https://html.spec.whatwg.org/multipage/server-sent-events.html).

This extends routing to include the `sse` function, which installs a special GET endpoint for streaming events.

For more information, please refer to the [documentation](https://ktor.io/docs/3.0.0-beta-1/sse-server.html).

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