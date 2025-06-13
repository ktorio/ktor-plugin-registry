import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

// The contents of the `install` function will be used for the project template
public fun Application.install() {
    dependencies {
        provide { GreetingService { "Hello, World!" } }
    }
}
