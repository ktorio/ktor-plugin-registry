# Ktor Simple Memory Cache
Memory cache provider for Ktor Simple Cache plugin

[![Maven Central with version prefix filter](https://img.shields.io/maven-central/v/com.ucasoft.ktor/ktor-simple-memory-cache/0.2.7?color=blue)](https://search.maven.org/artifact/com.ucasoft.ktor/ktor-simple-memory-cache/0.2.7/jar)
## Setup
### Gradle
```kotlin
repositories {
    mavenCentral()
}

implementation("com.ucasoft.ktor:ktor-simple-memory-cache:0.2.7")
```
## Usage
```kotlin
    install(SimpleCache) {
        memoryCache {
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
    }
```