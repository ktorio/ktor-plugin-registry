import io.ktor.client.*
import kotlinx.rpc.krpc.ktor.client.installKrpc

fun HttpClientConfig<*>.configure() {
    installKrpc()
}
