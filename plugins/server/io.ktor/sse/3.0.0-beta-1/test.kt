import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.sse.*
import io.ktor.server.testing.*
import io.ktor.sse.*
import kotlin.test.*

class ServerSentEventsTest {

    @Test
    fun testSeverSentEvents() = testApplication {
        routing {
            sse("/events") {
                repeat(100) {
                    send(ServerSentEvent("event $it"))
                }
            }
        }

        client.get("/events").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(ContentType.Text.EventStream.toString(), headers[HttpHeaders.ContentType])
            val events = bodyAsText().lines()
            assertEquals(201, events.size)
            for (i in 0 until 100) {
                assertEquals("data: event $i", events[i * 2])
                assertTrue(events[i * 2 + 1].isEmpty())
            }
        }
    }

}
