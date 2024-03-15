import com.fasterxml.jackson.databind.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*

public fun ContentNegotiationConfig.configureContentNegotiation() {
    jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
}
