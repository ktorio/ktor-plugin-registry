The koin-ktor module is dedicated to bring dependency injection for Ktor.

More information on Koin and the Ktor plugin can be [found here](https://insert-koin.io/docs/reference/koin-ktor/ktor).

## Usage

To start a Koin container in Ktor, install the Koin plugin like the following:

```kotlin
fun Application.main() {
    // Install Koin
    install(Koin) {
        slf4jLogger()
        modules(helloAppModule)
    }
}
```

Koin `inject()` and `get()` functions are available from Application, Route, Routing classes:

```ktor
fun Application.main() {

    // inject HelloService
    val service by inject<HelloService>()

    routing {
        get("/hello") {
            call.respondText(service.sayHello())
        }
    }
}
```

### Resolve from Ktor Request Scope (since 3.5.0)

You can declare components to live within Ktor request scope timeline. For this, you just need to declare your component inside a scope<ScopeRequest> section. Given a ScopeComponent class to instantiate on RequestScope, let's declare it:

```kotlin
scope<RequestScope>{
    scopedOf(::ScopeComponent)
}
```

And from your http call, just call `call.scope.get()` to resolve the right dependency:

```kotlin
routing {
    get("/hello") {
        val component = call.scope.get<ScopeComponent>()
        // ...
    }
}
```

Note: for each new request, the scope will be recreated. This creates and drops scope instances for each request.

### Run Koin from an external Ktor Module

For a Ktor module, you can load specific Koin modules. Just declare them with `koin { }` function:

```kotlin
fun Application.module2() {

    koin {
        // load koin modules
        modules(appModule2)
    }

}
```

### Ktor Events

You can listen to KTor Koin events:

```kotlin
fun Application.main() {
// ...

    // Install Ktor features
    environment.monitor.subscribe(KoinApplicationStarted) {
        log.info("Koin started.")
    }

    environment.monitor.subscribe(KoinApplicationStopPreparing) {
        log.info("Koin stopping...")
    }

    environment.monitor.subscribe(KoinApplicationStopped) {
        log.info("Koin stopped.")
    }

    //...
}
```