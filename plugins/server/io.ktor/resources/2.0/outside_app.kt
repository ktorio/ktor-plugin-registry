import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import kotlinx.serialization.Serializable

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
