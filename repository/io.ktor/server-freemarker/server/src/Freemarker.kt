package kastle

import freemarker.cache.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*

fun Application.configureFreemarker() {
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
}
