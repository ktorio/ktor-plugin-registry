package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

// The contents of the `install` function will be used for the project template
fun Application.configureDi() {
    dependencies {
        provide { GreetingService { "Hello, World!" } }
    }
}
