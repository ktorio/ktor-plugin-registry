import io.ktor.server.application.*
import kotlinx.rpc.krpc.ktor.server.Krpc

fun Application.configureRPC() {
    install(Krpc)
}
