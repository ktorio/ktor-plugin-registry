import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.sql.*
import kotlinx.coroutines.*

public fun Application.install() {
    val dbConnection: Connection = connectToPostgres(embedded = true)
    
        val cityService = CityService(dbConnection)
}
