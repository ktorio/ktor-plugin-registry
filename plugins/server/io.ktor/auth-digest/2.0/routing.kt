import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*

public fun Routing.configureRouting() {
    authenticate("myDigestAuth") {
        get("/protected/route/digest") {
            val principal = call.principal<UserIdPrincipal>()!!
            call.respondText("Hello ${principal.name}")
        }
    }
}
