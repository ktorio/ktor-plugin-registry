import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.response.*
import io.pebbletemplates.pebble.loader.ClasspathLoader

data class PebbleUser(val id: Int, val name: String)
