
Ktor allows you to generate and serve Swagger UI for your project based on the existing OpenAPI specification. With Swagger UI, you can visualize and interact with the API resources.

## Usage

To serve Swagger UI, you need to call the `swaggerUI` method that creates a `GET` endpoint with Swagger UI at the `path` rendered from the OpenAPI specification placed at `swaggerFile`:
```kotlin
import io.ktor.server.plugins.swagger.*

fun Application.main() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}
```
This method tries to look up the OpenAPI specification in the application resources. Otherwise, it tries to read the OpenAPI specification from the file system using `java.io.File`.

Optionally, you can customize Swagger UI inside the `swaggerUI` block. For example, you can use another Swagger UI version or apply a custom style.
```kotlin
swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml") {
     // this: SwaggerConfig
}
```
You can learn more from [Swagger UI](https://ktor.io/docs/swagger-ui.html).

###Configure CORS

To make sure your API works nicely with Swagger UI, you need to set up a policy for [Cross-Origin Resource Sharing (CORS)](https://ktor.io/docs/cors.html). The example below applies the following CORS configuration:
* `anyHost` enables cross-origin requests from any host;
* `allowHeader` allows the `Content-Type` client header used in [content negotiation](https://ktor.io/docs/serialization.html).
```kotlin
install(CORS) {
    anyHost()
    allowHeader(HttpHeaders.ContentType)
}
```
