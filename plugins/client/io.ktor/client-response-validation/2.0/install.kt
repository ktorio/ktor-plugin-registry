import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*

data class Error(val code: Int, val message: String)

class CustomResponseException(message: String): Exception(message)

public fun HttpClientConfig<*>.configure() {
    HttpResponseValidator {
        validateResponse { response ->
            val error: Error = response.body()
            if (error.code != 0) {
                throw CustomResponseException("Code: ${error.code}, message: ${error.message}")
            }
        }
    }
}
