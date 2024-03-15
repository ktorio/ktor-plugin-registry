
The [DefaultRequest](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.plugins/-default-request/index.html) plugin allows you to configure default parameters for all [requests](request.md): specify a base URL, add headers, configure query parameters, and so on.

## Usage

To install `DefaultRequest`, pass it to the `install` function inside a client configuration block ...
```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
//...
val client = HttpClient(CIO) {
    install(DefaultRequest)
}
```

... or call the `defaultRequest` function and [configure](#configure) required request parameters:

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
//...
val client = HttpClient(CIO) {
    defaultRequest {
        // this: DefaultRequestBuilder
    }
}
```

## Configure DefaultRequest {id="configure"}

### Base URL {id="url"}

`DefaultRequest` allows you to configure a base part of the URL that is merged with a request URL.
For example, the `url` function below specifies a base URL for all requests:

```kotlin
defaultRequest {
    url("https://ktor.io/docs/")
}
```

If you make the following request using the client with the above configuration, ...

```kotlin
val response: HttpResponse = client.get("welcome.html")
```

... the resulting URL will be the following: `https://ktor.io/docs/welcome.html`.
To learn how base and request URLs are merged, see [DefaultRequest](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.plugins/-default-request/index.html).


### URL parameters {id="url-params"}

The `url` function also allows you to specify URL components separately, for example:
- an HTTP scheme;
- a host name;
- a base URL path;
- a query parameter.

```kotlin
url {
    protocol = URLProtocol.HTTPS
    host = "ktor.io"
    path("docs/")
    parameters.append("token", "abc123")
}
```

### Headers {id="headers"}

To add a specific header to each request, use the `header` function:

```kotlin
defaultRequest {
    header("X-Custom-Header", "Hello")
}
```

To avoid duplicating headers, you can use the `appendIfNameAbsent`, `appendIfNameAndValueAbsent`, and `contains` functions:

```kotlin
defaultRequest {
    headers.appendIfNameAbsent("X-Custom-Header", "Hello")
}
```

