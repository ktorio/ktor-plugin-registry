import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.withService

suspend fun main() {
    val client = GrpcClient("localhost", 8081) {
        credentials = plaintext()
    }

    val service = client.withService<SampleService>()

    val response = service.greeting(
        ClientGreeting {
            name = "World"
        }
    )
    println("Response: ${response.content}")

    client.shutdown()
    client.awaitTermination()
}
