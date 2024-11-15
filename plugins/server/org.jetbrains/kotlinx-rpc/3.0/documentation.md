kotlinx.rpc is a Kotlin library for adding asynchronous Remote Procedure Call (RPC) services to your applications. Build your RPC with already known language constructs and nothing more!

# Usage

First, create your RPC service and define some methods:

```kotlin
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface AwesomeService : RemoteService {
    suspend fun getNews(city: String): Flow<String>
}
```
In your server code define how to respond by simply implementing the service:
```kotlin
class AwesomeServiceImpl(override val coroutineContext: CoroutineContext) : AwesomeService {
    override suspend fun getNews(city: String): Flow<String> {
        return flow { 
            emit("Today is 23 degrees!")
            emit("Harry Potter is in $city!")
            emit("New dogs cafe has opened doors to all fluffy customers!")
        }
    }
}
```
Then, choose how do you want your service to communicate. For example, you can use integration with [Ktor](https://ktor.io/):

```kotlin
fun main() {
    embeddedServer(Netty, 8080) {
        install(RPC)
        routing {
            rpc("/awesome") {
                rpcConfig {
                    serialization {
                        json()
                    }
                }

                registerService<AwesomeService> { ctx -> AwesomeServiceImpl(ctx) }
            }
        }
    }.start(wait = true)
}
```
To connect to the server use the following [Ktor Client](https://ktor.io/docs/create-client.html) setup:
```kotlin
val rpcClient = HttpClient { installRPC() }.rpc {
    url("ws://localhost:8080/awesome")

    rpcConfig {
        serialization {
            json()
        }
    }
}

streamScoped {
    rpcClient.withService<AwesomeService>().getNews("KotlinBurg").collect { article ->
        println(article)
    }
}
```