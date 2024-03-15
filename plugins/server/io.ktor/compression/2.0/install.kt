import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.response.*

public fun Application.configureHTTP() {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }
}
