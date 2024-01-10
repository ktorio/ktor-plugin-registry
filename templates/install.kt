import io.ktor.server.application.*
import org.group.plugin.sample.*

// The contents of the `install` function will be used for the project template
public fun Application.install() {
    install(Sample) {
        sampleProperty = "property.value"
    }
}
