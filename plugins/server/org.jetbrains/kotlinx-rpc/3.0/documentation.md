kotlinx.rpc is a Kotlin library for adding asynchronous Remote Procedure Call (RPC) services to your applications. Build your RPC with already known language constructs and nothing more!

# Usage

First, create your RPC service and define some methods:

```kotlin
@Rpc
interface AwesomeService {
    suspend fun greeting(name: String): String
    
    fun getNews(city: String): Flow<String>
}
```
In your server code define how to respond by simply implementing the service:
```kotlin
class AwesomeServiceImpl : AwesomeService {
    override suspend fun greeting(name: String): String {
        return "Hello, $name!"
    }
    
    override fun getNews(city: String): Flow<String> {
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

                registerService<AwesomeService> { AwesomeServiceImpl() }
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

val service = rpcClient.withService<AwesomeService>()

service.greeting("Alex")    
    
service.getNews("KotlinBurg").collect { article ->
    println(article)
}
```
