import com.example.proto.invoke
import com.example.proto.ClientGreeting
import com.example.proto.SampleService
import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.withService

suspend fun main() {
    val client = GrpcClient("localhost", GRPC_PORT) {
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
