package kastle

import com.github.mustachejava.DefaultMustacheFactory
import io.ktor.server.application.*
import io.ktor.server.mustache.Mustache
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.response.*

fun Application.configureMustache() {
    install(Mustache) {
        mustacheFactory = DefaultMustacheFactory("templates/mustache")
    }
}
