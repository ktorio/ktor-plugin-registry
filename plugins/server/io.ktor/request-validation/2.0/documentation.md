
The [RequestValidation](https://ktor.io/docs/server-request-validation.html) plugin provides the ability to validate a body of incoming requests. You can validate a raw request body or specified request object properties if the ContentNegotiation plugin with a serializer is installed. If a request body validation fails, the plugin raises `RequestValidationException`, which can be handled using the `StatusPages` plugin.

## Usage

Configuring RequestValidation involves three main steps:

1. Receiving body contents.
2. Configuring a validation function.
3. Handling validation exceptions.

### 1. Receive body

The RequestValidation plugin validates a body of a request if you call the receive function with a type parameter. For instance, the code snippet below shows how to receive a body as a String value:

```kotlin
routing {
    post("/text") {
        val body = call.receive<String>()
        call.respond(body)
    }
}
```

### 2. Configure a validation function

To validate a request body, use the validate function. This function returns a `ValidationResult` object representing a successful or unsuccessful validation result. For an unsuccessful result, `RequestValidationException` is raised.

```kotlin
install(RequestValidation) {
    validate<String> { bodyText ->
        if (bodyText.isEmpty())
            ValidationResult.Invalid("Body text should not be empty")
        else ValidationResult.Valid
    }
}
```

### 3. Handle validation exceptions

If request validation is failed, `RequestValidation` raises `RequestValidationException`. This exception allows you to access a request body and get reasons for all validation failures for this request.

You can handle `RequestValidationException` using the [StatusPages](https://ktor.io/docs/server-status-pages.html) plugin as follows:

```kotlin
install(StatusPages) {
    exception<RequestValidationException> { call, cause ->
        call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
    }
}
```