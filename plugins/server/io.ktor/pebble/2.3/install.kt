import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.response.*
import io.pebbletemplates.pebble.loader.ClasspathLoader

public fun Application.configureTemplating() {
    install(Pebble) {
        loader(ClasspathLoader().apply {
            prefix = "templates"
        })
    }
}
