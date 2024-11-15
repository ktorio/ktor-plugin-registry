import io.ktor.client.*
import kotlinx.rpc.krpc.ktor.client.installRPC

fun HttpClientConfig<*>.configure() {
    installRPC()
}