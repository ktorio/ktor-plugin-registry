import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import kotlinx.css.*
import kotlinx.html.*

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
   this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

