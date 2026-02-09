
The ContentNegotiation plugin serves two primary purposes: negotiating media types between the client and server and serializing/deserializing the content in a specific format when sending requests and receiving responses.

The [ContentNegotiation](https://api.ktor.io/ktor-client/ktor-client-plugins/ktor-client-content-negotiation/io.ktor.client.plugins.contentnegotiation/-content-negotiation/index.html) plugin serves two primary purposes:
* Negotiating media types between the client and server. For this, it uses the `Accept` and `Content-Type` headers.
* Serializing/deserializing the content in a specific format when sending requests and receiving responses.

> On the server, Ktor provides the __ContentNegotiation__ plugin for serializing/deserializing content.


## Usage

To install ContentNegotiation, pass it to the install function inside a client configuration block:

```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation)
}
```

To register the Gson serializer in your application, call the gson method:
```kotlin
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        gson()
    }
}
```
The __gson__ method also allows you to adjust serialization settings provided by [GsonBuilder](https://www.javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/GsonBuilder.html).


