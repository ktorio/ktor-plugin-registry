
The [MicrometerMetrics](https://ktor.io/docs/micrometer-metrics.html) plugin enables `Micrometer` metrics in your Ktor server application and allows you to choose the required monitoring system, such as Prometheus, JMX, Elastic, and so on. By default, Ktor exposes metrics for monitoring HTTP requests and a set of low-level metrics for monitoring the JVM. You can customize these metrics or create new ones.

## Usage

To use `MicrometerMetrics`, you need to create a registry for your monitoring system and assign it to the `registry` property:

```kotlin
fun Application.module() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }
}
```

Then, you can configure various metrics. For example, to customize tags for each timer, you can use the `timers` function that is called for each request:

```kotlin
install(MicrometerMetrics) {
    // ...
    timers { call, exception ->
        tag("region", call.request.headers["regionId"])
    }
}
```

You can learn more from the [Micrometer metrics](https://ktor.io/docs/micrometer-metrics.html) topic.
