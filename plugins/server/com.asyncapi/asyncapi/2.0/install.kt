import io.ktor.server.application.*
import com.asyncapi.kotlinasyncapi.ktor.AsyncApiPlugin
import com.asyncapi.kotlinasyncapi.context.service.AsyncApiExtension

public fun Application.install() {
    install(AsyncApiPlugin) {
        extension = AsyncApiExtension.builder {
            info {
                title("Sample API")
                version("1.0.0")
            }
        }
    }
}
