package kastle

import io.ktor.server.application.Application
import kotlinx.rpc.grpc.ktor.server.grpc
import kotlinx.rpc.registerService

fun Application.configureGrpc() {
    grpc(GRPC_PORT) {
        services {
            registerService<SampleService> { SampleServiceImpl() }
        }
    }
}
