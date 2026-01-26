
Ktor provides the capability to log application events using the `SLF4J` library. The [CallLogging](https://ktor.io/docs/call-logging.html) plugin allows you to log incoming client requests.

## Usage

The example below shows how to add conditions for filtering requests:

```kotlin
install(CallLogging) {
    filter { call ->
        call.request.path().startsWith("/api/v1")
    }
}
```

You can learn more about other configuration capabilities from [CallLogging](https://ktor.io/docs/call-logging.html).
