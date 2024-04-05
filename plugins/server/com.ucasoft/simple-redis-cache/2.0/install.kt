import io.ktor.server.application.*
import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleRedisCache.*

public fun Application.configureCache() {
    install(SimpleCache) {
        redisCache {
            invalidateAt = 10.seconds
            host = config.redis.host
            port = config.redis.firstMappedPort
        }
    }
}
