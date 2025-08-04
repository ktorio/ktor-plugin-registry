Add gRPC services to your Ktor application!

# Usage

## DISCLAIMER
**This is a dev preview of the kotlinx-rpc gRPC plugin for Ktor. It is not yet ready for production use.
The artifacts provided are dev artifacts, and the API may and will change in the future.**

**Please use it to play with the plugin. We will appreciate any feedback on the 
[Github](https://github.com/Kotlin/kotlinx-rpc/issues) 
or in [Slack](https://kotlinlang.slack.com/archives/C072YJ3Q91V)**

## gRPC Server
You can add gRPC services to your Ktor application by using the `grpc` function provided by the plugin. 
Here's an example:

Declare a service in a `.proto` file.
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

The `SampleService` interface will generated for you alongside with other types and helper declarations. 

Define an implementation. Then register it on the server and all done:

```kotlin
class SampleServiceImpl : SampleService {
    override suspend fun greeting(name: ClientGreeting): ServerGreeting {
        return ServerGreeting { content = "Hello, ${name.name}!" }
    }
}

fun Application.module() {
    grpc {
        registerService<SampleService> { SampleServiceImpl() }
    }
}
```

## gRPC Client
You can use any gRPC client on the other end, but also you can use ours!

To do that - use the `GrpcClient` class:
```Kotlin
val client = GrpcClient("localhost", 8081) {
    usePlaintext()
}

val response = client.withService<SampleService>().greeting(
    ClientGreeting {
        name = "Alex"
    }
)

assertEquals("Hello, Alex!", response.content, "Wrong response message")

client.close()
```
