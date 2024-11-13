import io.ktor.server.application.*
import kotlinx.rpc.krpc.ktor.server.RPC

fun Application.configureRPC() {
    install(RPC)
}