import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.*

public fun Application.install(database: Database) {
    val userService = UserService(database)
}
