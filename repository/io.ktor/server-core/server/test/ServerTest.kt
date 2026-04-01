package kastle

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        configure() // loads configuration
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

}
