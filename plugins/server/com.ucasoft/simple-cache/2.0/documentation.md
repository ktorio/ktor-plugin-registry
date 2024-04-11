
Base solution which provides the plugin implementation and abstract class for cache providers.

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
                call.respond(Random.nextInt().toString())
            }
        }
        cacheOutput {
            get("default-cache") {
                call.respond(Random.nextInt().toString())
            }
        }
        // Cache key will be built only on listed query keys. Others will be ignored!
        // `/based-only-on-id-cache?id=123?time=100` and `/based-only-on-id-cache?id=123?time=200` requests will use similar cache key!
        cacheOutput(queryKeys = listOf("id")) {
            get("based-only-on-id-cache") {
                call.respond(Random.nextInt().toString())
            }
        }
    }
```