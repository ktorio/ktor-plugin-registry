
Redis cache provider for Ktor Simple Cache plugin

[![Maven Central with version prefix filter](https://img.shields.io/maven-central/v/com.ucasoft.ktor/ktor-simple-redis-cache/0.2.8?color=blue)](https://search.maven.org/artifact/com.ucasoft.ktor/ktor-simple-redis-cache/0.2.8/jar)
## Setup
### Gradle
```kotlin
repositories {
    mavenCentral()
}

implementation("com.ucasoft.ktor:ktor-simple-redis-cache:0.2.8")
```
## Usage
```kotlin
    install(SimpleCache) {
        redisCache {
            invalidateAt = 10.seconds
            host = redis.host
            port = redis.firstMappedPort
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
    }
```