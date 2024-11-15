import io.ktor.client.HttpClient
import io.ktor.client.request.url
import kotlinx.rpc.RPCClient

import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

suspend fun HttpClient.rpcClient(url: String): RPCClient =
    rpc {
        url(url)

        rpcConfig {
            serialization {
                json()
            }
        }
    }

fun RPCClient.sampleService(): SampleService =
    withService<SampleService>()
