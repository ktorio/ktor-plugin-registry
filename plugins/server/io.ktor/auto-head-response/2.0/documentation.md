
The [AutoHeadResponse](https://ktor.io/docs/autoheadresponse.html) plugin provides us with the ability to automatically respond to `HEAD` requests for every route that has a `GET` defined. You can use `AutoHeadResponse` to avoid creating a separate `head` handler if you need to somehow process a response on the client before getting the actual content.

## Usage

In order to take advantage of this functionality, we need to install the `AutoHeadResponse` plugin in our application:
```kotlin
install(AutoHeadResponse)
routing {
    get("/home") {
        call.respondText("This is a response to a GET, but HEAD also works")
    }
}
```
In our case, the `/home` route will now respond to `HEAD` requests even though there is no explicit definition for this verb.
## Options

`AutoHeadResponse` does not provide any additional configuration options.
