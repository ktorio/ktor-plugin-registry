import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.css.CssBuilder

suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
   this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

