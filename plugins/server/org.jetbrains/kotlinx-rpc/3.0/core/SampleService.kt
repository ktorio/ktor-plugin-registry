import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable
import kotlin.coroutines.CoroutineContext

@Serializable
data class Data(val value: String)

@Rpc
interface SampleService : RemoteService {
    suspend fun hello(data: Data): String
}

class SampleServiceImpl(override val coroutineContext: CoroutineContext) : SampleService {
    override suspend fun hello(data: Data): String {
        return "Server: ${data.value}"
    }
}