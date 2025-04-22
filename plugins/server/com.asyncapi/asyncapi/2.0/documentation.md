
The `AsyncAPI` plugin allows you to generate and serve AsyncAPI documentation for your Ktor application.

## Usage
To serve your AsyncAPI specification via Ktor:
- install the `AsyncApiPlugin` in you application
- document your API with `AsyncApiExtension` and/or Kotlin scripting (see [Kotlin script usage](https://github.com/asyncapi/kotlin-asyncapi#kotlin-script-usage))
- add annotations to auto-generate components (see [annotation usage](https://github.com/asyncapi/kotlin-asyncapi#annotation-usage))

You can register multiple extensions to extend and override AsyncAPI components. Extensions with a higher order override extensions with a lower order. Please note that you can only extend top-level components for now (`info`, `channels`, `servers`...). Subcomponents will always be overwritten.

**Example** (simplified version of [Gitter example](https://github.com/asyncapi/spec/blob/22c6f2c7a61846338bfbd43d81024cb12cf4ed5f/examples/gitter-streaming.yml))
```kotlin
fun main() {
    embeddedServer(Netty, port = 8000) {
        install(AsyncApiPlugin) {
            extension = AsyncApiExtension.builder(order = 10) {
                info {
                    title("Gitter Streaming API")
                    version("1.0.0")
                }
                servers {
                    // ...
                }
                // ...
            }
        }
    }.start(wait = true)
}

@Channel(
    value = "/rooms/{roomId}",
    parameters = [
        Parameter(
            value = "roomId",
            schema = Schema(
                type = "string",
                examples = ["53307860c3599d1de448e19d"]
            )
        )
    ]
)
class RoomsChannel {

    @Subscribe(message = Message(ChatMessage::class))
    fun publish(/*...*/) { /*...*/ }
}

@Message
data class ChatMessage(
    val id: String,
    val text: String
)
```

## Configuration
The plugin provides the following configuration properties:

| Property          | Description                                                   | Default                            |
|-------------------|---------------------------------------------------------------|------------------------------------|
| `path`            | The resource path for serving the generated AsyncAPI document | `/docs/asyncapi`                   |
| `baseClass`       | The base class to filter code scanning packages               | `null`                             |
| `scanAnnotations` | Enables class path scanning for annotations                   | `true`                             |
| `extension`       | AsyncApiExtension hook                                        | `AsyncApiExtension.empty()`        |
| `extensions`      | For registering multiple AsyncApiExtension hooks              | `emptyList()`                      |
| `resourcePath`    | Path to the generated script resource file                    | `asyncapi/generated/asyncapi.json` |
| `sourcePath`      | Path to the AsyncAPI Kotlin script file                       | `build.asyncapi.kts`               |

You can learn more from [kotlin-asyncapi](https://github.com/asyncapi/kotlin-asyncapi).
