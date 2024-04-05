import io.ktor.server.application.*
import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleMemoryCache.*

public fun Application.configureCache() {
    install(SimpleCache) {
        memoryCache {
            invalidateAt = 10.seconds
        }
    }
}
