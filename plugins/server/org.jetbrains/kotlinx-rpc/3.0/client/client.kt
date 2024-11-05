import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

fun main() = runBlocking {
    val rpcClient = HttpClient { installRPC() }.rpc {
        url("ws://localhost:8080/api")

        rpcConfig {
            serialization {
                json()
            }
        }
    }
    val response = rpcClient.withService<SampleService>().hello(Data("Client"))
    println(response)
}