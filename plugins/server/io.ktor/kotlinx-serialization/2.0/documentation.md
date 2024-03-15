
The [kotlinx.serialization](https://ktor.io/docs/serialization.html) plugin provides the ability to serialize/deserialize the content in a specific format (such as JSON, XML, and CBOR). Note that this plugin depends on the `ContentNegotiation` plugin, which is used to negotiate media types between the client and server using the `Accept` and `Content-Type` headers.

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
