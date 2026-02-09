
Ktor client provides the capability to log HTTP calls using the Logging plugin. This plugin provides different logger types for different platforms.

## Usage

The example below shows how to configure the Logging plugin:

The `logger` property is set to `Logger.DEFAULT`, which uses an SLF4J logging framework. For Native targets, set this property to `Logger.SIMPLE`.

The `level` property specifies the logging level.
```kotlin
package com.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val client = HttpClient(CIO) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
        }

        val response: HttpResponse = client.get("https://ktor.io/")
    }
}
```
##Provide a custom logger
To use a custom logger in your client application, you need to create a `Logger` instance and override the log function. The example below shows how to use the [Napier](https://github.com/AAkira/Napier) library to log HTTP calls:

```kotlin
fun main() {
    runBlocking {
        val client = HttpClient(CIO) {
            install(Logging) {
                logger = object: Logger {
                    override fun log(message: String) {
                        Napier.v("HTTP Client", null, message)
                    }
                }
                level = LogLevel.HEADERS
            }
        }.also { Napier.base(DebugAntilog()) }

        val response: HttpResponse = client.get("https://ktor.io/")
    }
}
```
