import io.ktor.server.application.*
import io.ktor.server.auth.*
import java.io.File
import com.kborowy.authprovider.firebase.firebase

data class MyAuthenticatedUser(val id: String) : Principal

public fun Application.install() {
    install(Authentication) {
        firebase {
            adminFile = File("path/to/admin/file.json")
            realm = "My Server"
            validate { token ->
                MyAuthenticatedUser(id = token.uid)
            }
        }
    }
}
