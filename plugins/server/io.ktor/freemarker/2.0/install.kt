import freemarker.cache.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*

public fun Application.configureTemplating() {
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
}
