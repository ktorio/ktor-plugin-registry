
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

To register the __CBOR__ serializer in your application, call the __cbor__ method:

```kotlin
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.cbor.*

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        cbor()
    }
}
```

The __cbor__ method also allows you to access CBOR serialization settings provided by [CborBuilder](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/), for example:

```kotlin
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.cbor.*
import kotlinx.serialization.cbor.*

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        cbor(Cbor {
            ignoreUnknownKeys = true
        })
    }
}
```
