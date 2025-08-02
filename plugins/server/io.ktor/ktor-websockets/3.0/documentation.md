
Ktor supports the [WebSocket](https://ktor.io/docs/websocket.html) protocol and allows you to create applications that require real-time data transfer from and to the server. For example, WebSockets can be used to create a chat application.

## Usage

To install `WebSockets`, pass it to the `install` function:

```kotlin
install(WebSockets)
```

Optionally, you can configure various `WebSockets` options:

```kotlin
install(WebSockets) {
    pingPeriod = 15.seconds
    timeout = 15.seconds
    maxFrameSize = Long.MAX_VALUE
    masking = false
}
```

To learn how to handle WebSockets sessions, see [WebSocket](https://ktor.io/docs/websocket.html).
