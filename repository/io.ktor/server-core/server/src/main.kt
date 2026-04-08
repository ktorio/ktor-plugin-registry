package kastle

import io.ktor.server.engine.*
import io.ktor.server.application.*

private val configFormat: String by _properties
private val engineClass: String by _properties

fun main(args: Array<String>) {
    if (configFormat == "none") {
        embeddedServer(
            factory = _unsafe<ApplicationEngineFactory<*, *>>("$engineClass"),
            port = 8080,
            host = "0.0.0.0",
            module = Application::rootModule
        ).start(wait = true)
    } else {
        _slot("engineMain")
    }
}
