Add gRPC services to your Ktor application!

# Usage

## DISCLAIMER
**This is a dev preview of the kotlinx-rpc gRPC plugin for Ktor. It is not yet ready for production use.
The artifacts provided are dev artifacts, and the API may and will change in the future.**

**Please use it to play with the plugin. We will appreciate any feedback on [GitHub](https://github.com/Kotlin/kotlinx-rpc/issues) or in [Slack](https://kotlinlang.slack.com/archives/C072YJ3Q91V)**

## Project structure

The generated project has three modules:
- **core** — `.proto` schemas and the service implementation. Apply `rpc { protoc() }` here.
- **server** — Ktor server that hosts gRPC services.
- **client** — standalone gRPC client.

## Core module

Declare a service in a `.proto` file inside `src/commonMain/proto/`:
```protobuf
syntax = "proto3";

message ClientGreeting {
  string name = 1;
}

message ServerGreeting {
  string content = 2;
}

service SampleService {
  rpc greeting(ClientGreeting) returns (ServerGreeting);
}
```

The `SampleService` interface will be generated for you alongside other types and helper declarations.

## Server module

Implement the service in the server module:
```kotlin
class SampleServiceImpl : SampleService {
    override suspend fun greeting(name: ClientGreeting): ServerGreeting {
        return ServerGreeting { content = "Hello, ${name.name}!" }
    }
}
```


Register the service using the `grpc` extension on `Application`:
```kotlin
fun Application.module() {
    grpc(GRPC_PORT) {
        services {
            registerService<SampleService> { SampleServiceImpl() }
        }
    }
}
```

## Client module

Use `GrpcClient` to connect and call the service:
```kotlin
val client = GrpcClient("localhost", GRPC_PORT) {
    credentials = plaintext()
}

val service = client.withService<SampleService>()
val response = service.greeting(ClientGreeting { name = "World" })
println("Response: ${response.content}")

client.shutdown()
client.awaitTermination()
```

## Learn more

- [Getting started](https://kotlin.github.io/kotlinx-rpc/get-started.html)
- [gRPC Configuration](https://kotlin.github.io/kotlinx-rpc/grpc-configuration.html)
- [gRPC Services](https://kotlin.github.io/kotlinx-rpc/grpc-services.html)
- [gRPC With Ktor Server](https://kotlin.github.io/kotlinx-rpc/grpc-ktor-server.html)
- [Schema and codegen](https://kotlin.github.io/kotlinx-rpc/grpc-codegen.html)
- [Using generated code](https://kotlin.github.io/kotlinx-rpc/grpc-generated-code.html)
