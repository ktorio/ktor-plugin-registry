import io.ktor.server.testing.testApplication
import kotlinx.rpc.grpc.GrpcClient
import kotlinx.rpc.grpc.ktor.server.grpc
import kotlinx.rpc.registerService
import kotlinx.rpc.withService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class ApplicationRpcTest {
    @Test
    fun testRpc() = testApplication {
        application {
            grpc(8081) {
                registerService<SampleService> { SampleServiceImpl() }
            }
        }

        startApplication()

        val client = GrpcClient("localhost", 8081) {
            usePlaintext()
        }

        val response = client.withService<SampleService>().greeting(
            ClientGreeting {
                name = "Alex"
            }
        )

        assertEquals("Hello, Alex!", response.content, "Wrong response message")

        client.shutdown()
        client.awaitTermination(1.minutes)
    }
}
