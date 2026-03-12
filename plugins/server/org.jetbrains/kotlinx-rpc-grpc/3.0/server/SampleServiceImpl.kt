import com.example.proto.ClientGreeting
import com.example.proto.SampleService
import com.example.proto.ServerGreeting
import com.example.proto.invoke

class SampleServiceImpl : SampleService {
    override suspend fun greeting(name: ClientGreeting): ServerGreeting {
        return ServerGreeting { content = "Hello, ${name.name}!" }
    }
}
