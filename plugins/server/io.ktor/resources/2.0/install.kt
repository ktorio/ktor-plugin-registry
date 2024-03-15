import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

public fun Application.configureRouting() {
    install(Resources)
}
