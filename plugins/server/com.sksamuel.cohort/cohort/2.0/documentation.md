
[Cohort](https://github.com/sksamuel/cohort) is a Spring Actuator style implementation for Ktor. 
It provides health checks for orchestrators like Kubernetes and management of logging, databases, JVM settings, memory and threads in production.

### Features

All features are disabled by default.

* Comprehensive system healthchecks: Expose healthcheck endpoints that check for thread deadlocks, memory usage, disk space, cpu usage, garbage collection and more.
* Resource healthchecks: Additional modules to monitor the health of Redis, Kafka, Elasticsearch, databases and other resources.
* Micrometer integration: Send healthcheck metrics to a micrometer registry, so you can see which healthchecks are consistently failing or flakely.
* Database pools: See runtime metrics such as active and idle connections in database pools such as Hikari Connection Pool.
* JVM Info: Enable endpoints to export system properties, JVM arguments and version information, and O/S name / version.
* Thread and heap dumps: Optional endpoints to export a thread dump or heap dump, in the standard JVM format, for analysis locally.
* Database migrations: See the status of applied and pending database migrations from either Flyway or Liquibase.
* Logging configuration: View configured loggers and levels and modify log levels at runtime.

For a full list of features see [here](https://github.com/sksamuel/cohort).
 
### Usage

Basic usage is to install the plugin in your Ktor application.

```kotlin
install(Cohort) {
  // enable healthchecks for kubernetes
  healthcheck("/health", health)
}
```

A more comprehensive example is shown below, which enables all features and configures the endpoints.

```kotlin
install(Cohort) {

  // enable an endpoint to display operating system name and version
  operatingSystem = true

  // enable runtime JVM information such as vm options and vendor name
  jvmInfo = true

  // configure the Logback log manager to show effective log levels and allow runtime adjustment
  logManager = LogbackManager

  // show connection pool information
  dataSources = listOf(HikariDataSourceManager(ds))

  // show current system properties
  sysprops = true

  // enable an endpoint to dump the heap in hprof format
  heapDump = true

  // enable an endpoint to dump threads
  threadDump = true

  // set to true to return the detailed status of the healthcheck response
  verboseHealthCheckResponse = true

  // enable healthchecks for kubernetes
  // each of these is optional and can map to any healthcheck url you wish
  // for example if you just want a single health endpoint, you could use /health
  healthcheck("/liveness", livechecks)
  healthcheck("/readiness", readychecks)
  healthcheck("/startup", startupchecks)
}
```

### Healthchecks

Cohort provides HealthChecks for a variety of JVM metrics such as memory and thread deadlocks as well as connectivity to services such as Kafka and Elasticsearch and databases.

We use health checks by adding them to a HealthCheckRegistry instance, along with an interval of how often to run the checks. A registry requires a coroutine dispatcher to execute the checks on. Healthchecks can take advantage of coroutines to suspend if they need to do something IO based. Cohort will periodically run these healthchecks based on the passed schedule and record if they are healthy or unhealthy.

For example:

```kotlin
val checks = HealthCheckRegistry(Dispatchers.Default) {

// detects if threads are mutually blocked on each others locks
register(ThreadDeadlockHealthCheck(), 1.minutes)

// checks that we always have at least one database connection open
register(HikariConnectionsHealthCheck(ds, 1), 5.seconds)
}
```

With the registry created, we register it with Cohort by invoking the healthcheck method along with an endpoint url to expose it on.

For example:

```kotlin
install(Cohort) {
healthcheck("/healthcheck", checks)
}
```

Whenever the endpoint is accessed, a 200 is returned if all health checks are currently reporting healthy, and a 500 otherwise.

Which healthchecks you use is entirely up to you, and you may want to use some healthchecks for startup probes, some for readiness checks and some for liveness checks. See the section on kubernetes for discussion on how to structure healthchecks in a kubernetes environment.

For the full list of healthchecks see [here](https://github.com/sksamuel/cohort?tab=readme-ov-file#available-healthchecks).