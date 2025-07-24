import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.configureRouting() {
    get("/hello") {
        call.respondText("Hello World!")
    }

    post("/post") {
        val postData = call.receiveText()
        call.respondText("Received: $postData")
    }
}