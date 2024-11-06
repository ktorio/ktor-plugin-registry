import io.ktor.client.plugins.sse.sse
import io.ktor.server.testing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.coroutines.flow.*
import kotlin.test.*

class ServerSentEventsTest {

    @Test
    fun testServerSentEvents() = testApplication {
        install(SSE)
        routing {
            sse("/events") {
                repeat(100) {
                    send(ServerSentEvent("event $it"))
                }
            }
        }

        createClient {
            install(io.ktor.client.plugins.sse.SSE)
        }.sse("/events") {
            incoming.collectIndexed { i, event ->
                assertEquals("event $i", event.data)
            }
        }
    }

}
