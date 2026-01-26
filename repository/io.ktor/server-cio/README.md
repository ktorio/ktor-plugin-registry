This introduces the CIO engine for running Ktor servers.

The CIO engine is written in pure Kotlin and is supported on multiple platforms.

For more information on the different Ktor engines, consult [the documentation](https://ktor.io/docs/server-engines.html).

## Usage

You can configure your server to use the provided main function:

In Amper:

```yaml
# module.yaml
settings:
  jvm:
    mainClass: io.ktor.server.cio.EngineMain
```

Or, you may reference it from your own main function:

```kotlin
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*

fun main(args: Array<String>) {
    embeddedServer(CIO, port = 8080) {
        routing {
            get("/") {
                call.respondText("Hello, world!")
            }
        }
    }.start(wait = true)
}
```