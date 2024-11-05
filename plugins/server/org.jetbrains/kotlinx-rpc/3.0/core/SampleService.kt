import kotlinx.rpc.RPC
import kotlinx.serialization.Serializable
import kotlin.coroutines.CoroutineContext

@Serializable
data class Data(val value: String)

interface SampleService : RPC {
    suspend fun hello(data: Data): String
}

class SampleServiceImpl(override val coroutineContext: CoroutineContext) : SampleService {
    override suspend fun hello(data: Data): String {
        return "Server: ${data.value}"
    }
}