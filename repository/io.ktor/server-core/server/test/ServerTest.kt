package kastle

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

private val configFormat: String by _properties

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        if (configFormat == "none") {
            application {
                rootModule()
            }
        } else {
            // loads default configuration
            configure()
        }
        // verify server root returns 200
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

}
