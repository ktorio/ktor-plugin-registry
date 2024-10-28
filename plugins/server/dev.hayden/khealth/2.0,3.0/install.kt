import io.ktor.server.application.*
import dev.hayden.KHealth

// The contents of the `install` function will be used for the project template
public fun Application.install() {
    install(KHealth)
}
