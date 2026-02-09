
By default, Ktor doesn't validate a response depending on its status code. If required, you can use the following validation strategies:

* Use the `expectSuccess` property to throw exceptions for non-2xx responses.
* Add stricter validation of 2xx responses.
* Customize validation of non-2xx responses.



## Usage

Ktor allows you to enable default validation by setting the `expectSuccess` property to `true`. This can be done on a client configuration level ...

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*

val client = HttpClient(CIO) {
    expectSuccess = true
}
```... or by using the same property on a request level.

### Custom validation

You can add additional validation for 2xx responses or customize default validation by using the `HttpCallValidator` plugin. To install `HttpCallValidator`, call the `HttpResponseValidator` function inside a client configuration block:

```kotlin
val client = HttpClient(CIO) {
    HttpResponseValidator {
        // ...
    }
}
```###### Validate 2xx responses

As mentioned above, default validation throws exceptions for non-2xx error responses. If you need to add stricter validation and check 2xx responses, use the `validateResponse` function available in `HttpCallValidator`.

In the example below, a client receives a 2xx response with error details in a `JSON` format. The `validateResponse` is used to raise a `CustomResponseException`:

```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation) { json() }
    HttpResponseValidator {
        validateResponse { response ->
            val error: Error = response.body()
            if (error.code != 0) {
                throw CustomResponseException(response, "Code: ${error.code}, message: ${error.message}")
            }
        }
    }
}
```
