import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*

public fun ContentNegotiationConfig.configureContentNegotiation() {
    json()
}
