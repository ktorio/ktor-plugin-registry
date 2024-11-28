import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

public fun Routing.configureRouting() {
    get("/session/increment") {
        val session = call.sessions.get<MySession>() ?: MySession()
        call.sessions.set(session.copy(count = session.count + 1))
        call.respondText("Counter is ${session.count}. Refresh to increment.")
    }
}
