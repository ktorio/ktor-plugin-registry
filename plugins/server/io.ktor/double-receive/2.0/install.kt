import io.ktor.server.application.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*

public fun Application.configureRouting() {
    install(DoubleReceive)
}
