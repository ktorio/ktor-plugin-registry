import freemarker.cache.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*

data class IndexData(val items: List<Int>)
