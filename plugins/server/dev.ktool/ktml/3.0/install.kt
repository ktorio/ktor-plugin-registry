import io.ktor.server.application.*

import dev.ktml.ktor.KtmlPlugin

public fun Application.install() {
    install(KtmlPlugin) {
        templatePackage = "templates"
    }
}