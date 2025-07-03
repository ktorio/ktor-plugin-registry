import io.ktor.client.HttpClient
import io.ktor.client.request.url
import kotlinx.rpc.RpcClient
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

fun HttpClient.rpcClient(url: String): RpcClient =
    rpc {
        url(url)

        rpcConfig {
            serialization {
                json()
            }
        }
    }

fun RpcClient.sampleService(): SampleService =
    withService<SampleService>()
