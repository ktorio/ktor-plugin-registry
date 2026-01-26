
The [Metrics](https://ktor.io/docs/dropwizard-metrics.html) plugin allows you to configure the Metrics to get useful information about the server and incoming requests.

## Usage

The example below shows how to use the SLF4J Reporter to periodically emit reports to any output supported by SLF4J. For example, to output the metrics every 10 seconds, you would:
```kotlin
install(DropwizardMetrics) {
    Slf4jReporter.forRegistry(registry)
        .outputTo(log)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build()
        .start(10, TimeUnit.SECONDS)
}
```
To learn more, see the [Dropwizard metrics](https://ktor.io/docs/dropwizard-metrics.html) topic.
