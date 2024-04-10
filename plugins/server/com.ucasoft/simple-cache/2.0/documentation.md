
Base solution which provides the plugin implementation and abstract class for cache providers.

[![Maven Central with version prefix filter](https://img.shields.io/maven-central/v/com.ucasoft.ktor/ktor-simple-cache/0.2.8?color=blue)](https://search.maven.org/artifact/com.ucasoft.ktor/ktor-simple-cache/0.2.8/jar)
## Setup
### Gradle
```kotlin
repositories {
    mavenCentral()
}

implementation("com.ucasoft.ktor:ktor-simple-cache:0.2.8")
```
## Usage
```kotlin
    install(SimpleCache) {
        cacheProvider {
            invalidateAt = 10.seconds
        }
    }

    routing {
        cacheOutput(2.seconds) {
            get("short-cache") {
                call.respond(Random.nextInt())
            }
        }
        cacheOutput {
            get("default-cache") {
                call.respond(Random.nextInt())
            }
        }
        // Cache key will be built only on listed query keys. Others will be ignored!
        // `/based-only-on-id-cache?id=123?time=100` and `/based-only-on-id-cache?id=123?time=200` requests will use similar cache key!
        cacheOutput(queryKeys = listOf("id")) {
            get("based-only-on-id-cache") {
                call.respond(Random.nextInt())
            }
        }
    }
```