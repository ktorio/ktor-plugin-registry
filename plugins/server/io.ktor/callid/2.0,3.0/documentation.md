
The `CallId` plugin allows you to trace client requests end-to-end by using unique request IDs or call IDs. Using `CallId` along with `CallLogging` helps you troubleshoot calls by putting a call ID in the MDC context and configuring a logger to show a call ID for each request.

## Usage

The example below shows how to:
- retrieve a call ID and send it in the same header using the `header` function
- use the `verify` function to verify the retrieved call ID

```kotlin
install(CallId) {
    header(HttpHeaders.XRequestId)
    verify { callId: String ->
        callId.isNotEmpty()
    }
}
```

You can learn more about other configuration capabilities from [CallId](https://ktor.io/docs/call-id.html).
