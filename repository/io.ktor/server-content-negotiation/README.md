
The [ContentNegotiation](https://ktor.io/docs/serialization.html) plugin serves two primary purposes:
* Negotiating media types between the client and server. For this, it uses the `Accept` and `Content-Type` headers.
* Serializing/deserializing the content in a specific format (such as JSON, XML, and CBOR). This requires a separate serialization plugin: `kotlinx.serialization`, `Gson`, or `Jackson`.

## Usage

To install `ContentNegotiation` and register the JSON serializer in your application, call the `json` method:
```kotlin
install(ContentNegotiation) {
    json()
}
```
This allows you to deserialize received JSON data to an object of a specific class. For example, if you have the following data class, ...
```kotlin
@Serializable
data class Customer(val id: Int, val firstName: String, val lastName: String)
```
... you need to pass it to the `receive` method as a parameter to convert JSON data to an object:
```kotlin
post("/customer") {
    val customer = call.receive<Customer>()
    customerStorage.add(customer)
    call.respondText("Customer stored correctly", status = HttpStatusCode.Created)
}
```
To learn more, see [Content negotiation and serialization](https://ktor.io/docs/serialization.html).
