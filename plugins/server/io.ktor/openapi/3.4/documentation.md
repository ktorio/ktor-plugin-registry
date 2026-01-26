
The `OpenAPI` plugin allows you to serve OpenAPI documentation for your Ktor application. Documentation will be rendered from the OpenAPI file.

## Usage

To serve OpenAPI documentation, you need to call the `openAPI` method that creates a `GET` endpoint with documentation at the `path` rendered from the OpenAPI specification placed at `swaggerFile`:
```kotlin
import io.ktor.server.plugins.openapi.*

fun Application.main() {
    routing {
        openAPI(path="openapi", swaggerFile = "openapi/documentation.yaml")
    }
}
```
This method tries to look up the OpenAPI specification in the application resources. Otherwise, it tries to read the OpenAPI specification from the file system using `java.io.File`.

By default, the documentation is generated using `StaticHtml2Codegen`. You can customize generation settings inside the `openAPI` block:
```kotlin
openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml") {
     // this: OpenAPIConfig
}
```
You can learn more from [OpenAPI](https://ktor.io/docs/openapi.html).


