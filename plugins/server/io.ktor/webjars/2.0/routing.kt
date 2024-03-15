import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*

public fun Routing.configureRouting() {
    get("/webjars") {
        call.respondText("<script src='/webjars/jquery/jquery.js'></script>", ContentType.Text.Html)
    }
}
