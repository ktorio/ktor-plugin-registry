/**
 * slot://io.ktor/server-core/routingRoot
 */
package kastle

import io.ktor.server.html.respondHtml
import io.ktor.server.html.respondHtmlFragment
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlin.random.Random

fun Routing.htmxEndpoints() {
    val random = Random(System.currentTimeMillis())

    staticResources("/", "/web")

    get("/") {
        call.respondHtml {
            leaderboardPage(random)
        }
    }

    get("/more-rows") {
        call.respondHtmlFragment {
            table {
                tbody {
                    randomRows(random)
                }
            }
        }
    }
}