import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.velocity.*
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader

data class VelocityUser(val id: Int, val name: String)
