
[HTML DSL](https://ktor.io/docs/html-dsl.html) integrates the `kotlinx.html` library into Ktor and allows you to respond to a client with HTML blocks. With HTML DSL, you can write pure HTML in Kotlin, interpolate variables into views, and even build complex HTML layouts using templates.

## Usage

To send an HTML response, call the `respondHtml` method inside the required route:

```kotlin
routing {
    get("/") {
        val name = "Ktor"
        call.respondHtml(HttpStatusCode.OK) {
            head {
                title {
                    +name
                }
            }
            body {
                h1 {
                    +"Hello from         $name        !"
                }
            }
        }
    }
}
```

You can learn more from the [HTML DSL](https://ktor.io/docs/html-dsl.html) help topic.
