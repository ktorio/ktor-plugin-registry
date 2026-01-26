/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.configureAuthBasic() {
    authenticate("myauth1") {
        get("/protected/route/basic") {
            val principal = call.principal<UserIdPrincipal>()!!
            call.respondText("Hello ${principal.name}")
        }
    }
    authenticate("myauth2") {
        get("/protected/route/form") {
            val principal = call.principal<UserIdPrincipal>()!!
            call.respondText("Hello ${principal.name}")
        }
    }
}
