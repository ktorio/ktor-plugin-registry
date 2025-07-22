OpenTelemetry automatically instruments your Ktor application with distributed tracing, metrics, and logging.
It captures detailed information about every HTTP request and response,
creating a complete picture of how your application behaves and interacts with other services.
This observability data helps you understand performance bottlenecks, debug issues across microservices,
and monitor your application's health in production.

## Usage

In this example, we will use [Jaeger](https://www.jaegertracing.io/) as a tracing backend, but you can use any other
OpenTelemetry-compatible observability backend by configuring the appropriate exporter.
To enable Jaeger, you can run the following command:
```
docker run -d --name jaeger_instance \
    -p 4317:4317 \
    -p 16686:16686 \
    jaegertracing/all-in-one:latest
```
Jaeger UI will be available on http://localhost:16686/search

OpenTelemetry provides both server and client instrumentation that work together to create comprehensive observability.
The server instrumentation monitors incoming requests to your application,
while the client instrumentation tracks outgoing HTTP calls to external services.

Start by configuring OpenTelemetry in your application:

* Server
```kotlin
val openTelemetry: OpenTelemetry = ...

embeddedServer(Netty, 8080) {
    install(KtorServerTelemetry) {
        setOpenTelemetry(openTelemetry)
    }
}.start(wait = true)
```

* Client
```kotlin
val openTelemetry: OpenTelemetry = ...

val client = HttpClient {
    install(KtorClientTelemetry) {
        setOpenTelemetry(openTelemetry)
    }
}
```

[ktor-samples:opentelemetry project](https://github.com/ktorio/ktor-samples/tree/main/opentelemetry) contains examples of how to use the `KtorClientTelemetry` and `KtorServerTelemetry` plugins :
* [Examples for the server plugin `KtorServerTelemetry`](https://github.com/ktorio/ktor-samples/tree/main/opentelemetry/server/src/main/kotlin/opentelemetry/ktor/example/plugins/opentelemetry).
* [Examples for the client plugin `KtorClientTelemetry`](https://github.com/ktorio/ktor-samples/tree/main/opentelemetry/client/src/main/kotlin/opentelemetry/ktor/example/plugins/opentelemetry).
